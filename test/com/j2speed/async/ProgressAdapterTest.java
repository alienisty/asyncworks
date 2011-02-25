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

import org.junit.Test;

import com.j2speed.async.ProgressAdapter;

@SuppressWarnings("all")
@edu.umd.cs.findbugs.annotations.SuppressWarnings
public class ProgressAdapterTest {

  @Test
  public void test() {
    ProgressAdapter<Object> adapter = new ProgressAdapter<Object>() {};

    // allow coverage
    adapter.onProgress(null);
    adapter.onException(new Throwable());
    adapter.onComplete();
  }
}
