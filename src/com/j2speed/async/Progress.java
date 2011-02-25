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
 * Represent the progress result notified to a {@link ProgressHandler} during the processing of a
 * {@link Progressive}.
 * 
 * @author alienisy
 * 
 * @param <T>
 *          the type of value used to represent a progress.
 */
public interface Progress<T> extends Result<T> {
  /**
   * Cancels the execution of the providing {@link Progressive}.
   */
  public void cancel();
}
