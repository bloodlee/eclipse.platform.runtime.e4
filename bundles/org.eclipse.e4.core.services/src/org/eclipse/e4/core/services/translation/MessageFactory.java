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

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

public class MessageFactory {
	public static <M> M createInstance(final String locale, final Class<M> messages)
			throws InstantiationException, IllegalAccessException {
		if (System.getSecurityManager() == null) {
			return doCreateInstance(locale, messages);
		} else {
			return AccessController.doPrivileged(new PrivilegedAction<M>() {

				public M run() {
					try {
						return doCreateInstance(locale, messages);
					} catch (InstantiationException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
					return null;
				}

			});
		}
	}

	private static <M> M doCreateInstance(String locale, Class<M> messages)
			throws InstantiationException, IllegalAccessException {
		Message annotation = messages.getAnnotation(Message.class);

		if (annotation != null) {
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

				fields[i].set(instance, provider.translate(locale, basename));
			}

			return instance;
		}
	}
}