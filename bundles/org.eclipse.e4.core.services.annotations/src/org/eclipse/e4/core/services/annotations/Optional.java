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
package org.eclipse.e4.core.services.annotations;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import javax.inject.Qualifier;

/**
 * A boolean qualifier used to specify if injection is mandatory
 * or optional. Typically, if the injector is unable to find a 
 * value to inject, a <code>null</code> value will be injected.
 * However, if this qualifier is set to <code>false</code>, then 
 * injection will fail.
 *
 * <p>Example usage:
 *
 * <pre>
 *   public class Car {
 *     &#064;Inject &#064;Optional(false) Integer identificationNumber;
 *     ...
 *   }
 *  </pre>
 *  </p>
 */
@Qualifier
@Documented
@Retention(RUNTIME)
public @interface Optional {
	boolean value() default true;
}
