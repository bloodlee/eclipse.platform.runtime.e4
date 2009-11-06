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

package org.eclipse.e4.core.tests.services.internal.annotations;

import junit.framework.Test;
import junit.framework.TestSuite;

public class ServicesTestSuiteAnnotations extends TestSuite {
	public static Test suite() {
		return new ServicesTestSuiteAnnotations();
	}

	public ServicesTestSuiteAnnotations() {
		addTestSuite(AnnotationsInjectionTest.class);
		addTestSuite(TestConstructorInjection.class);
		addTestSuite(ServiceContextTest.class);
		addTestSuite(ProviderInjectionTest.class);
	}
}
