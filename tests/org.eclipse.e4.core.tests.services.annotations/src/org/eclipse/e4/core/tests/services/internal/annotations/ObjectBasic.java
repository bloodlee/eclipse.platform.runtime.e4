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

import org.eclipse.e4.core.services.annotations.PostConstruct;
import org.eclipse.e4.core.services.annotations.PreDestroy;
import org.eclipse.e4.core.services.context.IEclipseContext;

/**
 * Test class to check injection mechanism
 */
public class ObjectBasic {

	@Inject
	public String injectedString;
	@Inject
	private Integer injectedInteger;

	// Injected indirectly
	public Double d;
	public Float f;
	public Character c;
	protected IEclipseContext context;

	// Test status
	public boolean finalized = false;
	public boolean disposed = false;
	public int setMethodCalled = 0;
	public int setMethodCalled2 = 0;

	public ObjectBasic() {
		// placeholder
	}

	@Inject
	public void objectViaMethod(Double d) {
		setMethodCalled++;
		this.d = d;
	}

	@Inject
	public void arguments(Float f, Character c) {
		setMethodCalled2++;
		this.f = f;
		this.c = c;
	}

	@PostConstruct
	public void postCreate(IEclipseContext context) {
		this.context = context;
		finalized = true;
	}

	@PreDestroy
	public void preFinal(IEclipseContext context) {
		if (this.context != context)
			throw new IllegalArgumentException("Unexpected context");
		this.context = null;
		disposed = true;
	}

	public Integer getInt() {
		return injectedInteger;
	}

}
