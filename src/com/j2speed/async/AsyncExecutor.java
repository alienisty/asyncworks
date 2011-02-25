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

import java.util.List;
import java.util.concurrent.Executor;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * The {@link Executor} specialisation for use in and {@link AsyncRunner}.
 * <p>
 * A contract that must be satisfied by implementations, is that they have to be reentrant
 * relatively to the threads they own. To be more specific, if a thread used by an implementation to
 * run tasks tries to reuse the executor to execute another task, the executor should use the
 * current thread instead of going through the normal scheduling path.
 * </p>
 * <p>
 * This contract is required to avoid thread exhaustion deadlocks that could arise when using
 * certain recursive execution patterns are used.
 * </p>
 * 
 * @author Alex Nistico &lt;alienisty(at)gmail.com&gt;
 */
public interface AsyncExecutor extends Executor {

  /**
   * Removes the specified task from the execution queue of this executor.
   * <p>
   * This method returns whether the task was successfully removed or not. If the method returns
   * {@code true}, it means that the task was in the queue and it was not yet polled for running,
   * therefore, if this method returns {@code true}, the specified task will never start, unless it
   * is submitted again to the executor.
   * </p>
   * 
   * @param task
   *          the {@link Runnable} to cancel.
   * 
   * @return {@code true} if the task was still in the queue, {@code false} if the task was no
   *         longer in the queue.
   */
  public boolean cancel(Runnable task);

  /**
   * Cancels all pending tasks.
   * 
   * @return a list of all tasks that were in the queue when called.
   */
  @NonNull
  public List<Runnable> cancelPendings();

  /**
   * Attempts to stop all actively executing asyncs, halts the processing of waiting asyncs.
   * 
   * <p>
   * There are no guarantees beyond best-effort attempts to stop processing actively executing
   * tasks. Typical implementations will cancel via {@link Thread#interrupt}, so any task that fails
   * to respond to interrupts may never terminate.
   * </p>
   * 
   * @throws SecurityException
   *           if a security manager exists and shutting down this AsyncExecutor may manipulate
   *           threads that the caller is not permitted to modify because it does not hold
   *           {@link java.lang.RuntimePermission} <tt>("modifyThread")</tt>, or the security
   *           manager's <tt>checkAccess</tt> method denies access.
   */
  public void shutdown();
}
