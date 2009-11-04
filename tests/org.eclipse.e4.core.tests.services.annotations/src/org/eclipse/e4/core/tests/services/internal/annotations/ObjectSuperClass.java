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

import org.eclipse.e4.core.services.annotations.PostConstruct;
import org.eclipse.e4.core.services.annotations.PreDestroy;
import org.eclipse.e4.core.services.context.IEclipseContext;

/**
 * Test class to check injection mechanism into classes with inheritance
 */
public class ObjectSuperClass {

	protected IEclipseContext context;
	private String inject_String;
	private String myString;

	public int postConstructSetStringCalled;
	public int setFinalizedCalled = 0;
	public int setStringCalled = 0;
	public int superPostConstructCount;
	public int superPreDestroyCount;
	public int setOverriddenCalled = 0;

	public ObjectSuperClass() {
		// placeholder
	}

	public void contextSet(IEclipseContext context) {
		this.context = context;
		setFinalizedCalled++;
	}

	public IEclipseContext getContext() {
		return context;
	}

	public int getFinalizedCalled() {
		return setFinalizedCalled;
	}

	public String getString() {
		return inject_String;
	}

	public String getStringViaMethod() {
		return myString;
	}

	public void injectOverriddenMethod(Float f) {
		setOverriddenCalled++;
	}

	public void injectStringViaMethod(String string) {
		myString = string;
		setStringCalled++;
	}

	@PostConstruct
	public void superPostConstruct() {
		// record setter invocation counts at time of post construct invocation
		postConstructSetStringCalled = setStringCalled;
		superPostConstructCount++;
	}

	@PreDestroy
	public void superPreDestroy() {
		superPreDestroyCount++;
	}

	@PreDestroy()
	public void overriddenPreDestroy() {
		//
	}

}