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
package org.eclipse.e4.core.tests.services.internal.annotations;

import javax.inject.Inject;
import javax.inject.Named;

import junit.framework.TestCase;

import org.eclipse.e4.core.services.IDisposable;
import org.eclipse.e4.core.services.annotations.PreDestroy;
import org.eclipse.e4.core.services.context.EclipseContextFactory;
import org.eclipse.e4.core.services.context.IEclipseContext;
import org.eclipse.e4.core.services.context.spi.ContextInjectionFactory;

public class InjectionOrderTest extends TestCase {

	public static class InjectTargetMethod {
		Object o;

		@Inject
		void set(@Named("inject") Object o) {
			this.o = o;
		}

		@PreDestroy
		void pd() {
			assertNotNull(o);
		}
	}

	public static class InjectTargetField {
		@Inject @Named("inject")
		Object o;

		@PreDestroy
		void pd() {
			assertNotNull(o);
		}
	}

	public void testDisposeMethod() throws Exception {
		IEclipseContext appContext = EclipseContextFactory.create();
		appContext.set("inject", "a");

		ContextInjectionFactory.make(InjectTargetMethod.class, appContext);
		appContext.set("inject", "b");

		((IDisposable) appContext).dispose();
	}
	
	public void testDisposeField() throws Exception {
		IEclipseContext appContext = EclipseContextFactory.create();
		appContext.set("inject", "a");

		ContextInjectionFactory.make(InjectTargetField.class, appContext);
		appContext.set("inject", "b");

		((IDisposable) appContext).dispose();
	}
}
