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

import org.eclipse.e4.core.services.context.IEclipseContext;
import org.eclipse.e4.core.services.context.IRunAndTrack;
import org.eclipse.e4.core.services.injector.IObjectDescriptor;
import org.eclipse.e4.core.services.injector.IObjectProvider;
import org.eclipse.e4.core.services.injector.Injector;

public class ObjectProviderContext implements IObjectProvider {

	final static private String ECLIPSE_CONTEXT_NAME = IEclipseContext.class.getName();

	final private IEclipseContext context;
	private Injector injector;

	public ObjectProviderContext(IEclipseContext context) {
		this.context = context;
	}

	public boolean containsKey(IObjectDescriptor properties) {
		String key = getKey(properties);
		if (key == null)
			return false;
		if (ECLIPSE_CONTEXT_NAME.equals(key))
			return (context != null);
		return context.containsKey(key);
	}

	public Object get(IObjectDescriptor properties) {
		String key = getKey(properties);
		if (key == null)
			return null;
		if (ECLIPSE_CONTEXT_NAME.equals(key))
			return context;
		return context.get(key);
	}

	public void setInjector(Injector injector) {
		this.injector = injector;
	}

	public Injector getInjector() {
		return injector;
	}

	public String getKey(IObjectDescriptor key) {
		String result = key.getPropertyName();
		if (result != null)
			return result;
		Class elementClass = key.getElementClass();
		if (elementClass != null)
			return elementClass.getName();
		return null;
	}

	public String toString() {
		return "ContextToInjectorLink(" + context + ')'; //$NON-NLS-1$
	}

	public IEclipseContext getContext() {
		return context;
	}

	// TBD remove?
	static public ObjectProviderContext getObjectProvider(IEclipseContext context) {
		String key = ObjectProviderContext.class.getName();
		if (context.containsKey(key, true))
			return (ObjectProviderContext) context.get(key);
		ObjectProviderContext objectProvider = new ObjectProviderContext(context);
		Injector injector = new Injector(objectProvider);
		objectProvider.setInjector(injector);
		context.set(key, objectProvider);
		return objectProvider;
	}

	public void init(Object userObject) {
		injector.inject(userObject);
	}

	public void runAndTrack(final IRunAndTrack runnable, Object[] args) {
		context.runAndTrack(runnable, args);
	}

	// TBD remove?
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof ObjectProviderContext))
			return false;
		return context.equals(((ObjectProviderContext) obj).context);
	}

	// TBD remove?
	public int hashCode() {
		return context.hashCode();
	}
}
