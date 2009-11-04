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

import javax.inject.Inject;
import javax.inject.Named;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.e4.core.services.IDisposable;
import org.eclipse.e4.core.services.annotations.PostConstruct;
import org.eclipse.e4.core.services.annotations.PreDestroy;
import org.eclipse.e4.core.services.context.EclipseContextFactory;
import org.eclipse.e4.core.services.context.IEclipseContext;
import org.eclipse.e4.core.services.context.spi.ContextInjectionFactory;

/**
 * Tests for the basic context injection functionality
 */
public class AnnotationsInjectionTest extends TestCase {

	public static Test suite() {
		return new TestSuite(AnnotationsInjectionTest.class);
	}

	public AnnotationsInjectionTest() {
		super();
	}

	public AnnotationsInjectionTest(String name) {
		super(name);
	}

	public void testContextSetOneArg() {
		class TestData {
		}
		class Injected {
			int contextSetCalled = 0;
			int setMethodCalled = 0;

			public TestData value;

			@SuppressWarnings("unused")
			@Inject
			public void settings(IEclipseContext context) {
				contextSetCalled++;
			}

			@SuppressWarnings("unused")
			@Inject
			public void injectedMethod(@Named("testing123") TestData arg) {
				setMethodCalled++;
				value = arg;
			}
		}
		IEclipseContext context = EclipseContextFactory.create();
		TestData methodValue = new TestData();
		context.set("testing123", methodValue);
		Injected object = new Injected();
		ContextInjectionFactory.inject(object, context);
		assertEquals(1, object.setMethodCalled);
		assertEquals(1, object.contextSetCalled);

		TestData methodValue2 = new TestData();
		context.set("testing123", methodValue2);
		assertEquals(2, object.setMethodCalled);
		assertEquals(methodValue2, object.value);
		assertEquals(1, object.contextSetCalled);
	}

	public void testPostConstruct() {
		class TestData {
		}
		class Injected {
			int postConstructCalled = 0;
			int setMethodCalled = 0;
			public TestData value;

			@SuppressWarnings("unused")
			@PostConstruct
			public void init() {
				postConstructCalled++;
			}

			@SuppressWarnings("unused")
			@Inject
			public void setData(TestData arg) {
				setMethodCalled++;
				value = arg;
			}
		}
		IEclipseContext context = EclipseContextFactory.create();
		TestData methodValue = new TestData();
		context.set(TestData.class.getName(), methodValue);
		Injected object = new Injected();
		ContextInjectionFactory.inject(object, context);
		assertEquals(1, object.setMethodCalled);
		assertEquals(1, object.postConstructCalled);
		
		TestData methodValue2 = new TestData();
		context.set(TestData.class.getName(), methodValue2);
		assertEquals(2, object.setMethodCalled);
		assertEquals(1, object.postConstructCalled);
		assertEquals(methodValue2, object.value);
	}

	/**
	 * Tests basic context injection
	 */
	public synchronized void testInjection() {
		Integer testInt = new Integer(123);
		String testString = new String("abc");
		Double testDouble = new Double(1.23);
		Float testFloat = new Float(12.3);
		Character testChar = new Character('v');

		// create context
		IEclipseContext context = EclipseContextFactory.create();
		context.set(Integer.class.getName(), testInt);
		context.set(String.class.getName(), testString);
		context.set(Double.class.getName(), testDouble);
		context.set(Float.class.getName(), testFloat);
		context.set(Character.class.getName(), testChar);

		ObjectBasic userObject = new ObjectBasic();
		ContextInjectionFactory.inject(userObject, context);

		// check field injection
		assertEquals(testString, userObject.injectedString);
		assertEquals(testInt, userObject.getInt());
		assertEquals(context, userObject.context);

		// check method injection
		assertEquals(1, userObject.setMethodCalled);
		assertEquals(1, userObject.setMethodCalled2);
		assertEquals(testDouble, userObject.d);
		assertEquals(testFloat, userObject.f);
		assertEquals(testChar, userObject.c);

		// check post processing
		assertTrue(userObject.finalized);
	}

	/**
	 * Tests that fields are injected before methods.
	 */
	public void testFieldMethodOrder() {
		final AssertionFailedError[] error = new AssertionFailedError[1];
		class TestData {
		}
		class Injected {
			@Inject @Named("valueField")
			Object injectedField;
			Object methodValue;

			@SuppressWarnings("unused")
			@Inject
			public void injectedMethod(@Named("valueMethod") Object arg) {
				try {
					assertTrue(injectedField != null);
				} catch (AssertionFailedError e) {
					error[0] = e;
				}
				methodValue = arg;
			}
		}
		IEclipseContext context = EclipseContextFactory.create();
		TestData fieldValue = new TestData();
		TestData methodValue = new TestData();
		context.set("valueField", fieldValue);
		context.set("valueMethod", methodValue);
		Injected object = new Injected();
		ContextInjectionFactory.inject(object, context);
		if (error[0] != null)
			throw error[0];
		assertEquals(fieldValue, object.injectedField);
		assertEquals(methodValue, object.methodValue);

		// removing method value, the field should still have value
		context.remove("valueMethod");
		if (error[0] != null)
			throw error[0];
		assertEquals(fieldValue, object.injectedField);
		assertNull(object.methodValue);

		((IDisposable) context).dispose();
		if (error[0] != null)
			throw error[0];
	}

	public void testOptionalInjection() {
		Integer testInt = new Integer(123);
		IEclipseContext context = EclipseContextFactory.create();
		context.set(Integer.class.getName(), testInt);

		OptionalAnnotations userObject = new OptionalAnnotations();
		ContextInjectionFactory.inject(userObject, context);
		
		assertEquals(0, userObject.methodOptionalCalled);
		assertEquals(1, userObject.methodRequiredCalled);
		assertEquals(testInt, userObject.i);
		assertNull(userObject.s);
		assertNull(userObject.d);
		assertNull(userObject.f);
		
		// add optional services
		String testString = new String("abc");
		Double testDouble = new Double(1.23);
		Float testFloat = new Float(12.3);
		context.set(String.class.getName(), testString);
		context.set(Double.class.getName(), testDouble);
		context.set(Float.class.getName(), testFloat);
		
		assertEquals(1, userObject.methodOptionalCalled);
		assertEquals(2, userObject.methodRequiredCalled);
		assertEquals(testInt, userObject.i);
		assertEquals(testString, userObject.s);
		assertEquals(testDouble, userObject.d);
		assertEquals(testFloat, userObject.f);
	}

	/**
	 * Tests that a class with multiple inherited post-construct / pre-destroy methods.
	 */
	public void testInheritedSpecialMethods() {
		IEclipseContext context = EclipseContextFactory.create();
		context.set(Integer.class.getSimpleName(), new Integer(123));
		context.set(String.class.getSimpleName(), new String("abc"));
		context.set(String.class.getName(), new String("abc"));
		context.set(Float.class.getName(), new Float(12.3));

		ObjectSubClass userObject = new ObjectSubClass();
		ContextInjectionFactory.inject(userObject, context);
		assertEquals(1, userObject.superPostConstructCount);
		assertEquals(1, userObject.subPostConstructCount);
		assertEquals(0, userObject.superPreDestroyCount);
		assertEquals(0, userObject.subPreDestroyCount);
		assertEquals(0, userObject.overriddenPreDestroyCount);

		context.set(Float.class.getName(), new Float(45.6));
		assertEquals(1, userObject.superPostConstructCount);
		assertEquals(1, userObject.subPostConstructCount);
		assertEquals(0, userObject.superPreDestroyCount);
		assertEquals(0, userObject.subPreDestroyCount);
		assertEquals(0, userObject.overriddenPreDestroyCount);

		((IDisposable) context).dispose();
		assertEquals(1, userObject.superPreDestroyCount);
		assertEquals(1, userObject.subPreDestroyCount);
		assertEquals(1, userObject.overriddenPreDestroyCount);
	}

	public void testInvoke() {
		class TestData {
			public String value;
			
			public TestData(String tmp) {
				value = tmp;
			}
		}
		class Injected {
			public String myString;
			
			public Injected() {
				// placeholder
			}

			@SuppressWarnings("unused")
			public String doSomething(@Named("testing123") TestData data) {
				myString = data.value;
				return "true";
			}
		}
		IEclipseContext context = EclipseContextFactory.create();
		
		TestData methodValue = new TestData("abc");
		context.set("testing123", methodValue);
		Injected object = new Injected();
		assertNull(object.myString);
		
		assertEquals("true", ContextInjectionFactory.invoke(object, "doSomething", context, null));
		assertEquals("abc", object.myString);
	}
	
	public void testPreDestroy() {
		class TestData {
		}
		class Injected {
			int preDestoryCalled = 0;
			public TestData value;
			
			@Inject
			public TestData directFieldInjection;

			@SuppressWarnings("unused")
			@PreDestroy
			public void aboutToClose() {
				preDestoryCalled++;
				assertNotNull(value);
				assertNotNull(directFieldInjection);
			}

			@SuppressWarnings("unused")
			@Inject
			public void setData(TestData arg) {
				value = arg;
			}
		}
		IEclipseContext context = EclipseContextFactory.create();
		TestData methodValue = new TestData();
		context.set(TestData.class.getName(), methodValue);
		
		Injected object = new Injected();
		ContextInjectionFactory.inject(object, context);
		assertNotNull(object.value);
		assertNotNull(object.directFieldInjection);
		
		((IDisposable)context).dispose();
		
		assertEquals(1, object.preDestoryCalled);
		assertNull(object.value);
		assertNull(object.directFieldInjection);
	}
}
