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
package org.eclipse.e4.core.internal.contexts;

import org.eclipse.e4.core.di.suppliers.AbstractObjectSupplier;

import org.eclipse.e4.core.di.suppliers.IObjectDescriptor;

import org.eclipse.e4.core.di.suppliers.IRequestor;

import javax.inject.Named;
import org.eclipse.e4.core.contexts.ContextChangeEvent;
import org.eclipse.e4.core.contexts.IContextConstants;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.contexts.IRunAndTrack;
import org.eclipse.e4.core.di.IInjector;

public class ContextObjectSupplier extends AbstractObjectSupplier {

	final static protected String ECLIPSE_CONTEXT_NAME = IEclipseContext.class.getName();

	static private class ContextInjectionListener implements IRunAndTrackObject {

		final private Object[] result;
		final private String[] keys;
		final private IRequestor requestor;
		final private IEclipseContext context;

		public ContextInjectionListener(IEclipseContext context, Object[] result, String[] keys, IRequestor requestor) {
			this.result = result;
			this.keys = keys;
			this.requestor = requestor;
			this.context = context;
		}

		public boolean notify(ContextChangeEvent event, final IContextRecorder recorder) {
			if (event.getEventType() == ContextChangeEvent.INITIAL) {
				// needs to be done inside runnable to establish dependencies
				for (int i = 0; i < keys.length; i++) {
					if (keys[i] == null)
						continue;
					if (ECLIPSE_CONTEXT_NAME.equals(keys[i])) {
						result[i] = context;
						context.get(IContextConstants.PARENT); // creates pseudo-link
					} else if (context.containsKey(keys[i]))
						result[i] = context.get(keys[i]);
					else
						result[i] = IInjector.NOT_A_VALUE; // TBD make sure this still creates
															// dependency on the key
				}
				return true;
			}

			IInjector injector = requestor.getInjector();
			if (event.getEventType() == ContextChangeEvent.DISPOSE) {
				IEclipseContext originatingContext = event.getContext();
				if (originatingContext == context) {
					ContextObjectSupplier originatingSupplier = getObjectSupplier(originatingContext, injector);
					injector.disposed(originatingSupplier);
					return false;
				}
			} else if (event.getEventType() == ContextChangeEvent.UNINJECTED) {
				IEclipseContext originatingContext = event.getContext();
				if (originatingContext == context) {
					ContextObjectSupplier originatingSupplier = getObjectSupplier(originatingContext, injector);
					injector.uninject(event.getArguments()[0], originatingSupplier);
					return false;
				}
			} else {
				injector.resolveArguments(requestor, requestor.getPrimarySupplier());
				if (recorder != null)
					recorder.stopAccessRecording();
				try {
					requestor.execute();
				} finally {
					if (recorder != null)
						recorder.startAcessRecording();
				}
			}
			return true;
		}

		public boolean notify(ContextChangeEvent event) {
			return notify(event, null);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((context == null) ? 0 : context.hashCode());
			result = prime * result + ((requestor == null) ? 0 : requestor.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ContextInjectionListener other = (ContextInjectionListener) obj;
			if (context == null) {
				if (other.context != null)
					return false;
			} else if (!context.equals(other.context))
				return false;
			if (requestor == null) {
				if (other.requestor != null)
					return false;
			} else if (!requestor.equals(other.requestor))
				return false;
			return true;
		}

		public boolean batchProcess() {
			return requestor.shouldGroupUpdates();
		}

	}

	final private IEclipseContext context;

	public ContextObjectSupplier(IEclipseContext context, IInjector injector) {
		this.context = context;
	}

	public IEclipseContext getContext() {
		return context;
	}

	@Override
	public Object get(IObjectDescriptor descriptor, IRequestor requestor) {
		// This method is rarely or never used on the primary supplier
		if (descriptor == null)
			return IInjector.NOT_A_VALUE;
		Object[] result = get(new IObjectDescriptor[] {descriptor}, requestor);
		if (result == null)
			return null;
		return result[0];
	}

	@Override
	public Object[] get(IObjectDescriptor[] descriptors, final IRequestor requestor) {
		final Object[] result = new Object[descriptors.length];
		final String[] keys = new String[descriptors.length];

		for (int i = 0; i < descriptors.length; i++) {
			keys[i] = (descriptors[i] == null) ? null : getKey(descriptors[i]);
		}

		if (requestor != null && requestor.shouldTrack()) { // only track if requested
			IRunAndTrack trackable = new ContextInjectionListener(context, result, keys, requestor);
			context.runAndTrack(trackable, null);
		} else {
			for (int i = 0; i < descriptors.length; i++) {
				if (keys[i] == null)
					continue;
				if (ECLIPSE_CONTEXT_NAME.equals(keys[i]))
					result[i] = context;
				else if (context.containsKey(keys[i]))
					result[i] = context.get(keys[i]);
				else
					result[i] = IInjector.NOT_A_VALUE;
			}
		}
		return result;
	}

	private String getKey(IObjectDescriptor descriptor) {
		if (descriptor.hasQualifier(Named.class)) {
			Object namedAnnotation = descriptor.getQualifier(Named.class);
			String key = ((Named) namedAnnotation).value();
			return key;
		}
		Class<?> elementClass = descriptor.getElementClass();
		if (elementClass != null)
			return elementClass.getName();
		return null;
	}

	static public ContextObjectSupplier getObjectSupplier(IEclipseContext context, IInjector injector) {
		ContextObjectSupplier supplier = context.getLocal(ContextObjectSupplier.class);
		if (supplier != null)
			return supplier;
		ContextObjectSupplier objectSupplier = new ContextObjectSupplier(context, injector);
		context.set(ContextObjectSupplier.class, objectSupplier);
		return objectSupplier;
	}

}
