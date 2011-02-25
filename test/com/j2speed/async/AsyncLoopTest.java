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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

@SuppressWarnings("all")
public class AsyncLoopTest {
  @Test
  public void testLoop() {
    final List<Integer> expected = Arrays.asList(1, 2, 3, 4);
    final List<Integer> received = new LinkedList<Integer>();

    SyncRunner.get().start(new AsyncLoop<Integer>() {
      final Iterator<Integer> iter = expected.iterator();

      @Override
      protected Integer next() throws Exception {
        return iter.next();
      }
      @Override
      protected boolean hasNext() {
        return iter.hasNext();
      }
    }, new ProgressAdapter<Integer>() {
      @Override
      public void onProgress(Progress<Integer> progress) {
        received.add(progress.get());
      }
    });
    assertEquals(expected, received);
  }
}
