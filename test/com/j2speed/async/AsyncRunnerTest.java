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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.j2speed.junit.ThreadTransferRunner;

@SuppressWarnings("all")
@edu.umd.cs.findbugs.annotations.SuppressWarnings
@RunWith(ThreadTransferRunner.class)
@Ignore
public abstract class AsyncRunnerTest<R extends AsyncRunner> {
  private static final long TIMEOUT = 100;

  R runner;

  @Before
  public abstract void setUp();

  @After
  public abstract void tearDown();

  @Test(expected = NullPointerException.class)
  public void testDefaultRunner() {
    new DefaultRunner(null);
  }

  @Test
  public void testInvokationIsOnADifferentThread() throws Exception {
    final Exchanger<Thread> synch = new Exchanger<Thread>();
    runner.start(new Async() {
      @Override
      public void run(State state) throws Exception {
        synch.exchange(Thread.currentThread());
      }
    });
    assertFalse(Thread.currentThread().equals(synch.exchange(null, TIMEOUT, MILLISECONDS)));
  }

  @Test
  public void testAsyncCallsCompleteOnAllHandlers() throws InterruptedException {
    final CountDownLatch done = new CountDownLatch(3);
    final AtomicBoolean called0 = new AtomicBoolean();
    final AtomicBoolean called1 = new AtomicBoolean();
    final AtomicBoolean called2 = new AtomicBoolean();
    runner.start(new AsyncCompletion() {
      @Override
      public void run(State state) {}

      @Override
      public void onComplete() {
        called0.set(true);
        done.countDown();
      }
    }, new CompletionAdapter() {
      @Override
      public void onComplete() {
        called1.set(true);
        done.countDown();
      }
    }, new CompletionAdapter() {
      @Override
      public void onComplete() {
        called2.set(true);
        done.countDown();
      }
    });
    if (!done.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
      fail("timeout");
    }
    assertTrue(called0.get());
    assertTrue(called1.get());
    assertTrue(called2.get());
  }

  @Test
  public void testAsyncCallsCompleteOnAllHandlers2() throws InterruptedException {
    final CountDownLatch done = new CountDownLatch(1);
    runner.start(new AsyncCompletion() {
      @Override
      public void run(State state) {}
      @Override
      public void onComplete() {
        done.countDown();
      }
    });
    if (!done.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
      fail("timeout");
    }
  }

  @Test
  public void testAsyncDosentNeedHandlers() throws InterruptedException {
    final CountDownLatch done = new CountDownLatch(1);
    runner.start(new Async() {
      @Override
      public void run(State state) {
        done.countDown();
      }
    });
    if (!done.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
      fail("timeout");
    }
  }

  @Test
  public void testAsyncDoesntCallCompleteOnException() throws InterruptedException {
    final CountDownLatch done = new CountDownLatch(1);

    runner.start(new AsyncCompletion() {
      @Override
      public void run(State state) throws Exception {
        throw new Exception("test");
      }
      @Override
      public void onException(Throwable exception) {
        done.countDown();
      }
      @Override
      public void onComplete() {
        fail("complete should not be invoked on exception");
      }
    });
    if (!done.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
      fail("timeout");
    }
  }

  @Test(expected = NullPointerException.class)
  public void testAsyncAndNullHandlers() {
    runner.start(new Async() {
      @Override
      public void run(State state) throws Exception {}
    }, (CompletionHandler[]) null);
  }

  public void testAsyncRecevingExceptionOnAllHandlers() throws InterruptedException {
    // Test that onException receives the correct exception
    final CountDownLatch done = new CountDownLatch(3);
    final AtomicBoolean called0 = new AtomicBoolean();
    final AtomicBoolean called1 = new AtomicBoolean();
    final AtomicBoolean called2 = new AtomicBoolean();
    final Exception expected = new Exception("test");

    runner.start(new AsyncCompletion() {
      @Override
      public void run(State state) throws Exception {
        throw expected;
      }
      @Override
      public void onException(Throwable exception) {
        assertSame(expected, exception);
        called0.set(true);
        done.countDown();
      }
    }, new CompletionAdapter() {
      @Override
      public void onException(Throwable exception) {
        assertSame(expected, exception);
        called1.set(true);
        done.countDown();
      }
    }, new CompletionAdapter() {
      @Override
      public void onException(Throwable exception) {
        assertSame(expected, exception);
        called2.set(true);
        done.countDown();
      }
    });
    if (!done.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
      fail("timeout");
    }
    assertTrue(called0.get());
    assertTrue(called1.get());
    assertTrue(called2.get());
  }

  @Test
  public void testAsyncFinallyInvokedOnlyOnceOnAllHandlers() throws InterruptedException {
    final CountDownLatch done = new CountDownLatch(3);
    final CountDownLatch ready = new CountDownLatch(1);
    final AtomicInteger count0 = new AtomicInteger();
    final AtomicInteger count1 = new AtomicInteger();
    final AtomicInteger count2 = new AtomicInteger();
    AsyncRun async = runner.start(new AsyncCompletion() {
      @Override
      public void run(State state) throws Exception {
        ready.countDown();
        if (!done.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
          fail("Test timed out");
        }
      }
      @Override
      public void onFinally() {
        count0.incrementAndGet();
        done.countDown();
      }
    }, new CompletionAdapter() {
      @Override
      public void onFinally() {
        count1.incrementAndGet();
        done.countDown();
      }
    }, new CompletionAdapter() {
      @Override
      public void onFinally() {
        count2.incrementAndGet();
        done.countDown();
      }
    });
    // wait for the async to be started so that we know for sure that onFinally will be invoked at
    // least once.
    if (!ready.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
      fail("Test timed out");
    }
    // cancel should cause onFinally to be invoked
    async.cancel();
    if (!done.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
      fail("Test timed out");
    }
    // onFinally has been invoked
    assertEquals(1, count0.get());
    assertEquals(1, count1.get());
    assertEquals(1, count2.get());
  }

  @Test
  public void testRequestResponseOnAllHandlers() throws InterruptedException {
    final CountDownLatch done = new CountDownLatch(3);
    final AtomicBoolean called0 = new AtomicBoolean();
    final AtomicBoolean called1 = new AtomicBoolean();
    final AtomicBoolean called2 = new AtomicBoolean();
    final Integer expected = Integer.valueOf(260773);
    // Test that handle value receives the expected one.
    runner.start(new AsyncResponse<Integer>() {
      @Override
      public Integer run(State state) {
        return expected;
      }
      @Override
      public void onResponse(Result<Integer> result) {
        assertSame(expected, result.get());
        called0.set(true);
        done.countDown();
      }
    }, new ResponseAdapter<Integer>() {
      @Override
      public void onResponse(Result<Integer> result) {
        assertSame(expected, result.get());
        called1.set(true);
        done.countDown();
      }
    }, new ResponseAdapter<Integer>() {
      @Override
      public void onResponse(Result<Integer> result) {
        assertSame(expected, result.get());
        called2.set(true);
        done.countDown();
      }
    });
    if (!done.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
      fail("timeout");
    }
    assertTrue(called0.get());
    assertTrue(called1.get());
    assertTrue(called2.get());
  }

  @Test
  public void testRequestResponseOnImplicitHandler() throws InterruptedException {
    final CountDownLatch done = new CountDownLatch(1);
    // Test that handle value receives the expected one.
    runner.start(new AsyncResponse<Integer>() {
      Integer expected = Integer.valueOf(260773);
      @Override
      public Integer run(State state) {
        return expected;
      }
      @Override
      public void onResponse(Result<Integer> result) {
        assertSame(expected, result.get());
        done.countDown();
      }
    });
    if (!done.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
      fail("timeout");
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRequestNoImplicitHandler() throws InterruptedException {
    // Test that handle value receives the expected one.
    runner.start(new Request<Integer>() {
      @Override
      public Integer run(State state) {
        return null;
      }
    });
  }

  @Test(expected = NullPointerException.class)
  public void testRequestAndNullHandler() throws InterruptedException {
    // Test that handle value receives the expected one.
    runner.start(new Request<Integer>() {
      @Override
      public Integer run(State state) {
        return null;
      }
    }, (ResponseHandler<Integer>[]) null);
  }

  @Test
  public void testRequestRecevingExceptionOnAllHandlers() throws InterruptedException {
    // Test that onException receives the correct exception
    final CountDownLatch done = new CountDownLatch(3);
    final AtomicBoolean called0 = new AtomicBoolean();
    final AtomicBoolean called1 = new AtomicBoolean();
    final AtomicBoolean called2 = new AtomicBoolean();
    final Exception expected = new Exception("test");

    runner.start(new AsyncResponse<Integer>() {
      @Override
      public Integer run(State state) throws Exception {
        throw expected;
      }
      @Override
      public void onException(Throwable exception) {
        assertSame(expected, exception);
        called0.set(true);
        done.countDown();
      }
    }, new ResponseAdapter() {
      @Override
      public void onException(Throwable exception) {
        assertSame(expected, exception);
        called1.set(true);
        done.countDown();
      }
    }, new ResponseAdapter() {
      @Override
      public void onException(Throwable exception) {
        assertSame(expected, exception);
        called2.set(true);
        done.countDown();
      }
    });
    if (!done.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
      fail("timeout");
    }
    assertTrue(called0.get());
    assertTrue(called1.get());
    assertTrue(called2.get());
  }

  @Test
  public void testRequestFinallyInvokedOnlyOnceOnAllHandlers() throws InterruptedException {
    final CountDownLatch done = new CountDownLatch(3);
    final CountDownLatch ready = new CountDownLatch(1);
    final AtomicInteger count0 = new AtomicInteger();
    final AtomicInteger count1 = new AtomicInteger();
    final AtomicInteger count2 = new AtomicInteger();
    AsyncRun async = runner.start(new AsyncResponse<Integer>() {
      @Override
      public Integer run(State state) throws Exception {
        ready.countDown();
        if (!done.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
          fail("Test timed out");
        }
        return null;
      }
      @Override
      public void onFinally() {
        count0.incrementAndGet();
        done.countDown();
      }
    }, new ResponseAdapter() {
      @Override
      public void onFinally() {
        count1.incrementAndGet();
        done.countDown();
      }
    }, new ResponseAdapter() {
      @Override
      public void onFinally() {
        count2.incrementAndGet();
        done.countDown();
      }
    });
    // wait for the async to be started so that we know for sure that onFinally will be invoked at
    // least once.
    if (!ready.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
      fail("Test timed out");
    }
    // cancel should cause onFinally to be invoked
    async.cancel();
    if (!done.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
      fail("Test timed out");
    }
    // onFinally has been invoked
    assertEquals(1, count0.get());
    assertEquals(1, count1.get());
    assertEquals(1, count2.get());
  }

  @Test
  public void testProgressiveProgressOnAllHandlers() throws InterruptedException {
    final CountDownLatch done = new CountDownLatch(3);
    final AtomicBoolean called0 = new AtomicBoolean();
    final AtomicBoolean called1 = new AtomicBoolean();
    final AtomicBoolean called2 = new AtomicBoolean();

    final Integer expected = Integer.valueOf(260773);
    runner.start(new AsyncProgress<Integer>() {
      @Override
      public void run(ProgressState<Integer> notifier) {
        notifier.notifyProgress(expected);
      }
      @Override
      public void onProgress(Progress<Integer> progress) {
        assertSame(expected, progress.get());
        called0.set(true);
        done.countDown();
      }
    }, new ProgressAdapter<Integer>() {
      @Override
      public void onProgress(Progress<Integer> progress) {
        assertSame(expected, progress.get());
        called1.set(true);
        done.countDown();
      }
    }, new ProgressAdapter<Integer>() {
      @Override
      public void onProgress(Progress<Integer> progress) {
        assertSame(expected, progress.get());
        called2.set(true);
        done.countDown();
      }
    });
    if (!done.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
      fail("timeout");
    }
    assertTrue(called0.get());
    assertTrue(called1.get());
    assertTrue(called2.get());
  }

  @Test
  public void testProgressiveOnImplicitHandler() throws InterruptedException {
    final CountDownLatch done = new CountDownLatch(1);
    runner.start(new AsyncProgress<Integer>() {
      Integer expected = Integer.valueOf(260773);
      @Override
      public void run(ProgressState<Integer> notifier) {
        notifier.notifyProgress(expected);
      }
      @Override
      public void onProgress(Progress<Integer> progress) {
        assertSame(expected, progress.get());
        done.countDown();
      }
    });
    if (!done.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
      fail("timeout");
    }
  }

  @Test
  public void testProgressiveCallsCompleteOnAllHandlers() throws InterruptedException {
    final CountDownLatch done = new CountDownLatch(3);
    final AtomicBoolean called0 = new AtomicBoolean();
    final AtomicBoolean called1 = new AtomicBoolean();
    final AtomicBoolean called2 = new AtomicBoolean();

    runner.start(new AsyncProgress<Integer>() {
      @Override
      public void run(ProgressState<Integer> notifier) {}
      @Override
      public void onComplete() {
        called0.set(true);
        done.countDown();
      }
    }, new ProgressAdapter<Integer>() {
      @Override
      public void onComplete() {
        called1.set(true);
        done.countDown();
      }
    }, new ProgressAdapter<Integer>() {
      @Override
      public void onComplete() {
        called2.set(true);
        done.countDown();
      }
    });
    if (!done.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
      fail("timeout");
    }
    assertTrue(called0.get());
    assertTrue(called1.get());
    assertTrue(called2.get());
  }

  @Test
  public void testProgressiveCallsCompleteOnImplicitHandler() throws InterruptedException {
    final CountDownLatch done = new CountDownLatch(1);
    runner.start(new AsyncProgress<Integer>() {
      @Override
      public void run(ProgressState<Integer> notifier) {}
      @Override
      public void onComplete() {
        done.countDown();
      }
    });
    if (!done.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
      fail("timeout");
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testProgressiveNoImplicitHandler() throws InterruptedException {
    // Test that handle value receives the expected one.
    runner.start(new Progressive<Integer>() {
      @Override
      public void run(ProgressState<Integer> notifier) {}
    });
  }

  @Test(expected = NullPointerException.class)
  public void testProgressiveAndNullHandler() throws InterruptedException {
    // Test that handle value receives the expected one.
    runner.start(new Progressive<Integer>() {
      @Override
      public void run(ProgressState<Integer> notifier) {}
    }, (ProgressHandler<Integer>[]) null);
  }

  @Test
  public void testProgressiveDoesntCallCompleteOnException() throws InterruptedException {
    final CountDownLatch done = new CountDownLatch(1);

    runner.start(new AsyncProgress<Integer>() {
      @Override
      public void run(ProgressState<Integer> notifier) throws Exception {
        throw new Exception("test");
      }
      @Override
      public void onException(Throwable exception) {
        done.countDown();
      }
      @Override
      public void onComplete() {
        fail("complete should not be invoked on exception");
      }
    });
    if (!done.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
      fail("timeout");
    }
  }

  @Test
  public void testProgressiveRecevingExceptionOnAllHandlers() throws InterruptedException {
    // Test that onException receives the correct exception
    final CountDownLatch done = new CountDownLatch(3);
    final AtomicBoolean called0 = new AtomicBoolean();
    final AtomicBoolean called1 = new AtomicBoolean();
    final AtomicBoolean called2 = new AtomicBoolean();
    final Exception expected = new Exception("test");

    runner.start(new AsyncProgress<Integer>() {
      @Override
      public void run(ProgressState<Integer> state) throws Exception {
        throw expected;
      }
      @Override
      public void onException(Throwable exception) {
        assertSame(expected, exception);
        called0.set(true);
        done.countDown();
      }
    }, new ProgressAdapter() {
      @Override
      public void onException(Throwable exception) {
        assertSame(expected, exception);
        called1.set(true);
        done.countDown();
      }
    }, new ProgressAdapter() {
      @Override
      public void onException(Throwable exception) {
        assertSame(expected, exception);
        called2.set(true);
        done.countDown();
      }
    });
    if (!done.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
      fail("timeout");
    }
    assertTrue(called0.get());
    assertTrue(called1.get());
    assertTrue(called2.get());
  }

  @Test
  public void testProgressiveFinallyInvokedOnlyOnceOnAllHandlers() throws InterruptedException {
    final CountDownLatch done = new CountDownLatch(3);
    final CountDownLatch ready = new CountDownLatch(1);
    final AtomicInteger count0 = new AtomicInteger();
    final AtomicInteger count1 = new AtomicInteger();
    final AtomicInteger count2 = new AtomicInteger();
    AsyncRun async = runner.start(new AsyncProgress<Integer>() {
      @Override
      public void run(ProgressState<Integer> state) throws Exception {
        ready.countDown();
        if (!done.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
          fail("Test timed out");
        }
      }
      @Override
      public void onFinally() {
        count0.incrementAndGet();
        done.countDown();
      }
    }, new ProgressAdapter() {
      @Override
      public void onFinally() {
        count1.incrementAndGet();
        done.countDown();
      }
    }, new ProgressAdapter() {
      @Override
      public void onFinally() {
        count2.incrementAndGet();
        done.countDown();
      }
    });
    // wait for the async to be started so that we know for sure that onFinally will be invoked at
    // least once.
    if (!ready.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
      fail("Test timed out");
    }
    // cancel should cause onFinally to be invoked
    async.cancel();
    if (!done.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
      fail("Test timed out");
    }
    // onFinally has been invoked
    assertEquals(1, count0.get());
    assertEquals(1, count1.get());
    assertEquals(1, count2.get());
  }

  @Test
  public void testIsDone() throws InterruptedException {
    final CountDownLatch done = new CountDownLatch(1);
    AsyncRun async = runner.start(new AsyncCompletion() {
      @Override
      public void run(State state) throws Exception {}
      @Override
      public void onComplete() {
        done.countDown();
      }
    });
    if (!done.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
      fail("Test timed out");
    }
    assertTrue(async.isDone());
  }

  @Test
  public void testCancelOnInterruptibleMethods() throws InterruptedException {
    final CountDownLatch start = new CountDownLatch(1);
    final CountDownLatch done = new CountDownLatch(1);
    AsyncRun run = runner.start(new AsyncCompletion() {
      @Override
      public void run(State state) throws Exception {
        start.countDown();
        synchronized (this) {
          wait(TIMEOUT);
        }
      }
      public void onFinally() {
        done.countDown();
      }
    });
    if (!start.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
      fail("Test timed out");
    }
    run.cancel();
    if (!done.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
      fail("Test timed out");
    }
    assertTrue(run.isCancelled());
    assertTrue(run.isDone());
  }

  @Test
  public void testCancelPendings() throws InterruptedException {
    final CountDownLatch ready = new CountDownLatch(1);
    final CountDownLatch cancelled = new CountDownLatch(1);

    AsyncRun ran = runner.start(new Async() {
      @Override
      public void run(State state) throws InterruptedException {
        ready.countDown();
        cancelled.await();
      }
    });
    AsyncRun notRan = runner.start(new Async() {
      @Override
      public void run(State state) {}
    });

    ready.await();
    runner.cancelPendings();
    cancelled.countDown();

    assertTrue("Call1 should execute", ran.isStarted());
    assertFalse("Call2 should not execute", notRan.isStarted());
  }

  @Test
  public void testCancelPendingsFinally() throws InterruptedException {
    final CountDownLatch ready = new CountDownLatch(1);
    final CountDownLatch done = new CountDownLatch(1);

    AsyncRun running = runner.start(new Async() {
      @Override
      public void run(State state) throws InterruptedException {
        ready.countDown();
        synchronized (this) {
          wait(TIMEOUT);
        }
      }
    });

    AsyncRun pending = runner.start(new AsyncCompletion() {
      @Override
      public void run(State state) {
        fail("This should not have started");
      }
      @Override
      public void onFinally() {
        done.countDown();
      }
    });

    if (!ready.await(TIMEOUT, MILLISECONDS)) {
      fail("Test timed out");
    }
    runner.cancelPendings();
    if (!done.await(TIMEOUT, MILLISECONDS)) {
      fail("Test timed out");
    }
    assertTrue(running.isStarted());
    assertFalse(running.isCancelled());

    assertFalse(pending.isStarted());
    assertTrue(pending.isCancelled());
    assertTrue(pending.isDone());
  }

  /**
   * Tests the ability to cancel an asynchronous task from within the consumer.
   */
  @Test
  public void testCancelProgressiveFromProgress() throws InterruptedException {
    final CountDownLatch done = new CountDownLatch(1);
    AsyncRun run = runner.start(new AsyncProgress<Integer>() {
      @Override
      public void run(ProgressState<Integer> state) throws Exception {
        state.notifyProgress(Integer.valueOf(1));
        state.checkCancelled();
      }
      @Override
      public void onProgress(Progress<Integer> progress) {
        progress.cancel();
      }

      @Override
      public void onFinally() {
        done.countDown();
      }
    });
    assertFalse(run.isDone());
    if (!done.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
      fail("Test timed out");
    }
    assertTrue(run.isCancelled());
  }

  @Test
  public void testCancelWhenDone() throws InterruptedException {
    final CountDownLatch done = new CountDownLatch(1);
    AsyncRun run = runner.start(new AsyncCompletion() {
      @Override
      public void run(State state) throws Exception {}
      @Override
      public void onComplete() {
        // When finally is invoked the async is already marked as done
        done.countDown();
      }
    });
    if (!done.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
      fail("Test timed out");
    }
    assertTrue(run.isDone());
    run.cancel();
    assertFalse(run.isCancelled());
  }

  @Test
  public void testCancelIgnored() throws InterruptedException {
    final CountDownLatch ready = new CountDownLatch(1);
    final AtomicBoolean cancelled = new AtomicBoolean();
    final CountDownLatch done = new CountDownLatch(1);
    AsyncRun run = runner.start(new AsyncCompletion() {
      @Override
      public void run(State state) {
        ready.countDown();
        // not calling state.checkCancelled() so the cancellation is not noted
        while(!cancelled.get());
      }
      @Override
      public void onComplete() {
        // When finally is invoked the async is already marked as done
        done.countDown();
      }
    });
    if (!ready.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
      fail("Test timed out");
    }
    run.cancel();
    cancelled.set(true);
    if (!done.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
      fail("Test timed out");
    }
    assertTrue(run.isDone());
    assertFalse(run.isCancelled());
  }
}
