/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.core.services.internal.context;

import java.util.List;
import org.eclipse.e4.core.services.context.IEclipseContext;

/**
 * This class exists only for the purpose of automated testing. Clients should never reference this
 * class.
 * 
 * @noreference
 */
public final class TestHelper {
	private TestHelper() {
		// don't allow instantiation
	}

	public static List<Computation> getListeners(IEclipseContext context) {
		return ((EclipseContext) context).listeners;
	}
}
