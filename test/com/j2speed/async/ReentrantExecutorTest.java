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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

public class ReentrantExecutorTest {
  private static final long TIMEOUT = 100;
  private static final String TEST_EXECUTOR = "test-executor";

  @Test
  public void testExecutorThreadReentrance() throws InterruptedException {
    final ReentrantExecutor executor = new ReentrantExecutor(1, TEST_EXECUTOR);
    try {
      final CountDownLatch done = new CountDownLatch(1);
      executor.execute(new Runnable() {
        @Override
        public void run() {
          final CountDownLatch executed = new CountDownLatch(1);
          final AtomicReference<Thread> executing = new AtomicReference<Thread>();

          executor.execute(new Runnable() {
            @Override
            public void run() {
              executing.set(Thread.currentThread());
              executed.countDown();
            }
          });
          // The use of the latch makes sure that the inner async is fully executed.
          try {
            if (!executed.await(TIMEOUT, TimeUnit.MICROSECONDS)) {
              fail("timeout");
            }
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          // To be reentrant the inner async should have executed in the same thread as the current
          // async
          assertSame(Thread.currentThread(), executing.get());
          done.countDown();
        }
      });
      if (!done.await(TIMEOUT, TimeUnit.MICROSECONDS)) {
        fail("timeout");
      }
    } finally {
      executor.shutdown();
    }
  }

  @Test
  public void testIdleThreadClaiming() throws InterruptedException {
    // Tests that an idle thread is reclaimed after its timeout.
    // This is a non conventional test, it is here just for code coverage.
    final CountDownLatch latch = new CountDownLatch(1);
    final ReentrantExecutor executor = new ReentrantExecutor(0, 1, 20, TimeUnit.MILLISECONDS, TEST_EXECUTOR);
    try {
      executor.execute(new Runnable() {
        @Override
        public void run() {
          // do nothing, except trigger the latch
          latch.countDown();
        }
      });

      latch.await(500, TimeUnit.MILLISECONDS);

      // Busy-wait to give the thread time to die
      long start = System.currentTimeMillis();
      while ((System.currentTimeMillis() - start < 500) && executor.getPoolSize() > 0) {
        Thread.sleep(10);
      }

      assertEquals(0, executor.getPoolSize());
      // Add a couple of async to insure that at least one is on queue to properly test shutdown
      executor.execute(new Runnable() {
        @Override
        public void run() {
          try {
            Thread.sleep(40);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
      });
      executor.execute(new Runnable() {
        @Override
        public void run() {
          try {
            Thread.sleep(40);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
      });
    } finally {
      // Shut down the internal executor to allow full coverage
      executor.shutdown();
    }
  }
}
