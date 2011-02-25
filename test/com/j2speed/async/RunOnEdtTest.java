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
import static java.util.concurrent.TimeUnit.SECONDS;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.j2speed.async.Async;
import com.j2speed.async.State;
import com.j2speed.async.DefaultRunner;
import com.j2speed.async.RunOn;
import com.j2speed.async.ProgressAdapter;
import com.j2speed.async.ProgressState;
import com.j2speed.async.Progressive;
import com.j2speed.async.Request;
import com.j2speed.async.ResponseAdapter;
import com.j2speed.async.ResponseHandler;
import com.j2speed.junit.ThreadTransferRunner;

@SuppressWarnings("all")
@edu.umd.cs.findbugs.annotations.SuppressWarnings
@RunWith(ThreadTransferRunner.class)
public class RunOnEdtTest {

  /**
   * Timeout in milliseconds to wait on another thread before failing this unit test.
   */
  private static final long TIMEOUT = 100;

  static DefaultRunner runner;

  @BeforeClass
  public static void init() {
    runner = new DefaultRunner(0, 2, TIMEOUT, TimeUnit.MILLISECONDS, "testEdt");
  }

  @AfterClass
  public static void destroy() {
    runner.shutdown();
  }

  @Before
  public void setUp() {
    runner.cancelPendings();
  }

  @Test
  public void testStartExceptionHandlerArray() throws InterruptedException, TimeoutException {
    final Exchanger<Void> synch = new Exchanger<Void>();
    // Test that execute is invoked outside the EDT
    runner.start(new Async() {

      @Override
      public void run(State state) throws InvocationTargetException {
        assertFalse(EventQueue.isDispatchThread());
        try {
          synch.exchange(null);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    });
    synch.exchange(null, TIMEOUT, TimeUnit.MILLISECONDS);

    // Test that onException receives the correct exception and it is
    // invoked in the EDT
    final Exception test = new Exception("test");
    runner.start(new Async() {
      @Override
      public void run(State state) throws Exception {
        throw test;
      }
    }, new CompletionAdapter() {
      @Override
      @RunOn(Edt.class)
      public void onException(Throwable exception) {
        assertTrue(EventQueue.isDispatchThread());
        assertSame(test, exception);
        try {
          synch.exchange(null);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    });
    synch.exchange(null, TIMEOUT, TimeUnit.MILLISECONDS);
  }

  @Test
  public void testPolymorphicExceptionHandler() throws InterruptedException, TimeoutException {
    final Exchanger<Void> synch = new Exchanger<Void>();
    // Test that onException receives the correct exception and it is
    // invoked in the EDT
    final Exception test = new Exception("test");
    runner.start(new Async() {

      @Override
      public void run(State state) throws Exception {
        throw test;
      }
    }, new CompletionAdapter() {
      @Override
      @RunOn(Edt.class)
      public void onException(Throwable exception) {
        fail("The more specific method should be invoked");
      }

      @RunOn(Edt.class)
      public void onException(Exception exception) {
        assertTrue(EventQueue.isDispatchThread());
        assertSame(test, exception);
        try {
          synch.exchange(null);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    });
    synch.exchange(null, TIMEOUT, TimeUnit.MILLISECONDS);
  }

  @Test
  public void testHandleFinally() throws InterruptedException, TimeoutException {
    final Exchanger<Void> synch = new Exchanger<Void>();
    // Test that onException receives the correct exception and it is
    // invoked in the EDT
    final Exception test = new Exception("test");
    runner.start(new Async() {

      @Override
      public void run(State state) throws Exception {
        throw test;
      }
    }, new CompletionAdapter() {
      @Override
      @RunOn(Edt.class)
      public void onFinally() {
        assertTrue(EventQueue.isDispatchThread());
        try {
          synch.exchange(null);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    });
    synch.exchange(null, TIMEOUT, MILLISECONDS);
  }

  @Test
  public void testStartRequestOfTResponseHandlerOfT() throws InterruptedException, TimeoutException {
    final Integer expected = Integer.valueOf(260773);
    final Exchanger<Integer> synch = new Exchanger<Integer>();
    // Test that execute is invoked outside the EDT, handle value is invoked in
    // the EDT and that the received value is the expected one.
    runner.start(new Request<Integer>() {

      @Override
      public Integer run(State state) throws InvocationTargetException {
        assertFalse(EventQueue.isDispatchThread());
        return expected;
      }
    }, new ResponseAdapter<Integer>() {

      @Override
      @RunOn(Edt.class)
      public void onResponse(Result<Integer> result) {
        assertTrue(EventQueue.isDispatchThread());
        try {
          synch.exchange(result.get());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    });
    Integer actual = synch.exchange(null, TIMEOUT, TimeUnit.MILLISECONDS);
    Assert.assertSame(expected, actual);

    // Test that onException receives the correct exception and it is
    // invoked in the EDT
    final Exception test = new Exception("test");
    runner.start(new Request<Integer>() {

      @Override
      public Integer run(State state) throws Exception {
        throw test;
      }
    }, new ResponseAdapter<Integer>() {

      @Override
      @RunOn(Edt.class)
      public void onException(Throwable exception) {
        assertTrue(EventQueue.isDispatchThread());
        assertSame(test, exception);
        try {
          synch.exchange(null);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    });
    synch.exchange(null, TIMEOUT, TimeUnit.MILLISECONDS);
  }

  @Test
  public void testStartRequestOfTResponseHandlerOfTGenerics() throws InterruptedException,
    TimeoutException {
    final Integer expected = Integer.valueOf(260773);
    final Exchanger<Number> synch = new Exchanger<Number>();
    // Test that execute is invoked outside the EDT, handle value is invoked in
    // the EDT and that the received value is the expected one.
    runner.start(new Request<Number>() {

      @Override
      public Number run(State state) throws InvocationTargetException {
        assertFalse(EventQueue.isDispatchThread());
        return expected;
      }
    }, new ResponseAdapter<Number>() {

      @Override
      @RunOn(Edt.class)
      public void onResponse(Result<Number> result) {
        assertTrue(EventQueue.isDispatchThread());
        try {
          synch.exchange(result.get());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    });
    Number actual = synch.exchange(null, TIMEOUT, TimeUnit.MILLISECONDS);
    Assert.assertSame(expected, actual);
  }

  @Test
  public void testStartRequestOfTResponseHandlerOfTGenericsWithInterface()
    throws InterruptedException, TimeoutException {
    final Integer expected = Integer.valueOf(260773);
    final Exchanger<Number> synch = new Exchanger<Number>();
    // Test that execute is invoked outside the EDT, handle value is invoked in
    // the EDT and that the received value is the expected one.
    runner.start(new Request<Number>() {

      @Override
      public Number run(State state) throws InvocationTargetException {
        assertFalse(EventQueue.isDispatchThread());
        return expected;
      }
    }, new ResponseHandler<Number>() {

      @Override
      @RunOn(Edt.class)
      public void onResponse(Result<Number> result) {
        assertTrue(EventQueue.isDispatchThread());
        try {
          synch.exchange(result.get());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }

      @Override
      public void onException(Throwable exception) {}

      @Override
      public void onFinally() {}
    });
    Number actual = synch.exchange(null, TIMEOUT, TimeUnit.MILLISECONDS);
    Assert.assertSame(expected, actual);
  }

  @Test
  public void testStartProgressiveOfTProgressHandlerOfT() throws InterruptedException,
    TimeoutException {
    final Integer expected = Integer.valueOf(260773);
    final Exchanger<Integer> synch = new Exchanger<Integer>();
    // Test that execute is invoked outside the EDT, handle value is invoked in
    // the EDT and that the received value is the expected one.
    runner.start(new Progressive<Integer>() {

      @Override
      public void run(ProgressState<Integer> notifier) throws InvocationTargetException {
        assertFalse(EventQueue.isDispatchThread());
        notifier.notifyProgress(expected);
      }
    }, new ProgressAdapter<Integer>() {

      @Override
      @RunOn(Edt.class)
      public void onProgress(Progress<Integer> progress) {
        assertTrue(EventQueue.isDispatchThread());
        try {
          synch.exchange(progress.get());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    });
    Integer actual = synch.exchange(null, TIMEOUT, TimeUnit.MILLISECONDS);
    Assert.assertSame(expected, actual);

    // Test that execute is invoked outside the EDT, handle value is invoked in
    // the EDT and that the received value is the expected one and that
    // complete() is called in the EDT when notifyOnComplete() returns true.
    runner.start(new Progressive<Integer>() {

      @Override
      public void run(ProgressState<Integer> notifier) throws InvocationTargetException {
        assertFalse(EventQueue.isDispatchThread());
        notifier.notifyProgress(expected);
      }
    }, new ProgressAdapter<Integer>() {

      private Integer received;

      @Override
      @RunOn(Edt.class)
      public void onProgress(Progress<Integer> progress) {
        assertTrue(EventQueue.isDispatchThread());
        received = progress.get();
      }

      @Override
      @RunOn(Edt.class)
      public void onComplete() {
        assertTrue(EventQueue.isDispatchThread());
        try {
          synch.exchange(received);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    });
    actual = synch.exchange(null, TIMEOUT, TimeUnit.MILLISECONDS);
    Assert.assertSame(expected, actual);

    // Test that onException receives the correct exception and it is
    // invoked in the EDT
    final Exception test = new Exception("test");
    runner.start(new Progressive<Integer>() {

      @Override
      public void run(ProgressState<Integer> notifier) throws Exception {
        throw test;
      }
    }, new ProgressAdapter<Integer>() {

      @Override
      @RunOn(Edt.class)
      public void onException(Throwable exception) {
        assertTrue(EventQueue.isDispatchThread());
        assertSame(test, exception);
        try {
          synch.exchange(null);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    });
    synch.exchange(null, TIMEOUT, TimeUnit.MILLISECONDS);
  }
}
