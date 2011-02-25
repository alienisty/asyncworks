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
 * An {@link ExceptionHandler} that also handles the notification of the completion of a
 * computation.
 * 
 * @author Alex Nistico &lt;alienisty(at)gmail.com&gt;
 */
public interface CompletionHandler extends ExceptionHandler {
  /**
   * Called back when the an asynchronous computation completes normally, without exception.
   * <p>
   * This method supports the @{@link RunOn} annotation that causes the runner to call-back this
   * method on the EDT.
   * </p>
   * <p>
   * Note that this method is not invoked if the execution throws an exception or is cancelled, use
   * {@link #onFinally()} to perform actions at the end of an asynchronous operation whatever the
   * termination of the execution is, normal or exceptional.
   * </p>
   */
  public void onComplete();
}
