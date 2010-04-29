/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.core.internal.di;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import org.eclipse.e4.core.di.IInjector;
import org.eclipse.e4.core.di.InjectionException;
import org.eclipse.e4.core.di.suppliers.AbstractObjectSupplier;
import org.eclipse.e4.core.di.suppliers.IObjectDescriptor;

public class ConstructorRequestor extends Requestor {

	final private Constructor<?> constructor;

	public ConstructorRequestor(Constructor<?> constructor, IInjector injector, AbstractObjectSupplier primarySupplier) {
		// TBD make an integer update types? 0 - static , 1 - normal, 2 - grouped?
		super(null, injector, primarySupplier, null, false /* do not track */);
		this.constructor = constructor;
	}

	public Object execute() throws InjectionException {
		return callConstructor(constructor, actualArgs);
	}

	@Override
	public IObjectDescriptor[] getDependentObjects() {
		Annotation[][] annotations = constructor.getParameterAnnotations();
		Type[] logicalParams = constructor.getGenericParameterTypes();
		// JDK bug: different methods see / don't see generated args for nested classes
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5087240
		Class<?>[] compilerParams = constructor.getParameterTypes();
		if (compilerParams.length > logicalParams.length) {
			Type[] tmp = new Type[compilerParams.length];
			System.arraycopy(compilerParams, 0, tmp, 0, compilerParams.length - logicalParams.length);
			System.arraycopy(logicalParams, 0, tmp, compilerParams.length - logicalParams.length, logicalParams.length);
			logicalParams = tmp;
		}

		IObjectDescriptor[] descriptors = new IObjectDescriptor[logicalParams.length];
		for (int i = 0; i < logicalParams.length; i++) {
			descriptors[i] = new ObjectDescriptor(logicalParams[i], annotations[i]);
		}
		return descriptors;
	}

	private Object callConstructor(Constructor<?> constructor, Object[] args) throws InjectionException {
		Object result = null;
		boolean wasAccessible = true;
		if (!constructor.isAccessible()) {
			constructor.setAccessible(true);
			wasAccessible = false;
		}
		try {
			result = constructor.newInstance(args);
		} catch (IllegalArgumentException e) {
			throw new InjectionException(e);
		} catch (InstantiationException e) {
			throw new InjectionException(e);
		} catch (IllegalAccessException e) {
			throw new InjectionException(e);
		} catch (InvocationTargetException e) {
			Throwable originalException = e.getCause();
			throw new InjectionException((originalException != null) ? originalException : e);
		} finally {
			if (!wasAccessible)
				constructor.setAccessible(false);
		}
		return result;
	}

	@Override
	public String toString() {
		StringBuffer tmp = new StringBuffer();
		Object object = getRequestingObject();
		if (object != null)
			tmp.append(object.getClass().getSimpleName());
		tmp.append('(');
		tmp.append(')');
		return tmp.toString();
	}

}
