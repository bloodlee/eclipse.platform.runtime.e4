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
package org.eclipse.e4.core.tests.services.internal.annotations;

import java.lang.reflect.InvocationTargetException;

import javax.inject.Inject;
import javax.inject.Provider;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.e4.core.services.context.EclipseContextFactory;
import org.eclipse.e4.core.services.context.IEclipseContext;
import org.eclipse.e4.core.services.context.spi.ContextInjectionFactory;
import org.eclipse.e4.core.services.injector.IInjector;
import org.eclipse.e4.core.services.injector.InjectorFactory;

/**
 * Testing provider interface
 */
public class ProviderInjectionTest extends TestCase {

	static public class TestData {
		
		public String data;
		
		@Inject
		public TestData(String tmp) {
			data = tmp;
		}
	}
	
	static public class TestInvokeClass {
		public Provider<TestData> provider;
		public TestInvokeClass() {
			// placeholder
		}
		public int execute(Provider<TestData> arg) {
			provider = arg;
			return 1;
		}
	}
	
	static public class TestConstructorClass {
		public Provider<TestData> provider;
		
		@Inject
		public TestConstructorClass(Provider<TestData> provider) {
			this.provider = provider;
		}
	}
	
	public static Test suite() {
		return new TestSuite(ProviderInjectionTest.class);
	}

	public ProviderInjectionTest() {
		super();
	}

	public ProviderInjectionTest(String name) {
		super(name);
	}

	public synchronized void testInvokeWithProvider() throws InvocationTargetException {

		// create context
		IEclipseContext context = EclipseContextFactory.create();
		context.set(String.class.getName(), "abc");
		IInjector injector = InjectorFactory.getInjector();
		injector.addBinding(TestData.class);

		TestInvokeClass userObject = new TestInvokeClass();
		assertEquals(1, ContextInjectionFactory.invoke(userObject, "execute", context, null));
		
		assertNotNull(userObject.provider.get());
		assertEquals("abc", userObject.provider.get().data);
	}
	
	public synchronized void testConstructorWithProvider() throws InvocationTargetException, InstantiationException {
		// create context
		IEclipseContext context = EclipseContextFactory.create();
		context.set(String.class.getName(), "abc");
		IInjector injector = InjectorFactory.getInjector();
		injector.addBinding(TestData.class);

		TestConstructorClass userObject = (TestConstructorClass) ContextInjectionFactory.make(TestConstructorClass.class, context);
		
		assertNotNull(userObject);
		assertNotNull(userObject.provider);
		assertNotNull(userObject.provider.get());
		assertEquals("abc", userObject.provider.get().data);
	}

}
