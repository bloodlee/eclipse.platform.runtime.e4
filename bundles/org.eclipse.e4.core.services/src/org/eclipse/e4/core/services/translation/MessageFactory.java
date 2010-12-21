/*******************************************************************************
 *  Copyright (c) 2010 BestSolution.at and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      Tom Schind<tom.schindl@bestsolution.at> - initial API and implementation
 ******************************************************************************/
package org.eclipse.e4.core.services.translation;

import com.google.common.collect.MapMaker;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ConcurrentMap;
import org.eclipse.e4.core.services.translation.Message.ReferenceType;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

public class MessageFactory {

	// Cache so when multiple views with same information are shown we only hold
	// one instance
	private static ConcurrentMap<Object, Object> SOFT_CACHE = new MapMaker().softValues().makeMap();
	private static ConcurrentMap<Object, Object> WEAK_CACHE = new MapMaker().weakValues().makeMap();

	@SuppressWarnings("unchecked")
	public static <M> M createInstance(final String locale, final Class<M> messages)
			throws InstantiationException, IllegalAccessException {
		String key = messages.getName() + "_" + locale;

		final Message annotation = messages.getAnnotation(Message.class);
		ConcurrentMap<Object, Object> cache = null;

		if (annotation == null || annotation.referenceType() == ReferenceType.SOFT) {
			cache = SOFT_CACHE;
		} else if (annotation.referenceType() == ReferenceType.WEAK) {
			cache = WEAK_CACHE;
		}

		if (cache != null && cache.containsKey(key)) {
			return (M) SOFT_CACHE.get(key);
		}

		M instance;

		if (System.getSecurityManager() == null) {
			instance = doCreateInstance(locale, messages, annotation);
		} else {
			instance = AccessController.doPrivileged(new PrivilegedAction<M>() {

				public M run() {
					try {
						return doCreateInstance(locale, messages, annotation);
					} catch (InstantiationException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
					return null;
				}

			});
		}

		if (cache != null) {
			cache.put(key, instance);
		}

		return instance;
	}

	private static <M> M doCreateInstance(String locale, Class<M> messages, Message annotation)
			throws InstantiationException, IllegalAccessException {

		if (annotation != null && !annotation.providerId().equals("")) {
			Bundle b = FrameworkUtil.getBundle(MessageFactory.class);
			BundleContext ctx = b.getBundleContext();
			ServiceReference<ITranslationService> reference = ctx
					.getServiceReference(ITranslationService.class);
			ITranslationService service = ctx.getService(reference);

			M instance = messages.newInstance();
			Field[] fields = messages.getDeclaredFields();
			String[] keys = new String[fields.length];

			for (int i = 0; i < fields.length; i++) {
				keys[i] = fields[i].getName();
			}

			String[] translations = service.translate(locale, annotation.providerId(), keys);

			for (int i = 0; i < fields.length; i++) {
				if (!fields[i].isAccessible()) {
					fields[i].setAccessible(true);
				}

				fields[i].set(instance, translations[i]);
			}

			return instance;
		} else {
			String basename = messages.getName().replace('.', '/');
			PropertiesBundleTranslationProvider provider = new PropertiesBundleTranslationProvider(
					messages.getClassLoader(), basename);

			M instance = messages.newInstance();
			Field[] fields = messages.getDeclaredFields();

			for (int i = 0; i < fields.length; i++) {
				if (!fields[i].isAccessible()) {
					fields[i].setAccessible(true);
				}

				fields[i].set(instance, provider.translate(locale, fields[i].getName()));
			}

			return instance;
		}
	}
}