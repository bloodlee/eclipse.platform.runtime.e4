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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Qualifier;

import org.eclipse.e4.core.services.annotations.Optional;
import org.eclipse.e4.core.services.annotations.PostConstruct;
import org.eclipse.e4.core.services.annotations.PreDestroy;
import org.eclipse.e4.core.services.internal.context.InjectionProperties;

// TBD make all injections optional unless "mandatory" is specified

public class AnnotationsSupport {

	static public InjectionProperties getInjectProperties(Field field) {
		InjectionProperties property = getInjectProperties(field.getAnnotations());
		processProvider(field.getGenericType(), property);
		return property;
	}

	static public InjectionProperties getInjectProperties(Method method) {
		return getInjectProperties(method.getAnnotations());
	}
	
	static public InjectionProperties getInjectProperties(Constructor constructor) {
		return getInjectProperties(constructor.getAnnotations());
	}
	
	static public InjectionProperties[] getInjectParamsProperties(Constructor constructor) {
		Annotation[][] annotations = constructor.getParameterAnnotations();
		InjectionProperties[] result = new InjectionProperties[annotations.length]; 
		for(int i = 0 ; i < annotations.length; i++)
			result[i] = getInjectProperties(annotations[i]);
		processProviders(constructor.getGenericParameterTypes(), result);
		return result;
	}

	static public InjectionProperties[] getInjectParamProperties(Method method) {
		Annotation[][] annotations = method.getParameterAnnotations();
		InjectionProperties[] result = new InjectionProperties[annotations.length]; 
		for(int i = 0 ; i < annotations.length; i++)
			result[i] = getInjectProperties(annotations[i]);
		processProviders(method.getGenericParameterTypes(), result);
		return result;
	}

	static private InjectionProperties getInjectProperties(Annotation[] annotations) {
		boolean inject = false;
		boolean optional = false;
		String named = null;
		String qualifier = null;
		Class<?> qualifierClass = null;
		for (Annotation annotation : annotations) {
			if (annotation instanceof Inject)
				inject = true;
			else if (annotation instanceof Optional)
				optional = true;
			else if (annotation instanceof Named)
				named = ((Named) annotation).value();
			else if (annotation.annotationType().isAnnotationPresent(Qualifier.class)) {
				Type type = annotation.annotationType();
				if (type instanceof Class<?>) {
					qualifierClass = (Class<?>) type;
					qualifier = qualifierClass.getName();
				}
			}
		}
		String injectName = (named != null) ? named : qualifier;
		InjectionProperties result = new InjectionProperties(inject, injectName, optional);
		if (qualifierClass != null)
			result.setQualifier(qualifierClass);
		return result;
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
	
	static private void processProviders(Type[] params, InjectionProperties[] properties) {
		// Go backwards: match last params to the last properties. You'd expect that 
		// params.length == properties.length, but it is not always the case. See:
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5087240
		int paramsPos = params.length - 1;
		for(int i = properties.length - 1 ; i >= 0 && paramsPos >= 0; paramsPos--, i--) {
			processProvider(params[paramsPos], properties[i]);
		}
	}
	
	static private void processProvider(Type param, InjectionProperties property) {
		if (!(param instanceof ParameterizedType))
			return;
		Type rawType = ((ParameterizedType)param).getRawType();
		if (!(rawType instanceof Class<?>))
			return;
		boolean isProvider = ((Class<?>) rawType).isAssignableFrom(Provider.class);
		if (!isProvider)
			return;
		Type[] actualTypes = ((ParameterizedType)param).getActualTypeArguments();
		if (actualTypes.length != 1)
			return;
		if (!(actualTypes[0] instanceof Class<?>))
			return;
		Class<?> clazz = (Class<?>)actualTypes[0];
		if (property.getQualifier() != null)
			clazz = property.getQualifier();
		property.setProvider(new ContextProvider(property.getPropertyName(), clazz));
	}

}
