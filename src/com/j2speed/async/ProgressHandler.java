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
 * It handles the values received during the execution of an asynchronous {@link Progressive}.
 * 
 * @param <T>
 *          the type of the value.
 * 
 * @author Alex Nistico &lt;alienisty(at)gmail.com&gt;
 */
public interface ProgressHandler<T> extends CompletionHandler {
  /**
   * Handles the asynchronously passed in value representing the progress made by the
   * {@link Progressive}.
   * <p>
   * This method supports the @{@link RunOn} annotation that causes the runner to call-back this
   * method on the EDT.
   * </p>
   * 
   * @param progress
   *          the current progress provided by the corresponding {@link Progressive}.
   */
  public void onProgress(@NonNull Progress<T> progress);
}
