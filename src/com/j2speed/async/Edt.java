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

import static java.awt.EventQueue.invokeLater;

/**
 * Transfer class for the EDT. This class satisfies the contract established by the {@link RunOn}
 * annotation to transfer the execution to a specified thread, specifically the AWT Event Dispatch
 * Thread.
 * 
 * @author Alex Nistico &lt;alienisty(at)gmail.com&gt;
 */
public abstract class Edt {
  private Edt() {}

  /**
   * This method satisfies the contract specified by the {@link RunOn} annotation.
   * 
   * @param runnable
   *          the code to transfer to the EDT to run.
   */
  public static void transfer(Runnable runnable) {
    invokeLater(runnable);
  }
}
