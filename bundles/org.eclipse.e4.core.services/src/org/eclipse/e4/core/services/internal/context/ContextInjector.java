/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.core.services.internal.context;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.core.internal.services.ServicesActivator;
import org.eclipse.e4.core.services.IDisposable;
import org.eclipse.e4.core.services.context.IEclipseContext;
import org.eclipse.e4.core.services.context.spi.ContextInjectionFactory;
import org.eclipse.e4.core.services.context.spi.IContextConstants;
import org.eclipse.e4.core.services.injector.IObjectDescriptor;
import org.eclipse.e4.core.services.injector.IObjectProvider;
import org.eclipse.e4.core.services.internal.annotations.AnnotationsSupport;

// TBD rename InjectorImpl
/**
 * Reflection-based context injector.
 */
public class ContextInjector {

	private class Processor {

		final private IObjectDescriptor descriptor;
		protected boolean addition;

		protected boolean shouldProcessPostConstruct = false;
		protected boolean isInDispose = false;
		protected Object userObject;

		protected boolean injectWithNulls = false;

		protected boolean processStatic = false;

		private List postConstructMethods;

		public ArrayList classHierarchy = new ArrayList(5);

		public Processor(IObjectDescriptor descriptor, boolean addition, boolean isInDispose) {
			this.descriptor = descriptor;
			this.addition = addition;
			this.isInDispose = isInDispose;
		}

		public void setObject(Object userObject) {
			this.userObject = userObject;
			// this operation also resets state variables
			classHierarchy.clear();
		}

		public void setInjectNulls(boolean injectWithNulls) {
			this.injectWithNulls = injectWithNulls;
		}

		public void setProcessStatic(boolean processStatic) {
			this.processStatic = processStatic;
		}

		/**
		 * The method assumes injection is needed for this field.
		 */
		public boolean processField(final Field field, InjectionProperties properties) {
			if (Modifier.isStatic(field.getModifiers()) != processStatic)
				return true;
			if (descriptor != null) { // filter if descriptor is specified
				String descriptorsKey = context.getKey(descriptor);
				if (!descriptorsKey.equals(context.getKey(properties)))
					return true;
			}
			Object value = null;
			if (addition) {
				Object provider = properties.getProvider();
				if (provider != null)
					value = provider;
				else if (context.containsKey(properties))
					value = context.get(properties);
				else {
					if (!properties.isOptional()) {
						if (shouldTrace)
							System.out.println("Could not set " + field.getName()
									+ " because of the missing: " + context.getKey(properties));
						return false;
					}
					return true;
				}
			}
			return setField(userObject, field, value);
		}

		public boolean processMethod(final Method method, boolean optional)
				throws InvocationTargetException {
			if (Modifier.isStatic(method.getModifiers()) != processStatic)
				return true;
			// we only get here if we are injecting
			InjectionProperties[] properties = annotationSupport.getInjectParamProperties(method);
			if (descriptor != null) {
				// is it one of the arguments of this method?
				boolean found = false;
				String descriptorsKey = context.getKey(descriptor);
				for (int i = 0; i < properties.length; i++) {
					if (descriptorsKey.equals(context.getKey(properties[i]))) {
						found = true;
						break;
					}
				}
				if (!found)
					return true;
			}

			Object[] actualParams = processParams(properties, method.getParameterTypes(),
					!addition, injectWithNulls);
			if (actualParams != null)
				callMethod(userObject, method, actualParams);
			else if (!optional) {
				if (shouldTrace)
					System.out.println("Could not invoke " + method.getName()
							+ ": no matching context elements");
				return false;
			}
			return true;
		}

		public void addPostConstructMethod(Method method) {
			if (postConstructMethods == null)
				postConstructMethods = new ArrayList(1);
			postConstructMethods.add(method);
		}

		public void processPostConstructMethod() throws InvocationTargetException {
			if (!shouldProcessPostConstruct)
				return;
			if (postConstructMethods == null)
				return;
			for (Iterator it = postConstructMethods.iterator(); it.hasNext();) {
				Method method = (Method) it.next();
				InjectionProperties[] properties = annotationSupport
						.getInjectParamProperties(method);
				Object[] actualParams = processParams(properties, method.getParameterTypes(),
						!addition, injectWithNulls);
				if (actualParams == null)
					logError(userObject, new IllegalArgumentException());
				else
					callMethod(userObject, method, actualParams);
			}
			postConstructMethods.clear();
		}

	}

	// TBD investigate if this approach to reparenting works with calculated values and providers
	private class ReparentProcessor extends Processor {

		private IObjectProvider oldParent;

		public ReparentProcessor(IObjectProvider oldParent) {
			super(null, true /* set */, false);
			this.oldParent = oldParent;
		}

		/**
		 * Returns whether the value associated with the given key is affected by the parent change.
		 */
		private boolean hasChanged(InjectionProperties key) {
			// if value is local then parent change has no effect
			// XXX this is incorrect
			// if (context.getLocal(key) != null)
			// return false;
			// XXX this is incorrect: different parents, same grandparent
			// Object oldValue = oldParent == null ? null : oldParent.internalGet(
			// (EclipseContext) context, key, null, false);
			// Object newValue = context == null ? null : ((EclipseContext) context).internalGet(
			// (EclipseContext) context, key, null, false);
			// return oldValue != newValue;

			// XXX for now, check if values are different
			Object oldValue = oldParent.get(key);
			Object newValue = context.get(key);
			return (oldValue != newValue); // use pointer comparison, not #equals()
		}

		public boolean processField(final Field field, InjectionProperties properties) {
			if (hasChanged(properties))
				return super.processField(field, properties);
			return true;
		}

		public boolean processMethod(final Method method, boolean optional)
				throws InvocationTargetException {
			// any argument changed?
			InjectionProperties[] properties = annotationSupport.getInjectParamProperties(method);

			boolean changed = false;
			for (int i = 0; i < properties.length; i++) {
				if (hasChanged(properties[i])) {
					changed = true;
					break;
				}
			}
			if (changed)
				return super.processMethod(method, optional);
			return true;
		}

	}

	final static private String DEBUG_INJECTOR = "org.eclipse.e4.core.services/debug/injector"; //$NON-NLS-1$
	final static private boolean shouldTrace = ServicesActivator.getDefault()
			.getBooleanDebugOption(DEBUG_INJECTOR, false);
	final static private String JAVA_OBJECT = "java.lang.Object"; //$NON-NLS-1$

	// TBD rename objectProvider
	final protected IObjectProvider context;
	final private AnnotationsSupport annotationSupport;

	protected WeakRefList userObjects = new WeakRefList(3); // start small

	public ContextInjector(IObjectProvider context) {
		this.context = context;
		// plug-in class that gets replaced in Java 1.5+
		annotationSupport = new AnnotationsSupport(context);
	}

	public void added(IObjectDescriptor descriptor) {
		Object[] objectsCopy = userObjects.getSafeCopy();
		Processor processor = new Processor(descriptor, true, false);
		for (int i = 0; i < objectsCopy.length; i++) {
			try {
				processClassHierarchy(objectsCopy[i], processor);
			} catch (InvocationTargetException e) {
				logExternalError("Exception occured while processing addition on", objectsCopy[i],
						e);
			}
		}
	}

	public boolean inject(Object userObject) {
		Processor processor = new Processor(null, true, false);
		processor.shouldProcessPostConstruct = true;
		boolean result = false;
		try {
			result = processClassHierarchy(userObject, processor);
		} catch (InvocationTargetException e) {
			logExternalError("Exception occured while processing injecting", userObject, e);
		}
		userObjects.add(userObject);
		return result;
	}

	// TBD use null object to inject statics
	public boolean injectStatic(Class clazz) {
		Processor processor = new Processor(null, true, false);
		processor.shouldProcessPostConstruct = true;
		processor.setProcessStatic(true);
		try {
			Object object = make(clazz);
			return processClassHierarchy(object, processor);
		} catch (InvocationTargetException e) {
			// try-catch won't be necessary once we stop creating an object
			e.printStackTrace();
		} catch (InstantiationException e) {
			// try-catch won't be necessary once we stop creating an object
			e.printStackTrace();
		}
		return false;
	}

	public void removed(IObjectDescriptor descriptor) {
		Processor processor = new Processor(descriptor, false, false);
		Object[] objectsCopy = userObjects.getSafeCopy();
		for (int i = 0; i < objectsCopy.length; i++) {
			try {
				processClassHierarchy(objectsCopy[i], processor);
			} catch (InvocationTargetException e) {
				logExternalError("Exception occured while processing removal on", objectsCopy[i], e);
			}
		}
	}

	public boolean uninject(Object releasedObject) {
		if (!userObjects.remove(releasedObject))
			return false;
		Processor processor = new Processor(null, false, false);
		processor.setInjectNulls(true);
		try {
			return processClassHierarchy(releasedObject, processor);
		} catch (InvocationTargetException e) {
			logExternalError("Exception occured while uninjecting", releasedObject, e);
		}
		return false;
	}

	public void dispose() {
		Object[] objectsCopy = userObjects.getSafeCopy();
		Processor processor = new Processor(null, false, true);
		processor.setInjectNulls(true);
		for (int i = 0; i < objectsCopy.length; i++) {
			if (objectsCopy[i] instanceof IDisposable)
				((IDisposable) objectsCopy[i]).dispose();
			try {
				processClassHierarchy(objectsCopy[i], processor);
			} catch (InvocationTargetException e) {
				logExternalError("Exception occured while disposing", objectsCopy[i], e);
			}
		}
	}

	public void reparent(IObjectProvider oldParent) {
		if (oldParent == context)
			return;
		Object[] objectsCopy = userObjects.getSafeCopy();
		Processor processor = new ReparentProcessor(oldParent);
		for (int i = 0; i < objectsCopy.length; i++) {
			try {
				processClassHierarchy(objectsCopy[i], processor);
			} catch (InvocationTargetException e) {
				logExternalError("Exception occured while reparenting", objectsCopy[i], e);
			}
		}
	}

	/**
	 * Make the processor visit all declared members on the given class and all superclasses
	 * 
	 * @throws InvocationTargetException
	 */
	private boolean processClass(Class objectsClass, Processor processor)
			throws InvocationTargetException {
		if (processor.addition) {
			// order: superclass, fields, methods
			if (objectsClass != null) {
				Class superClass = objectsClass.getSuperclass();
				if (!superClass.getName().equals(JAVA_OBJECT)) {
					processor.classHierarchy.add(objectsClass);
					if (!processClass(superClass, processor))
						return false;
					processor.classHierarchy.remove(objectsClass);
				}
			}
			if (!processFields(objectsClass, processor))
				return false;
			if (!processMethods(objectsClass, processor))
				return false;
		} else {
			// order: methods, fields, superclass
			if (!processMethods(objectsClass, processor))
				return false;
			if (!processFields(objectsClass, processor))
				return false;
			if (objectsClass != null) {
				Class superClass = objectsClass.getSuperclass();
				if (!superClass.getName().equals(JAVA_OBJECT)) {
					processor.classHierarchy.add(objectsClass);
					if (!processClass(superClass, processor))
						return false;
					processor.classHierarchy.remove(objectsClass);
				}
			}
		}
		return true;
	}

	private boolean processClassHierarchy(Object userObject, Processor processor)
			throws InvocationTargetException {
		processor.setObject(userObject);
		if (!processClass((userObject == null) ? null : userObject.getClass(), processor))
			return false;
		processor.processPostConstructMethod();
		return true;
	}

	/**
	 * Make the processor visit all declared fields on the given class.
	 */
	private boolean processFields(Class objectsClass, Processor processor) {
		Field[] fields = objectsClass.getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			Field field = fields[i];

			InjectionProperties properties = annotationSupport.getInjectProperties(field);
			if (field.getName().startsWith(IContextConstants.INJECTION_PREFIX))
				properties.setInject(true);

			if (!properties.shouldInject())
				continue;
			if (!processor.processField(field, properties))
				return false;
		}
		return true;
	}

	/**
	 * Make the processor visit all declared methods on the given class.
	 * 
	 * @throws InvocationTargetException
	 */
	private boolean processMethods(Class objectsClass, Processor processor)
			throws InvocationTargetException {
		Method[] methods = objectsClass.getDeclaredMethods();
		if (processor.isInDispose) {
			for (int i = 0; i < methods.length; i++) {
				Method method = methods[i];
				if (method.getParameterTypes().length > 0) // TBD why?
					continue;
				if (!annotationSupport.isPreDestory(method))
					continue;
				if (!isOverridden(method, processor))
					callMethod(processor.userObject, method, null);
			}
		}
		for (int i = 0; i < methods.length; i++) {
			Method method = methods[i];
			if (isOverridden(method, processor))
				continue; // process in the subclass
			if (processor.shouldProcessPostConstruct) {
				if (isPostConstruct(method)) {
					processor.addPostConstructMethod(method);
					continue;
				}
			}

			InjectionProperties properties = annotationSupport.getInjectProperties(method);
			if (method.getName().startsWith(IContextConstants.INJECTION_PREFIX))
				properties.setInject(true);

			if (!properties.shouldInject())
				continue;
			if (!processor.processMethod(method, properties.isOptional()))
				return false;
		}
		return true;
	}

	// TBD simplify this: only one non-annotation and one "implements IInitializable"?
	/**
	 * Returns whether the given method is a post-construction process method, as defined by the
	 * class comment of {@link ContextInjectionFactory}.
	 */
	private boolean isPostConstruct(Method method) {
		boolean isPostConstruct = annotationSupport.isPostConstruct(method);
		if (isPostConstruct)
			return true;
		if (!method.getName().equals(IContextConstants.INJECTION_SET_CONTEXT_METHOD))
			return false;
		Class[] parms = method.getParameterTypes();
		if (parms.length == 0)
			return true;
		if (parms.length == 1 && parms[0].equals(IEclipseContext.class))
			return true;
		return false;
	}

	/**
	 * Checks if a given method is overridden with an injectable method.
	 */
	private boolean isOverridden(Method method, Processor processor) {
		int modifiers = method.getModifiers();
		if (Modifier.isPrivate(modifiers))
			return false;
		if (Modifier.isStatic(modifiers))
			return false;
		// method is not private if we reached this line, check not(public OR protected)
		boolean isDefault = !(Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers));

		for (Iterator i = processor.classHierarchy.iterator(); i.hasNext();) {
			Class subClass = (Class) i.next();
			Method override = null;
			try {
				override = subClass.getDeclaredMethod(method.getName(), method.getParameterTypes());
			} catch (SecurityException e) {
				continue;
			} catch (NoSuchMethodException e) {
				continue; // this is the desired outcome
			}
			if (override != null) {
				if (isDefault) { // must be in the same package to override
					Package originalPackage = method.getDeclaringClass().getPackage();
					Package overridePackage = subClass.getPackage();

					if (originalPackage == null && overridePackage == null)
						return true;
					if (originalPackage == null || overridePackage == null)
						return false;
					if (originalPackage.equals(overridePackage))
						return true;
				} else
					return true;
			}
		}
		return false;
	}

	public Object invoke(Object userObject, String methodName) throws InvocationTargetException,
			CoreException {
		Method[] methods = userObject.getClass().getDeclaredMethods();
		for (int j = 0; j < methods.length; j++) {
			Method method = methods[j];
			if (!method.getName().equals(methodName))
				continue;

			InjectionProperties[] properties = annotationSupport.getInjectParamProperties(method);
			Object[] actualParams = processParams(properties, method.getParameterTypes(), false,
					false);
			if (actualParams != null)
				return callMethod(userObject, method, actualParams);
		}
		IStatus status = new Status(IStatus.ERROR, "org.eclipse.e4.core.services",
				"Unable to find matching method to invoke");
		throw new CoreException(status);
	}

	public Object invoke(Object userObject, String methodName, Object defaultValue)
			throws InvocationTargetException {
		Method[] methods = userObject.getClass().getDeclaredMethods();
		for (int j = 0; j < methods.length; j++) {
			Method method = methods[j];
			if (!method.getName().equals(methodName))
				continue;

			InjectionProperties[] properties = annotationSupport.getInjectParamProperties(method);
			Object[] actualParams = processParams(properties, method.getParameterTypes(), false,
					false);
			if (actualParams != null)
				return callMethod(userObject, method, actualParams);
		}
		return defaultValue;
	}

	public Object make(Class clazz) throws InvocationTargetException, InstantiationException {
		Constructor[] constructors = clazz.getDeclaredConstructors();

		// Sort the constructors by descending number of constructor arguments
		ArrayList sortedConstructors = new ArrayList(constructors.length);
		for (int i = 0; i < constructors.length; i++)
			sortedConstructors.add(constructors[i]);
		Collections.sort(sortedConstructors, new Comparator() {
			public int compare(Object c1, Object c2) {
				int l1 = ((Constructor) c1).getParameterTypes().length;
				int l2 = ((Constructor) c2).getParameterTypes().length;
				return l2 - l1;
			}
		});

		for (Iterator i = sortedConstructors.iterator(); i.hasNext();) {
			Constructor constructor = (Constructor) i.next();

			// skip private and protected constructors; allow public and package visibility
			if (((constructor.getModifiers() & Modifier.PRIVATE) != 0)
					|| ((constructor.getModifiers() & Modifier.PROTECTED) != 0))
				continue;

			// unless this is the default constructor, it has to be tagged
			InjectionProperties cProps = annotationSupport.getInjectProperties(constructor);
			if (!cProps.shouldInject() && constructor.getParameterTypes().length != 0)
				continue;

			InjectionProperties[] properties = annotationSupport
					.getInjectParamsProperties(constructor);
			Object[] actualParams = processParams(properties, constructor.getParameterTypes(),
					false, false);
			if (actualParams == null)
				continue;
			Object newInstance = callConstructor(constructor, actualParams);
			if (newInstance != null)
				return newInstance;
		}

		if (shouldTrace)
			System.out
					.println("Could not find satisfiable constructor in class " + clazz.getName());
		return null;
	}

	private Object[] processParams(InjectionProperties[] properties, Class[] parameterTypes,
			boolean ignoreMissing, boolean injectWithNulls) {
		Object[] actualParams = new Object[properties.length];
		for (int i = 0; i < actualParams.length; i++) {
			// 1) if we have a provider, use it
			Object provider = properties[i].getProvider();
			if (provider != null) {
				actualParams[i] = provider;
				continue;
			}
			// 2) if we have the key in the context
			if (context.containsKey(properties[i])) {
				if (injectWithNulls) {
					actualParams[i] = null;
					continue;
				} else {
					Object candidate = context.get(properties[i]);
					if (candidate != null
							&& parameterTypes[i].isAssignableFrom(candidate.getClass())) {
						actualParams[i] = candidate;
						continue;
					}
				}
			}
			// 3) can we ignore this argument?
			if (ignoreMissing || properties[i].isOptional()) {
				actualParams[i] = null;
				continue;
			}
			return null;
		}
		return actualParams;
	}

	private boolean setField(Object userObject, Field field, Object value) {
		if ((value != null) && !field.getType().isAssignableFrom(value.getClass())) {
			// TBD add debug option
			return false;
		}

		boolean wasAccessible = true;
		if (!field.isAccessible()) {
			field.setAccessible(true);
			wasAccessible = false;
		}
		try {
			field.set(userObject, value);
		} catch (IllegalArgumentException e) {
			logError(field, e);
			return false;
		} catch (IllegalAccessException e) {
			logError(field, e);
			return false;
		} finally {
			if (!wasAccessible)
				field.setAccessible(false);
		}
		return true;
	}

	private Object callMethod(Object userObject, Method method, Object[] args)
			throws InvocationTargetException {
		Object result = null;
		boolean wasAccessible = true;
		if (!method.isAccessible()) {
			method.setAccessible(true);
			wasAccessible = false;
		}
		try {
			result = method.invoke(userObject, args);
		} catch (IllegalArgumentException e) {
			// should not happen, is checked during formation of the array of actual arguments
			logError(method, e);
			return null;
		} catch (IllegalAccessException e) {
			// should not happen, is checked at the start of this method
			logError(method, e);
			return null;
		} finally {
			if (!wasAccessible)
				method.setAccessible(false);
		}
		return result;
	}

	private Object callConstructor(Constructor constructor, Object[] args)
			throws InvocationTargetException, InstantiationException {
		if (args != null) { // make sure args are assignable
			Class[] parameterTypes = constructor.getParameterTypes();
			if (parameterTypes.length != args.length) {
				// internal error, log it
				logError(constructor, new IllegalArgumentException());
				return null;
			}
			for (int i = 0; i < args.length; i++) {
				if ((args[i] != null) && !parameterTypes[i].isAssignableFrom(args[i].getClass()))
					return null;
			}
		}

		Object result = null;
		boolean wasAccessible = true;
		if (!constructor.isAccessible()) {
			constructor.setAccessible(true);
			wasAccessible = false;
		}
		try {
			result = constructor.newInstance(args);
		} catch (IllegalArgumentException e) {
			// should not happen, is checked at the start of this method
			logError(constructor, e);
			return null;
		} catch (IllegalAccessException e) {
			// should not happen as we set constructor to be accessible
			logError(constructor, e);
			return null;
		} finally {
			if (!wasAccessible)
				constructor.setAccessible(false);
		}
		return result;
	}

	private void logExternalError(String msg, Object destination, Exception e) {
		System.out.println(msg + " " + destination.toString()); //$NON-NLS-1$
		if (e != null)
			e.printStackTrace();
		// TBD convert this into real logging
		// String msg = NLS.bind("Injection failed", destination.toString());
		// RuntimeLog.log(new Status(IStatus.WARNING,
		// IRuntimeConstants.PI_COMMON, 0, msg, e));
	}

	private void logError(Object destination, Exception e) {
		System.out.println("Injection failed " + destination.toString()); //$NON-NLS-1$
		if (e != null)
			e.printStackTrace();
		// TBD convert this into real logging
		// String msg = NLS.bind("Injection failed", destination.toString());
		// RuntimeLog.log(new Status(IStatus.WARNING,
		// IRuntimeConstants.PI_COMMON, 0, msg, e));
	}

}
