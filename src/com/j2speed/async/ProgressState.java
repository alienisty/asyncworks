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
 * Notifies a progress handler of a progress. An instance of this class is passed as a parameter to
 * the execute method of a {@link Progressive}. The instance type is {@link AsyncRunner}
 * implementation dependent.
 * 
 * @param <T>
 *          the type of the value.
 * 
 * @author Alex Nistico &lt;alienisty(at)gmail.com&gt;
 */
public interface ProgressState<T> extends State {
  /**
   * Allows the {@link Progressive} to notify the {@link ProgressHandler} of an available progress
   * value.
   * 
   * @param value
   *          the currently generated progress value.
   */
  public void notifyProgress(@CheckForNull T value);
}
