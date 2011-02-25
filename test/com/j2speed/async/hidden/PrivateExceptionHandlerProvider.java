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
package com.j2speed.async.hidden;

import java.util.concurrent.atomic.AtomicReference;

import com.j2speed.async.ExceptionHandler;

@SuppressWarnings("all")
@edu.umd.cs.findbugs.annotations.SuppressWarnings
public class PrivateExceptionHandlerProvider {

  public static final String message = "Handled";

  public static ExceptionHandler makeSpecific(final AtomicReference<String> target) {
    return new ExceptionHandler() {

      @Override
      public void onFinally() {}

      public void onException(Exception exception) {
        target.set(message);
      }

      @Override
      public void onException(Throwable exception) {}
    };
  }

}
