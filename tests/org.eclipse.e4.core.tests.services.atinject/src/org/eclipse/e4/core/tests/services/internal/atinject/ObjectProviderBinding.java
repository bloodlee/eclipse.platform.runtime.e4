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
package org.eclipse.e4.core.tests.services.internal.atinject;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.inject.Singleton;

import org.eclipse.e4.core.services.injector.IObjectDescriptor;
import org.eclipse.e4.core.services.injector.IObjectProvider;
import org.eclipse.e4.core.services.injector.Injector;

public class ObjectProviderBinding implements IObjectProvider {

	private Injector injector;

	public class KeyBinding {
		private String key;
		private String qualifier;
		private Class<?> clazz;

		public KeyBinding(String key, Class<?> clazz) {
			this(key, null, clazz);
		}
		
		public KeyBinding(String key, String qualifier, Class<?> clazz) {
			this.key = key;
			this.qualifier = qualifier;
			this.clazz = clazz;
		}

		public String getKey() {
			String result = key;
			if (qualifier != null)
				result += "," + qualifier;
			return result;
		}
		
		public Class<?> getValueClass() {
			return clazz;
		}
		
		public void inject(Class<?> clazz) {
			this.clazz = clazz;
		}
		
		public KeyBinding named(String qualifier) {
			this.qualifier = qualifier;
			return this;
		}
		
	}

	private ArrayList<KeyBinding> bindings = new ArrayList<KeyBinding>();
	
	private HashMap<Class<?>, Object> singletonCache = new HashMap<Class<?>, Object>();

	public ObjectProviderBinding() {
		// placeholder
	}

	public void addBinding(String key, Class<?> clazz) {
		bindings.add(new KeyBinding(key, clazz));
	}
	public void addBinding(String key, String qualifier, Class<?> clazz) {
		bindings.add(new KeyBinding(key, qualifier, clazz));
	}
	
	public KeyBinding addBinding(Class<?> clazz) {
		KeyBinding binding = new KeyBinding(clazz.getName(), clazz);
		bindings.add(binding);
		return binding;
	}
	
	public boolean containsKey(IObjectDescriptor properties) {
		return findBinding(properties) != null;
	}
	
	// Convenience access method - makes descriptor based on the class name
	public Object get(final Class<?> clazz) {
		IObjectDescriptor desc = new IObjectDescriptor() {
			public Class getElementClass() {
				return clazz;
			}
			public String getPropertyName() {
				return null;
			}
		};
		return get(desc);
	}

	public Object get(IObjectDescriptor properties) {
		KeyBinding binding = findBinding(properties);
		if (binding == null)
			return null;
		Class<?> clazz = binding.getValueClass();
		boolean isSingleton = clazz.isAnnotationPresent(Singleton.class);
		if (isSingleton && singletonCache.containsKey(clazz))
			return singletonCache.get(clazz);
		// TBD #make and #inject are separate operations due to IEclipseContext#runAndTrack.
		Object value;
		try {
			value = injector.make(binding.getValueClass());
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			return null;
		} catch (InstantiationException e) {
			e.printStackTrace();
			return null;
		}
		if (value != null)
			injector.inject(value);
		if (isSingleton)
			singletonCache.put(clazz, value);
		return value;
	}

	public void setInjector(Injector injector) {
		this.injector = injector;
	}
	
	public Injector getInjector() {
		return injector;
	}
	
	public String getKey(IObjectDescriptor key) {
		Class<?> elementClass = key.getElementClass();
		String result = (elementClass == null) ? "" : elementClass.getName();
		if (key.getPropertyName() != null)
			result += "," + key.getPropertyName();
		return result;
	}
	
	private KeyBinding findBinding(IObjectDescriptor properties) {
		String key = getKey(properties);
		for (KeyBinding binding : bindings) {
			if (key.equals(binding.getKey()))
				return binding;
		}
		return null;
	}

}
