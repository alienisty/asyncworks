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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.Thread.State;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.j2speed.accessor.Accessors;
import com.j2speed.async.ExecutionQueue;
import com.j2speed.junit.ThreadTransferRunner;

@SuppressWarnings("all")
@RunWith(ThreadTransferRunner.class)
public class ExecutionQueueTest {

  ExecutionQueue queue;

  Queue<Runnable> buffer;

  @Before
  public void setUp() throws Exception {
    queue = new ExecutionQueue();
    buffer = accessField("bufferQueue", queue);
  }

  @Test
  public void testClear() {
    assertTrue(buffer.isEmpty());
    assertTrue(queue.isEmpty());
    buffer.offer(new Runnable() {
      @Override
      public void run() {}
    });
    assertTrue(queue.isEmpty());
    assertFalse(buffer.isEmpty());
    queue.clear();
    assertTrue(buffer.isEmpty());
    assertNull(queue.poll());
  }

  @Test
  public void testPollLongTimeUnit() throws Exception {
    test(new QueueQuery() {
      public Runnable ask() throws Exception {
        return queue.poll(500, TimeUnit.MILLISECONDS);
      }
    });
  }

  @Test
  public void testPoll() throws Exception {
    test(new QueueQuery() {
      public Runnable ask() throws Exception {
        return queue.poll();
      }
    });
  }

  @Test
  public void testTake() throws Exception {
    test(new QueueQuery() {
      public Runnable ask() throws Exception {
        return queue.take();
      }
    });
  }

  private void test(QueueQuery query) throws Exception {
    Runnable expected = offer();
    assertSame(expected, query.ask());

    expected = offer();
    Runnable expectedFirst = new Runnable() {
      public void run() {}
    };
    buffer.offer(expectedFirst);
    // first return the buffered tasks
    assertSame(expectedFirst, query.ask());
    // then satisfy the ones waiting in the synchronous queue
    assertSame(expected, query.ask());

    assertNull(queue.poll());
  }

  @Test
  public void testRejectedExecution() throws InterruptedException {
    Runnable expectedFirst = new Runnable() {
      public void run() {}
    };

    queue.rejectedExecution(expectedFirst, null);
    Runnable expectedLast = offer();

    // Test expected values
    assertSame(expectedFirst, queue.poll());
    assertSame(expectedLast, queue.poll());
    assertNull(queue.poll());
  }

  private Runnable offer() {
    Thread thread = new Thread() {
      @Override
      public void run() {
        try {
          if (!queue.offer(this, 200, TimeUnit.MILLISECONDS)) {
            fail("Test timed out");
          }
        } catch (InterruptedException e) {
          fail("Test interrupted");
        }
      }
    };
    thread.start();
    while (thread.getState() != State.TIMED_WAITING) {
      // wait until the thread is parked
      continue;
    }
    return thread;
  }

  interface QueueQuery {
    Runnable ask() throws Exception;
  }
}
