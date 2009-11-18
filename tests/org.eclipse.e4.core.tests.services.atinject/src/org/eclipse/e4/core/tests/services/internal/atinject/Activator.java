package org.eclipse.e4.core.tests.services.internal.atinject;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator implements BundleActivator {
	
	static private BundleContext bundleContext;

	public void start(BundleContext context) throws Exception {
		bundleContext = context;
		
	}

	public void stop(BundleContext context) throws Exception {
		bundleContext = null;
	}
	
	static public BundleContext getContext() {
		return bundleContext;
	}

}
