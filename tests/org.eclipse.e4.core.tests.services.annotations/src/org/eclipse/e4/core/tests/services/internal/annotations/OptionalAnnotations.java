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

import javax.inject.Inject;
import org.eclipse.e4.core.services.annotations.Optional;

public class OptionalAnnotations {

	@Inject @Optional(true)
	public Float f = null;
	
	public Double d;
	public String s = new String("ouch");
	public Integer i;
	
	public int methodOptionalCalled = 0;
	public int methodRequiredCalled = 0;
	
	public OptionalAnnotations() {
		// placehodler
	}

	@Inject @Optional(true)
	public void methodOptional(Double d) {
		this.d = d;
		methodOptionalCalled++;
	}

	@Inject
	public void methodRequired(@Optional(true) String s, Integer i) {
		this.s = s;
		this.i = i;
		methodRequiredCalled++;
	}

}
