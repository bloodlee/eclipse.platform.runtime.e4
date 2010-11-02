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
package org.eclipse.e4.core.internal.tests.di;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import junit.framework.TestCase;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;

/**
 * Checks that injected objects that do not have normal links
 * established to the context are still notified on context
 * disposal. 
 * (No links: nothing was actually injected; or only IEclipseContext was injected;
 * or constructor injection was used.)
 * See bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=301462 . 
 */
public class DisposeClassLinkTest extends TestCase {

	public static class Test {
		private int count = 0;

		public int getCount() {
			return count;
		}

		@PreDestroy
		void preDestroy() {
			count++;
		}
	}
	
	public static class InjectionObject {

	    @Inject
	    IEclipseContext context;

	    int postConstruct = 0;
	    int preDestroy = 0;

	    @PostConstruct
	    void construct() {
	        postConstruct++;
	    }

	    @PreDestroy
	    void destroy() {
	        preDestroy++;
	    }

	}
	public void testMake() throws Exception {
		IEclipseContext context = EclipseContextFactory.create();
		Test test = (Test) ContextInjectionFactory.make(Test.class, context);

		assertEquals(0, test.getCount());
		context.dispose();
		assertEquals(1, test.getCount());
	}

	public void testDisposeParent() throws Exception {
		IEclipseContext parentContext = EclipseContextFactory.create();
		IEclipseContext context = parentContext.createChild();
		Test test = (Test) ContextInjectionFactory.make(Test.class, context);

		assertEquals(0, test.getCount());
		context.dispose();
		assertEquals(1, test.getCount());
		parentContext.dispose();
		assertEquals(1, test.getCount());
	}

	public void testInject() throws Exception {
		IEclipseContext parentContext = EclipseContextFactory.create();
		IEclipseContext context = parentContext.createChild();
		Test test = new Test();
		ContextInjectionFactory.inject(test, context);

		assertEquals(0, test.getCount());
		context.dispose();
		assertEquals(1, test.getCount());
	}

	public void testDisposeParentFirst() throws Exception {
		IEclipseContext parentContext = EclipseContextFactory.create();
		IEclipseContext context = parentContext.createChild();
		Test test = new Test();
		ContextInjectionFactory.inject(test, context);

		assertEquals(0, test.getCount());
		context.dispose();
		assertEquals(1, test.getCount());
		parentContext.dispose();
		assertEquals(1, test.getCount());
	}

	public void testInjectedWithContext() throws Exception {
	    IEclipseContext context = EclipseContextFactory.create();

	    InjectionObject obj = (InjectionObject) ContextInjectionFactory.make(InjectionObject.class, context);

	    assertEquals("The object has been injected with the context", context, obj.context);
	    assertEquals("@PostConstruct should have been called once", 1, obj.postConstruct);
	    assertEquals("@PreDestroy should not have been called", 0, obj.preDestroy);

	    context.dispose();

	    assertNull("The object should have been uninjected", obj.context);
	    assertEquals("@PostConstruct should only have been called once", 1, obj.postConstruct);
	    assertEquals("@PreDestroy should have been called during uninjection", 1, obj.preDestroy);
	}

}
