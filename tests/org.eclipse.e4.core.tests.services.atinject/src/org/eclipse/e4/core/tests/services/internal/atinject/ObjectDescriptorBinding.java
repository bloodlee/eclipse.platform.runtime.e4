/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.core.tests.services.internal.atinject;

import org.eclipse.e4.core.services.injector.IObjectDescriptor;

public class ObjectDescriptorBinding implements IObjectDescriptor {
	final private String description;
	final private Class clazz;
	
	public ObjectDescriptorBinding(String description, Class clazz) {
		this.description = description;
		this.clazz = clazz;
	}

	public Class getElementClass() {
		return clazz;
	}

	public String getPropertyName() {
		return description;
	}

	public String getKey() {
		Class<?> elementClass = getElementClass();
		String result = (elementClass == null) ? "" : elementClass.getName();
		if (getPropertyName() != null)
			result += "," + getPropertyName();
		return result;
	}

}
