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

/**
 * Represents the current execution life cycle of an asynchronous computation.
 * 
 * @author Alex Nistico &lt;alienisty(at)gmail.com&gt;
 */
public interface AsyncRun {
  /**
   * Whether the computation ever started or not.
   * 
   * @return {@code true} if started, {@code false} otherwise.
   */
  public boolean isStarted();

  /**
   * Cancels the async.
   */
  public void cancel();

  /**
   * Whether the asynchronous computation has been cancelled or not.
   * 
   * @return {@code true} if cancelled, {@code false} otherwise.
   */
  public boolean isCancelled();

  /**
   * Returns true if the asynchronous computation is completed. Completion may be due to normal
   * termination, an exception, or cancellation; in all of these cases, this method will return
   * true.
   * 
   * @return true if this task completed
   */
  public boolean isDone();
}
