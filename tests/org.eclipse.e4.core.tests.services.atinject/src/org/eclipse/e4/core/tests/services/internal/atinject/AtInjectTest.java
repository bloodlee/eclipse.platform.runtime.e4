/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.e4.core.tests.services.internal.atinject;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.atinject.tck.Tck;
import org.atinject.tck.auto.Car;
import org.atinject.tck.auto.Convertible;
import org.atinject.tck.auto.Drivers;
import org.atinject.tck.auto.DriversSeat;
import org.atinject.tck.auto.Engine;
import org.atinject.tck.auto.FuelTank;
import org.atinject.tck.auto.Seat;
import org.atinject.tck.auto.Tire;
import org.atinject.tck.auto.V8Engine;
import org.atinject.tck.auto.accessories.Cupholder;
import org.atinject.tck.auto.accessories.SpareTire;
import org.eclipse.e4.core.services.injector.Injector;

public class AtInjectTest extends TestSuite {

	public static Test suite() {
		
		ObjectProviderBinding objectProvider = new ObjectProviderBinding();
		Injector injector = new Injector(objectProvider);
		objectProvider.setInjector(injector);
		
		// TCK description:
		objectProvider.addBinding(Car.class).inject(Convertible.class);
		objectProvider.addBinding(Seat.class).named(Drivers.class.getName()).inject(DriversSeat.class);
		objectProvider.addBinding(Engine.class).inject(V8Engine.class);
		objectProvider.addBinding(Tire.class).named("spare").inject(SpareTire.class);
		
		objectProvider.addBinding(Cupholder.class);
		objectProvider.addBinding(Tire.class);
		objectProvider.addBinding(FuelTank.class);
		
		// missing: - TBD - should those bindings be added automatically?
		objectProvider.addBinding(SpareTire.class);
		objectProvider.addBinding(Seat.class);
		objectProvider.addBinding(DriversSeat.class);

		// inject statics
		injector.injectStatic(Convertible.class);
		injector.injectStatic(Tire.class);
		injector.injectStatic(SpareTire.class);
		
		Car car = (Car) objectProvider.get(Car.class);
		return Tck.testsFor(car, true, true);
	}
}
