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

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Runs all e4 core service tests.
 */
public class ServicesTestSuite extends TestSuite {
	public static Test suite() {
		return new ServicesTestSuite();
	}

	public ServicesTestSuite() {
		addTestSuite(EclipseContextTest.class);
		addTestSuite(ServiceContextTest.class);
		addTestSuite(ContextInjectionTest.class);
		addTestSuite(ContextInjectionDisposeTest.class);
		addTestSuite(ContextInjectionFactoryTest.class);
		addTestSuite(ContextDynamicTest.class);
		addTestSuite(JSONObjectTest.class);
		addTestSuite(ReparentingTest.class);
		addTestSuite(RunAndTrackTest.class);
	}
}
