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
import org.eclipse.e4.core.services.injector.IObjectProvider;
import org.eclipse.e4.core.services.internal.context.InjectionProperties;

public class AnnotationsSupport {

	private IObjectProvider context;

	public AnnotationsSupport(IObjectProvider context) {
		this.context = context;
	}
	
	public InjectionProperties getInjectProperties(Field field) {
		InjectionProperties property = getInjectProperties(field.getAnnotations(), field.getGenericType());
		return property;
	}

	public InjectionProperties getInjectProperties(Method method) {
		return getInjectProperties(method.getAnnotations(), null);
	}
	
	public InjectionProperties getInjectProperties(Constructor constructor) {
		return getInjectProperties(constructor.getAnnotations(), null);
	}
	
	public InjectionProperties[] getInjectParamsProperties(Constructor constructor) {
		Annotation[][] annotations = constructor.getParameterAnnotations();
		Type[] logicalParams = constructor.getGenericParameterTypes();
		// JDK bug: different methods see / don't see generated args for nested classes
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5087240
		Class<?>[] compilerParams = constructor.getParameterTypes();
		if (compilerParams.length > logicalParams.length) { 
			Type[] tmp = new Type[compilerParams.length];
			System.arraycopy(compilerParams, 0, tmp, 0, compilerParams.length - logicalParams.length);
			System.arraycopy(logicalParams, 0, tmp, compilerParams.length - logicalParams.length, logicalParams.length);
			logicalParams = tmp;
		}
		return  getInjectProperties(annotations, logicalParams);
	}

	public InjectionProperties[] getInjectParamProperties(Method method) {
		Annotation[][] annotations = method.getParameterAnnotations();
		Type[] params = method.getGenericParameterTypes();
		return  getInjectProperties(annotations, params);
	}

	private InjectionProperties[] getInjectProperties(Annotation[][] annotations, Type[] params) {
		InjectionProperties[] result = new InjectionProperties[params.length]; 
		for(int i = 0 ; i <  params.length; i++)
			result[i] = getInjectProperties(annotations[i], params[i]);
		return result;
	}

	private InjectionProperties getInjectProperties(Annotation[] annotations, Type param) {
		// Process annotations
		boolean inject = false;
		boolean optional = false;
		String named = null;
		String qualifier = null;
		Class<?> qualifierClass = null;
		if (annotations != null) {
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
		}
		String injectName = (named != null) ? named : qualifier;
		Class<?> elementClass =  getElementClass(param); 
		InjectionProperties result = new InjectionProperties(inject, injectName, optional, elementClass);
		if (qualifierClass != null)
			result.setQualifier(qualifierClass);
		
		// Process providers
		if (!(param instanceof ParameterizedType))
			return result;
		Type rawType = ((ParameterizedType)param).getRawType();
		if (!(rawType instanceof Class<?>))
			return result;
		boolean isProvider = ((Class<?>) rawType).isAssignableFrom(Provider.class);
		if (!isProvider)
			return result;
		Type[] actualTypes = ((ParameterizedType)param).getActualTypeArguments();
		if (actualTypes.length != 1)
			return result;
		if (!(actualTypes[0] instanceof Class<?>))
			return result;
		Class<?> clazz = (Class<?>)actualTypes[0];
		result.setProvider(new ContextProvider(result.getPropertyName(), clazz, context));
		
		return result;
	}
	
	private Class<?> getElementClass(Type type) {
		if (type == null)
			return null;
		if (type instanceof Class<?>)
			return (Class<?>) type;
		if (type instanceof ParameterizedType) {
			Type rawType = ((ParameterizedType)type).getRawType();
			if (rawType instanceof Class<?>)
				return (Class<?>) rawType;
		}
		System.err.println("Unexpected type"); // TBD improve
		return null;
	}

	public boolean isPostConstruct(Method method) {
		return method.isAnnotationPresent(PostConstruct.class);
	}
	
	public boolean isPreDestory(Method method) {
		return method.isAnnotationPresent(PreDestroy.class);
	}
}
