/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.e4.core.tests.services.internal.annotations;

import javax.inject.Inject;

import junit.framework.TestCase;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IContextConstants;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.IDisposable;
import org.eclipse.e4.core.internal.contexts.EclipseContext;
import org.eclipse.e4.core.internal.contexts.ILookupStrategy;

/**
 * Tests that a strategy is not still accessed after its context is disposed.
 */
public class Bug304585Test extends TestCase {

	public static class InjectFieldTarget {
		@Inject
		PrintService printer;
	}
	
	public static class InjectMethodTarget {
		@Inject
		public void setPrinter(PrintService printer) {
			//
		}
	}


	public void testFieldInjection() throws Exception {
		IEclipseContext parent = EclipseContextFactory.create("ParentContext");
		Strategy strategy = new Strategy();
		IEclipseContext child = new EclipseContext(parent, strategy);

		ContextInjectionFactory.make(InjectFieldTarget.class, child);

		((IDisposable) child).dispose();
		((IDisposable) parent).dispose();
		assertFalse("Strategy used after context disposed", strategy.lookupAfterDisposed);
	}

	public void testMethodInjection() throws Exception {
		IEclipseContext parent = EclipseContextFactory.create("ParentContext");
		Strategy strategy = new Strategy();
		IEclipseContext child = new EclipseContext(parent, strategy);

		ContextInjectionFactory.make(InjectMethodTarget.class, child);

		((IDisposable) child).dispose();
		((IDisposable) parent).dispose();
		assertFalse("Strategy used after context disposed", strategy.lookupAfterDisposed);
	}

	public static class Strategy implements IDisposable, ILookupStrategy {
		private boolean disposed = false;
		boolean lookupAfterDisposed = false;
		private PrintService printer = new StringPrintService();

		public void dispose() {
			disposed = true;
		}

		public Object lookup(String name, IEclipseContext context) {
			if (disposed)
				lookupAfterDisposed = true;
			if (name != null && name.equals(PrintService.SERVICE_NAME)) {
				return printer;
			}
			return null;
		}

		public boolean containsKey(String name, IEclipseContext context) {
			if (disposed)
				lookupAfterDisposed = true;
			return name != null && name.equals(PrintService.SERVICE_NAME);
		}

	}

}
