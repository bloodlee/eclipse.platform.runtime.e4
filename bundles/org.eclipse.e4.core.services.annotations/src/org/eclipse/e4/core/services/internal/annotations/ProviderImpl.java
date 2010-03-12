/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
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

import org.eclipse.e4.core.services.injector.IObjectDescriptor;
import org.eclipse.e4.core.services.injector.IObjectProvider;

public class ProviderImpl<T> implements Provider<T> {
	
	final private IObjectProvider objectProvider;
	final private IObjectDescriptor objectDescriptor;
	
	public ProviderImpl(IObjectDescriptor descriptor, IObjectProvider provider) {
		objectDescriptor = descriptor;
		objectProvider = provider;
	}
	
	@SuppressWarnings("unchecked")
	public T get() {
		try {
			return (T) objectProvider.get(objectDescriptor);
		} catch (ClassCastException e) {
			return null;
		}
	}

}
