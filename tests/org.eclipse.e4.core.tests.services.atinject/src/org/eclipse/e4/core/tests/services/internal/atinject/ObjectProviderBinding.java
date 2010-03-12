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
package org.eclipse.e4.core.tests.services.internal.atinject;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.inject.Singleton;

import org.eclipse.e4.core.services.context.ContextChangeEvent;
import org.eclipse.e4.core.services.context.EclipseContextFactory;
import org.eclipse.e4.core.services.context.IRunAndTrack;
import org.eclipse.e4.core.services.injector.IObjectDescriptor;
import org.eclipse.e4.core.services.injector.IObjectProvider;
import org.eclipse.e4.core.services.injector.InjectorFactory;

public class ObjectProviderBinding implements IObjectProvider {

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
	
	public Object get(IObjectDescriptor properties) {
		KeyBinding binding = findBinding(properties);
		if (binding == null)
			return null;
		Class<?> clazz = binding.getValueClass();
		boolean isSingleton = clazz.isAnnotationPresent(Singleton.class);
		if (isSingleton && singletonCache.containsKey(clazz))
			return singletonCache.get(clazz);
		Object value;
		try {
			value = InjectorFactory.getInjector().make(binding.getValueClass(), this);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			return null;
		} catch (InstantiationException e) {
			e.printStackTrace();
			return null;
		}
		if (isSingleton)
			singletonCache.put(clazz, value);
		return value;
	}

	private KeyBinding findBinding(IObjectDescriptor properties) {
		String key = properties.getKey();
		for (KeyBinding binding : bindings) {
			if (key.equals(binding.getKey()))
				return binding;
		}
		return null;
	}

	public void runAndTrack(IRunAndTrack runnable, Object[] args) {
		// there is no dynamic support; this is one time only
		ContextChangeEvent event = EclipseContextFactory.createContextEvent(this, ContextChangeEvent.INITIAL, null, null, null); 
		runnable.notify(event);
	}

	public IObjectDescriptor makeDescriptor(String description, Class clazz) {
		return new ObjectDescriptorBinding(description, clazz);
	}

}
