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
 * Allows the execution of an asynchronous computation to query the running status.
 * 
 * @author Alex Nistico &lt;alienisty(at)gmail.com&gt;
 */
public interface State {
  /**
   * Whether the current execution has been cancelled or not.
   * <p>
   * Asynchronous computations should be implemented so that this method is invoked during execution
   * to allow a prompt and controlled termination if the execution is cancelled.
   * </p>
   * 
   * @throws CancelledException
   *           if the related asynchronous computation has been cancelled.
   */
  public void checkCancelled();
}
