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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A request that produce a value of the specified type.
 * 
 * @param <T>
 *          the type of the value.
 * 
 * @author Alex Nistico &lt;alienisty(at)gmail.com&gt;
 */
public interface Request<T> {
  /**
   * Produces the a value of type T.
   * <p>
   * Implementation of this interface should make sure to query the {@link State#checkCancelled()}
   * method during execution to allow a prompt and controlled execution termination.<br>
   * Note, also, that when an execution is cancelled, the executing thread is interrupted, so, if
   * the implementation uses interruptible methods, then, if clean up is necessary in those cases,
   * the exception needs to be handled and not let it just propagate.
   * </p>
   * 
   * @param state
   *          the current execution control.
   * 
   * @return the result of the request.
   * 
   * @throws Exception
   *           when an exception happened within the execution an need to be sent to the exception
   *           handlers.
   */
  @CheckForNull
  public T run(@NonNull State state) throws Exception;
}
