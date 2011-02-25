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

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Handles the exception caught during the execution of an asynchronous computation.
 * 
 * @author Alex Nistico &lt;alienisty(at)gmail.com&gt;
 */
public interface ExceptionHandler {

  /**
   * Handles the exception caught during the execution of an asynchronous computation.
   * <p>
   * TODO: add documentation for dynamic method extension for specific exception type handling.
   * </p>
   * <p>
   * This method supports the @{@link RunOn} annotation that causes the runner to call-back this
   * method on the EDT.
   * </p>
   * 
   * @param exception
   *          the exception to handle.
   */
  public void onException(@NonNull Throwable exception);

  /**
   * This method is always invoked at the end of a computation or an exception notification.
   * <p>
   * This method supports the @{@link RunOn} annotation that causes the runner to call-back this
   * method on the EDT.
   * </p>
   * <p>
   * This is analogous to the {@code finally} keyword.
   * </p>
   */
  public void onFinally();
}
