/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.e4.core.services;

/**
 * A generic interface to be used on objects that require explicit disposal.
 */
public interface IDisposable {
	/**
	 * Disposes of this object. If this object is already disposed this method
	 * will have no effect.
	 */
	public void dispose();
}
