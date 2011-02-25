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

import static com.j2speed.accessor.Accessors.accessField;
import static com.j2speed.accessor.Accessors.accessStaticField;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SyncRunnerTest extends AsyncRunnerTest<SyncRunner> {

  @Before
  public void setUp() {
    runner = SyncRunner.get();
  }

  @After
  public void tearDown() {}

  @Test
  public void testAsyncRunOnSameThread() throws InterruptedException {
    final AtomicReference<Thread> executing = new AtomicReference<Thread>();
    // Test that execute is invoked in the same thread
    runner.start(new Async() {
      @Override
      public void run(State state) {
        executing.set(Thread.currentThread());
      }
    });
    assertTrue(Thread.currentThread().equals(executing.get()));
  }

  @Test
  public void testAynchReentrant() {
    // Test thread reentrance
    final AtomicReference<Thread> executing = new AtomicReference<Thread>();
    final AtomicReference<Thread> executing2 = new AtomicReference<Thread>();
    runner.start(new Async() {
      @Override
      public void run(State state) throws InvocationTargetException {
        executing.set(Thread.currentThread());
        runner.start(new Async() {
          @Override
          public void run(State state) throws InvocationTargetException {
            executing2.set(Thread.currentThread());
          }
        });
      }
    });
    assertTrue(Thread.currentThread().equals(executing.get()));
    assertTrue(Thread.currentThread().equals(executing2.get()));
  }

  @Test
  public void testCancelPendings() throws InterruptedException {
    // this is just for coverage, SyncRunner will never have pending asyncs.
    runner.cancelPendings();
  }

  @Test
  public void testShutdwon() throws InterruptedException {
    // this is just for coverage, SynchInvoker will never be shutdown.
    AsyncExecutor executor = accessField("executor", accessStaticField("DELEGATE", SyncRunner.class).get());
    executor.shutdown();
  }

  @Test
  public void testCancelOnExecutor() throws InterruptedException {
    // this is just for coverage, SynchInvoker will never be shutdown.
    AsyncExecutor executor = accessField("executor", accessStaticField("DELEGATE", SyncRunner.class).get());
    assertFalse(executor.cancel(null));
  }

  // Override test not applicable for SyncRunner
  public void testInvokationIsOnADifferentThread() {}

  public void testAsyncFinallyInvokedOnlyOnceOnAllHandlers() {}

  public void testRequestFinallyInvokedOnlyOnceOnAllHandlers() {}

  public void testProgressiveFinallyInvokedOnlyOnceOnAllHandlers() {}

  public void testCancelOnInterruptibleMethods() {}

  public void testCancelCallRaceCondition() {}

  public void testCancelPendingsFinally() {}

  public void testCancelProgressiveFromProgress() {}

  public void testCancelWhenDone() {}

  public void testCancelIgnored() {}
}
