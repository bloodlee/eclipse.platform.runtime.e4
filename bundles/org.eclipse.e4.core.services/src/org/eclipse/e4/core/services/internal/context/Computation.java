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
import java.util.Map.Entry;

import org.eclipse.e4.core.services.context.IEclipseContext;

abstract class Computation {
	Map dependencies = new HashMap();

	final void clear() {
		doClear();
		stopListening();
	}

	private void stopListening() {
		if (EclipseContext.DEBUG) System.out.println(toString() + " no longer listening");
		for (Iterator it = dependencies.entrySet().iterator(); it.hasNext();) {
			Map.Entry entry = (Entry) it.next();
			EclipseContext context = (EclipseContext) entry.getKey(); // XXX IEclipseContex
			context.listeners.remove(this);
		}
		dependencies.clear();
	}

	protected void doClear() {
	}

	protected void doHandleInvalid(IEclipseContext context) {
	}

	final void handleInvalid(IEclipseContext context, String name) {
		Set names = (Set) dependencies.get(context);
		if (names != null && names.contains(name)) {
			clear();
			doHandleInvalid(context);
		}
	}

	void startListening() {
		if (EclipseContext.DEBUG) System.out.println(toString() + " now listening to: "
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