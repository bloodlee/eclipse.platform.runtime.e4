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
package org.eclipse.e4.core.services.internal.annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.eclipse.e4.core.internal.di.InjectionProperties;

public class OldAnnotationsSupport {

	public OldAnnotationsSupport() {
		// placeholder
	}
	
	public InjectionProperties getInjectProperties(Field field) {
		return null;
	}

	public InjectionProperties getInjectProperties(Method method) {
		return null;
	}
	
	public InjectionProperties getInjectProperties(Constructor constructor) {
		return null;
	}
	
	public InjectionProperties[] getInjectParamsProperties(Constructor constructor) {
		return null;
	}

	public InjectionProperties[] getInjectParamProperties(Method method) {
		return null;
	}

	private InjectionProperties[] getInjectProperties(Annotation[][] annotations, Type[] params) {
		return null;
	}

	private InjectionProperties getInjectProperties(Annotation[] annotations, Type param) {
		return null;
	}
	
	public boolean isPostConstruct(Method method) {
		return false;
	}
	
	public boolean isPreDestory(Method method) {
		return false;
	}
}
