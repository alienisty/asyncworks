/**
 * Copyright © 2011 J2Speed. All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.j2speed.async;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a method that needs to be invoked on a specific thread (or pool of threads).
 * <p>
 * This annotation accepts a {@link Class} for its value. The class needs to specify a static method
 * with the following signature:
 * 
 * <pre>
 *  public static void transfer(Runnable);
 * </pre>
 * 
 * </p>
 * <p>
 * When one of the supported methods is annotated with this annotation, the processor will get the
 * class and it will try to invoke a static method with the above signature. See {@link Edt} for an
 * example.
 * </p>
 * 
 * @author Alex Nistico &lt;alienisty(at)gmail.com&gt;
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RunOn {
/**
 * The {@link Class} that provide a method with the following signature:
 * 
 * <pre>
 *  public static void transfer(Runnable);
 * </pre>
 * 
 * used to transfer the execution of the annotated method to the specific thread (or pool of
 * threads) managed by the specified class.
 * 
 * @return a {@link Class} containing a static method according to the above contract.
 */
Class<?> value();
}
