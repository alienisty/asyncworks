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

/**
 * A loop based {@link Progressive} that executes until the execution is cancelled or the loop has
 * no more available iterations.
 * 
 * @param <T>
 *          the type of the progressive value.
 * 
 * @author Alex Nistico &lt;alienisty(at)gmail.com&gt;
 */
public abstract class AsyncLoop<T> implements Progressive<T> {
  @Override
  public final void run(ProgressState<T> state) throws Exception {
    while (hasNext()) {
      state.checkCancelled();
      state.notifyProgress(next());
    }
  }

  /**
   * Whether the loop condition is met or not.
   * 
   * @return {@code true} if the loop condition is still met, {@code false} otherwise.
   */
  protected abstract boolean hasNext();

  /**
   * Invoked until the execution is not cancelled or the loop condition is true
   * 
   * @return the result of the current iteration or {@code null}.
   * 
   * @throws Exception
   *           when an exception happened within the execution and need to be sent to the exception
   *           handlers.
   */
  @CheckForNull
  protected abstract T next() throws Exception;
}
