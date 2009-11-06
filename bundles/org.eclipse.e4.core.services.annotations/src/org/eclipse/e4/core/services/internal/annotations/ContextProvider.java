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

import org.eclipse.e4.core.services.context.IEclipseContext;
import org.eclipse.e4.core.services.internal.context.IContextProvider;


public class ContextProvider<T> implements Provider<T>, IContextProvider {
	
	private IEclipseContext context;
	private Class<?> clazz;
	private String name;
	
	
	public ContextProvider(String name, Class<?> clazz) {
		this.name = name;
		this.clazz = clazz;
	}
	
	public void setContext(IEclipseContext context) {
		this.context = context;
	}
	
	// TBD scope processing
	@SuppressWarnings("unchecked")
	public T get() {
		if (name != null) {
			if (context.containsKey(name))
				return (T) context.get(name); // TBD where do we put class type checks?
		}
		if (clazz == null)
			return null;
		// is there such class already described in the context?
		String key = clazz.getName();
		if (context.containsKey(key))
			return (T) context.get(key); // TBD where do we put class type checks?
		return (T) context.make(clazz);
	}

}
