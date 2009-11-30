/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.e4.core.services.internal.context;

import java.lang.reflect.InvocationTargetException;
import junit.framework.TestCase;
import org.eclipse.e4.core.services.context.EclipseContextFactory;
import org.eclipse.e4.core.services.context.IEclipseContext;
import org.eclipse.e4.core.services.context.spi.ContextInjectionFactory;

public class ContextInjectionFactoryTest extends TestCase {

	static class TestObject {

		private int executed = 0;

		private int executedWithParams = 0;

		public void execute() {
			executed++;
		}

		public void executeWithParams(String string) {
			executedWithParams++;
		}

		public int getExecuted() {
			return executed;
		}

		public int getExecutedWithParams() {
			return executedWithParams;
		}

	}

	static class TestConstructorObjectBasic {
		public boolean defaultConstructorCalled = false;

		public TestConstructorObjectBasic() {
			defaultConstructorCalled = true;
		}
	}

	private TestObject testObject;
	private IEclipseContext context;

	protected void setUp() throws Exception {
		super.setUp();

		testObject = new TestObject();
		context = EclipseContextFactory.create();
	}

	public void testInvoke() throws Exception {
		ContextInjectionFactory.invoke(testObject, "execute", context, null);

		assertEquals(1, testObject.getExecuted());
		assertEquals(0, testObject.getExecutedWithParams());
	}

	public void testInvokeWithParameters() throws Exception {
		context.set(String.class.getName(), "");

		ContextInjectionFactory.invoke(testObject, "executeWithParams", context, null);

		assertEquals(0, testObject.getExecuted());
		assertEquals(1, testObject.getExecutedWithParams());
	}

	/**
	 * If no other constructors are available, the default constructor should be used
	 */
	public void testConstructorInjectionBasic() throws InvocationTargetException,
			InstantiationException {
		IEclipseContext context = EclipseContextFactory.create();
		// add an extra argument for the inner class constructors
		context.set(ContextInjectionFactoryTest.class.getName(), this);

		Object basicResult = ContextInjectionFactory
				.make(TestConstructorObjectBasic.class, context);
		assertNotNull(basicResult);
		assertTrue(basicResult instanceof TestConstructorObjectBasic);
		assertTrue(((TestConstructorObjectBasic) basicResult).defaultConstructorCalled);
	}
}
