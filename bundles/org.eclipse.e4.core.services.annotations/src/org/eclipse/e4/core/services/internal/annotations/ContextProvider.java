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
package org.eclipse.e4.core.services.internal.annotations;

import javax.inject.Provider;

import org.eclipse.e4.core.services.injector.IObjectProvider;
import org.eclipse.e4.core.services.internal.context.InjectionProperties;

// TBD rename ProviderImpl
public class ContextProvider<T> implements Provider<T> {
	
	private IObjectProvider objectProvider;
	private InjectionProperties properties;
	
	public ContextProvider(String name, Class<?> clazz, IObjectProvider objectProvider) {
		properties = new InjectionProperties(true, name, false, clazz);
		this.objectProvider = objectProvider;
	}
	
	@SuppressWarnings("unchecked")
	public T get() {
		try {
			return (T) objectProvider.get(properties);
		} catch (ClassCastException e) {
			return null;
		}
	}

}
