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



import org.eclipse.e4.core.di.AbstractObjectSupplier;
import org.eclipse.e4.core.di.IInjector;
import org.eclipse.e4.core.di.IObjectDescriptor;
import org.eclipse.e4.core.di.ObjectDescriptorFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

public class ConstructorRequestor extends Requestor {

	final private Constructor<?> constructor;

	public ConstructorRequestor(Constructor<?> constructor, IInjector injector, AbstractObjectSupplier primarySupplier) {
		// TBD make an integer update types? 0 - static , 1 - normal, 2 - grouped?
		super(injector, primarySupplier, null,  false /* do not track */, false /* N/A: no updates */, false /* mandatory */);
		this.constructor = constructor;
	}

	public Object execute() throws InvocationTargetException, InstantiationException {
		return callConstructor(constructor, actualArgs);
	}

	@Override
	public IObjectDescriptor[] getDependentObjects() {
		// Class[] parameterTypes = constructor.getParameterTypes();
		Type[] parameterTypes = constructor.getGenericParameterTypes();
		InjectionProperties[] properties = annotationSupport.getInjectParamsProperties(constructor);
		IObjectDescriptor[] descriptors = new IObjectDescriptor[properties.length];
		for (int i = 0; i < properties.length; i++) {
			descriptors[i] = ObjectDescriptorFactory.make(parameterTypes[i], properties[i].getQualifiers());
		}
		return descriptors;
	}

	private Object callConstructor(Constructor<?> constructor, Object[] args)
			throws InvocationTargetException, InstantiationException {
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

}
