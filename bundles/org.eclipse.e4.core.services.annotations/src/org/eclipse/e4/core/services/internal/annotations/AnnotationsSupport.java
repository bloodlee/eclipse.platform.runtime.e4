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
package org.eclipse.e4.core.services.internal.annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.services.annotations.Optional;
import org.eclipse.e4.core.services.annotations.PostConstruct;
import org.eclipse.e4.core.services.annotations.PreDestroy;
import org.eclipse.e4.core.services.internal.context.InjectionProperties;

// TBD make all injections optional unless "mandatory" is specified

public class AnnotationsSupport {

	static public InjectionProperties getInjectProperties(Field field) {
		return getInjectProperties(field.getAnnotations());
	}

	static public InjectionProperties getInjectProperties(Method method) {
		return getInjectProperties(method.getAnnotations());
	}
	
	static public InjectionProperties getInjectProperties(Class<?> type) {
		return getInjectProperties(type.getAnnotations());
	}
	
	static private InjectionProperties getInjectProperties(Annotation[] annotations) {
		boolean inject = false;
		boolean optional = false;
		String injectName = null;
		for (Annotation annotation : annotations) {
			if (annotation instanceof Inject) {
				inject = true;
			} else if (annotation instanceof Optional) {
				inject = true;
				optional = ((Optional) annotation).value();
			} else if (annotation instanceof Named)
				injectName = ((Named) annotation).value();
		}
		return new InjectionProperties(inject, injectName, optional);
	}

	static public boolean isPostConstruct(Method method) {
		Annotation[] annotations = method.getAnnotations();
		for (Annotation annotation : annotations) {
			if (annotation instanceof PostConstruct)
				return true;
		}
		return false;
	}
	
	static public boolean isPreDestory(Method method) {
		Annotation[] annotations = method.getAnnotations();
		for (Annotation annotation : annotations) {
			if (annotation instanceof PreDestroy)
				return true;
		}
		return false;
	}

}
