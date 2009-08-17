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

import junit.framework.TestCase;
import org.eclipse.e4.core.services.context.EclipseContextFactory;
import org.eclipse.e4.core.services.context.IContextFunction;
import org.eclipse.e4.core.services.context.IEclipseContext;
import org.eclipse.e4.core.services.context.spi.ContextFunction;
import org.eclipse.e4.core.services.context.spi.IContextConstants;

public class EclipseContextTest extends TestCase {

	private static class ComputedValueBar extends ContextFunction {
		public Object compute(IEclipseContext context, Object[] arguments) {
			return context.get("bar");
		}
	}

	private static class ConcatFunction implements IContextFunction {
		public Object compute(IEclipseContext context, Object[] arguments) {
			String separator = (String) context.get("separator");
			StringBuffer result = new StringBuffer();
			for (int i = 0; i < arguments.length; i++) {
				if (i > 0) {
					result.append(separator);
				}
				result.append(arguments[i]);
			}
			return result.toString();
		}
	}

	private IEclipseContext context;
	private IEclipseContext parentContext;

	private int runCounter;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.parentContext = EclipseContextFactory.create();
		this.parentContext.set(IContextConstants.DEBUG_STRING, getName() + "-parent");
		this.context = EclipseContextFactory.create(parentContext, null);
		context.set(IContextConstants.DEBUG_STRING, getName());
		runCounter = 0;
	}

	public void testContainsKey() {
		assertFalse("1.0", context.containsKey("function"));
		assertFalse("1.1", context.containsKey("separator"));

		context.set("function", new ConcatFunction());
		context.set("separator", ",");
		assertTrue("2.0", context.containsKey("function"));
		assertTrue("2.1", context.containsKey("separator"));

		// null value is still a value
		context.set("separator", null);
		assertTrue("3.0", context.containsKey("separator"));

		context.remove("separator");
		assertFalse("4.0", context.containsKey("separator"));
	}

	public void testContainsKeyLocal() {
		IEclipseContext contextGlobal = EclipseContextFactory.create();
		contextGlobal.set("global", new Object());
		IEclipseContext contextLocal = EclipseContextFactory.create(contextGlobal, null);
		contextLocal.set("local", new Object());

		assertTrue("1.0", contextLocal.containsKey("local", true));
		assertTrue("1.1", contextLocal.containsKey("local", false));
		assertFalse("1.2", contextLocal.containsKey("global", true));
		assertTrue("1.3", contextLocal.containsKey("global", false));

		assertFalse("2.0", contextGlobal.containsKey("local", true));
		assertFalse("2.1", contextGlobal.containsKey("local", false));
		assertTrue("2.2", contextGlobal.containsKey("global", true));
		assertTrue("2.3", contextGlobal.containsKey("global", false));
	}

	public void testFunctions() {
		context.set("function", new ConcatFunction());
		context.set("separator", ",");
		assertEquals("x", context.get("function", new String[] { "x" }));
		assertEquals("x,y", context.get("function", new String[] { "x", "y" }));
	}

	public void testGet() {
		assertNull(context.get("foo"));
		context.set("foo", "bar");
		assertEquals("bar", context.get("foo"));
		assertNull(parentContext.get("foo"));
		context.remove("foo");
		assertNull(context.get("foo"));
		parentContext.set("foo", "bar");
		assertEquals("bar", context.get("foo"));
		context.set("foo", new ComputedValueBar());
		assertNull(context.get("foo"));
		context.set("bar", "baz");
		assertEquals("baz", context.get("foo"));
	}

	public void testGetLocal() {
		assertNull(context.getLocal("foo"));
		context.set("foo", "bar");
		assertEquals("bar", context.getLocal("foo"));
		assertNull(parentContext.getLocal("foo"));
		context.remove("foo");
		assertNull(context.getLocal("foo"));
		parentContext.set("foo", "bar");
		assertNull(context.getLocal("foo"));
		context.set("foo", new ComputedValueBar());
		assertNull(context.getLocal("foo"));
		context.set("bar", "baz");
		assertEquals("baz", context.getLocal("foo"));
	}

	/**
	 * Tests handling of a context function defined in the parent that uses values defined in the
	 * child
	 */
	public void testContextFunctionInParent() {
		IEclipseContext parent = EclipseContextFactory.create();
		final IEclipseContext child = EclipseContextFactory.create(parent, null);
		parent.set("sum", new AddContextFunction());
		parent.set("x", 3);
		parent.set("y", 3);
		child.set("x", 1);
		child.set("y", 1);
		assertEquals(6, ((Integer) parent.get("sum")).intValue());
		assertEquals(2, ((Integer) child.get("sum")).intValue());
		child.set("x", 5);
		assertEquals(6, ((Integer) parent.get("sum")).intValue());
		assertEquals(6, ((Integer) child.get("sum")).intValue());
		child.remove("x");
		assertEquals(6, ((Integer) parent.get("sum")).intValue());
		assertEquals(4, ((Integer) child.get("sum")).intValue());
		parent.set("x", 10);
		assertEquals(13, ((Integer) parent.get("sum")).intValue());
		assertEquals(11, ((Integer) child.get("sum")).intValue());
	}

	public void testRunAndTrack() {
		final Object[] value = new Object[1];
		context.runAndTrack(new Runnable() {
			public void run() {
				runCounter++;
				value[0] = context.get("foo");
			}
		});
		assertEquals(1, runCounter);
		assertEquals(null, value[0]);
		context.set("foo", "bar");
		assertEquals(2, runCounter);
		assertEquals("bar", value[0]);
		context.remove("foo");
		assertEquals(3, runCounter);
		assertEquals(null, value[0]);
		context.set("foo", new IContextFunction() {
			public Object compute(IEclipseContext context, Object[] arguments) {
				return context.get("bar");
			}
		});
		assertEquals(4, runCounter);
		assertEquals(null, value[0]);
		context.set("bar", "baz");
		assertEquals(5, runCounter);
		assertEquals("baz", value[0]);
		context.set("bar", "baf");
		assertEquals(6, runCounter);
		assertEquals("baf", value[0]);
		context.remove("bar");
		assertEquals(7, runCounter);
		assertEquals(null, value[0]);
		parentContext.set("bar", "bam");
		assertEquals(8, runCounter);
		assertEquals("bam", value[0]);
	}

	public void testRunAndTrackMultipleValues() {
		IEclipseContext parent = EclipseContextFactory.create();
		final IEclipseContext child = EclipseContextFactory.create(parent, null);
		parent.set("parentValue", "x");
		parent.set(IContextConstants.DEBUG_STRING, "ParentContext");
		child.set("childValue", "x");
		child.set(IContextConstants.DEBUG_STRING, "ChildContext");
		Runnable runnable = new Runnable() {
			public void run() {
				runCounter++;
				if (runCounter < 2)
					child.get("childValue");
				if (runCounter < 3)
					child.get("parentValue");
			}
		};
		child.runAndTrack(runnable);
		assertEquals(1, runCounter);
		child.set("childValue", "z");
		assertEquals(2, runCounter);
		parent.set("parentValue", "z");
		assertEquals(3, runCounter);
		// at this point we should no longer be listening
		child.set("childValue", "y");
		assertEquals(3, runCounter);
		parent.set("parentValue", "y");
		assertEquals(3, runCounter);
		// TODO this shouldn't be required
		// ((EclipseContext) child).removeRunAndTrack(runnable);
		assertTrue(TestHelper.getListeners(child).isEmpty());
		assertTrue(TestHelper.getListeners(parent).isEmpty());
	}

	public void testModify() {
		IEclipseContext grandParent = EclipseContextFactory.create();
		IEclipseContext parent = EclipseContextFactory.create(grandParent, null);
		IEclipseContext child = EclipseContextFactory.create(parent, null);

		child.set("a", "a1");
		parent.set("b", "b2");
		grandParent.set("c", "c3");

		child.declareModifiable("a");
		parent.declareModifiable("b");
		grandParent.declareModifiable("c");

		// test pre-conditions
		assertNull(grandParent.get("b"));
		assertEquals("b2", parent.get("b"));
		assertEquals("b2", child.get("b"));
		assertFalse(child.containsKey("b", true /* localOnly */));

		// modify value on the middle node via its child
		child.modify("b", "abc");

		assertFalse(grandParent.containsKey("b"));
		assertEquals("abc", parent.get("b"));
		assertEquals("abc", child.get("b"));
		assertFalse(child.containsKey("b", true /* localOnly */));

		// modifying non-exist values adds it to the context
		child.modify("d", "123");

		assertFalse(grandParent.containsKey("d"));
		assertFalse(parent.containsKey("d"));
		assertNull(parent.get("d"));
		assertEquals("123", child.get("d"));

		// edge conditions: modify value in the top node
		grandParent.modify("c", "cNew");
		assertTrue(grandParent.containsKey("c"));
		assertEquals("cNew", grandParent.get("c"));
		assertFalse(parent.containsKey("c", true /* localOnly */));
		assertFalse(child.containsKey("c", true /* localOnly */));
		assertTrue(child.containsKey("c", false /* localOnly */));

		// edge condition: modify value in the leaf node
		child.modify("a", "aNew");
		assertTrue(child.containsKey("a"));
		assertFalse(parent.containsKey("a"));
		assertFalse(grandParent.containsKey("a"));
		assertEquals("aNew", child.get("a"));
		assertNull(parent.get("a"));

		// test access rules
		child.set("aNo", "a1");
		parent.set("bNo", "b2");
		grandParent.set("cNo", "c3");

		boolean exception = false;
		try {
			child.modify("bNo", "new");
		} catch (IllegalArgumentException e) {
			exception = true;
		}
		assertTrue(exception);

		exception = false;
		try {
			grandParent.modify("cNo", "new");
		} catch (IllegalArgumentException e) {
			exception = true;
		}
		assertTrue(exception);

		exception = false;
		try {
			child.modify("aNo", "new");
		} catch (IllegalArgumentException e) {
			exception = true;
		}
		assertTrue(exception);
	}

	public void testRemoveValueComputationOnDispose() {
		IEclipseContext parent = EclipseContextFactory.create();
		IEclipseContext child = EclipseContextFactory.create(parent, null);
		parent.set("x", 1);
		parent.set("y", 1);
		parent.set("sum", new AddContextFunction());
		parent.set(IContextConstants.DEBUG_STRING, "ParentContext");
		child.set(IContextConstants.DEBUG_STRING, "ChildContext");

		child.get("sum");
		assertEquals(1, TestHelper.getListeners(parent).size());
		((EclipseContext) child).dispose();
		assertEquals(0, TestHelper.getListeners(parent).size());
	}
}
