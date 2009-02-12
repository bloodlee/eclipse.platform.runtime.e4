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

package org.eclipse.e4.core.services.internal.context;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.e4.core.services.context.IEclipseContext;

abstract class Computation {
	Map dependencies = new HashMap();

	final void clear(IEclipseContext context, String name) {
		doClear();
		stopListening(context, name);
	}

	private void stopListening(IEclipseContext context, String name) {
		if (EclipseContext.DEBUG) System.out.println(toString() + " no longer listening"); //$NON-NLS-1$
		
		Set properties = (Set) dependencies.get(context);
		if (properties != null) {
			((EclipseContext)context).listeners.remove(this); // XXX IEclipseContext
			properties.remove(name);
		}
	}

	protected void doClear() {
	}

	protected void doHandleInvalid(IEclipseContext context, String name, int eventType) {
	}

	final void handleInvalid(IEclipseContext context, String name, int eventType) {
		Set names = (Set) dependencies.get(context);
		if (names != null && names.contains(name)) {
			clear(context, name);
			doHandleInvalid(context, name, eventType);
		}
	}

	void startListening() {
		if (EclipseContext.DEBUG) System.out.println(toString() + " now listening to: " //$NON-NLS-1$
				+ mapToString(dependencies));
		for (Iterator it = dependencies.keySet().iterator(); it.hasNext();) {
			EclipseContext c = (EclipseContext) it.next();  // XXX IEclipseContex
			c.listeners.add(this);
		}
	}

	private String mapToString(Map map) {
		StringBuffer result = new StringBuffer('{');
		for (Iterator it = map.entrySet().iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			result.append(entry.getKey());
			result.append("->(");
			Set set = (Set) entry.getValue();
			for (Iterator it2 = set.iterator(); it2.hasNext();) {
				String name = (String) it2.next();
				result.append(name);
				if (it2.hasNext()) {
					result.append(",");
				}
			}
			result.append(")");
			if (it.hasNext()) {
				result.append(",");
			}
		}
		return result.toString();
	}

	void addDependency(IEclipseContext context, String name) {
		Set properties = (Set) dependencies.get(context);
		if (properties == null) {
			properties = new HashSet();
			dependencies.put(context, properties);
		}
		properties.add(name);
	}

}