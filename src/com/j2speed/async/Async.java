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
 * An asynchronous computation that can be executed on a specified {@link AsyncRunner}. This type of
 * computation is not meant to generate a value to notify to the caller, use {@link Request} or
 * {@link Progressive} for that. An {@link Async} should be used for call and forget type of
 * computations.
 * 
 * @author Alex Nistico &lt;alienisty(at)gmail.com&gt;
 */
public interface Async {
  /**
   * Executes the computation asynchronously.
   * <p>
   * Implementation of this interface should make sure to query the {@link State#checkCancelled()}
   * method during execution to allow a prompt and controlled termination.<br>
   * Note, also, that when an execution is cancelled, the executing thread is interrupted, to allow
   * interruptible methods to be notified if necessary, so there is no need to check the state if
   * you are going to use an interruptible method.
   * </p>
   * 
   * @param state
   *          the current execution state.
   * 
   * @throws Exception
   *           when an exception happened within the execution an need to be sent to the exception
   *           handlers.
   */
  public void run(@NonNull State state) throws Exception;
}
