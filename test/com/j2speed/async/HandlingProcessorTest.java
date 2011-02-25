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

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.security.AccessControlException;
import java.security.Permission;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Exchanger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.j2speed.async.ExceptionAdapter;
import com.j2speed.async.ExceptionHandler;
import com.j2speed.async.HandlingProcessor;
import com.j2speed.async.ProgressAdapter;
import com.j2speed.async.ResponseAdapter;
import com.j2speed.async.hidden.PrivateExceptionHandlerProvider;

@SuppressWarnings("all")
@edu.umd.cs.findbugs.annotations.SuppressWarnings()
public class HandlingProcessorTest {

  @Test
  public void testHandleException() {
    Throwable expected = new NullPointerException();
    final AtomicReference<Throwable> received = new AtomicReference<Throwable>();
    HandlingProcessor.handleException(new ExceptionAdapter() {
      @Override
      public void onException(Throwable exception) {
        received.set(exception);
      }
    }, expected);
    assertSame(expected, received.get());
  }

  @Test
  public void testHandleExceptionSpecificThatThrows() {
    Throwable expected = new NullPointerException();
    try {
      HandlingProcessor.handleException(new ExceptionAdapter() {
        public void handleException(NullPointerException exception) throws Exception {
          throw new Exception(exception);
        }
      }, expected);
    } catch (RuntimeException e) {
      assertSame(e.getCause().getClass(), Exception.class);
      assertSame(expected, e.getCause().getCause());
    }
  }

  @Test
  public void testHandleExceptionSpecificThatThrowsRuntime() {
    Throwable expected = new NullPointerException();
    try {
      HandlingProcessor.handleException(new ExceptionAdapter() {
        public void handleException(NullPointerException exception) {
          throw exception;
        }
      }, expected);
    } catch (NullPointerException e) {
      assertSame(expected, e);
    }
  }

  @Test
  public void testHandleExceptionSpecificThatThrowsError() {
    Throwable expected = new Error();
    try {
      HandlingProcessor.handleException(new ExceptionAdapter() {
        public void handleException(Error exception) {
          throw exception;
        }
      }, expected);
    } catch (Error e) {
      assertSame(expected, e);
    }
  }

  @Test(expected = AccessControlException.class)
  public void testHandleExceptionNotAccessible() {
    SecurityManager oldSecurity = System.getSecurityManager();
    try {
      System.setSecurityManager(new SecurityManager() {
        @Override
        public void checkPermission(Permission perm) {
          if ("suppressAccessChecks".equals(perm.getName())) {
            throw new AccessControlException("access denied " + perm, perm);
          }
        }
      });

      HandlingProcessor.handleException(new ExceptionAdapter() {
        public void onException(NullPointerException exception) {
          fail();
        }
      }, new NullPointerException());
    } finally {
      System.setSecurityManager(oldSecurity);
    }
  }

  @Test
  public void testHandleExceptionSelectOnlyPublicMethods() {
    Throwable expected = new NullPointerException();
    AtomicReference<Throwable> received = new AtomicReference<Throwable>();
    HandlingProcessor.handleException(new SneakyExceptionHandler(received), expected);
    assertSame(expected, received.get());
  }

  @Test
  public void testJvmBug4071957_second() {
    // Tests handling for bug 4071957
    // see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4071957
    Throwable expected = new NullPointerException();
    AtomicReference<String> target = new AtomicReference<String>();
    ExceptionHandler handler = PrivateExceptionHandlerProvider.makeSpecific(target);
    HandlingProcessor.handleException(handler, expected);
    assertEquals(PrivateExceptionHandlerProvider.message, target.get());
  }

  @Test
  public void testHandleSpecificException() {
    final AtomicReference<Throwable> received = new AtomicReference<Throwable>();
    final AtomicReference<NullPointerException> specific = new AtomicReference<NullPointerException>();
    Throwable expected = new NullPointerException();
    HandlingProcessor.handleException(new ExceptionAdapter() {
      @Override
      public void onException(Throwable exception) {
        received.set(exception);
      }

      public void onException(NullPointerException exception) {
        specific.set(exception);
      }
    }, expected);
    assertNull(received.get());
    assertSame(expected, specific.get());
  }

  @Test
  public void testHandleSpecificExceptionPolymorphic() {
    final AtomicReference<Throwable> received = new AtomicReference<Throwable>();
    final AtomicReference<IOException> specific = new AtomicReference<IOException>();
    Throwable expected = new FileNotFoundException();
    HandlingProcessor.handleException(new ExceptionAdapter() {
      @Override
      public void onException(Throwable exception) {
        received.set(exception);
      }

      public void onException(IOException exception) {
        specific.set(exception);
      }
    }, expected);
    assertNull(received.get());
    assertSame(expected, specific.get());
  }

  @Test
  public void testHandleFinnally() {
    final AtomicBoolean received = new AtomicBoolean();
    HandlingProcessor.handleFinally(new ExceptionAdapter() {
      @Override
      public void onFinally() {
        received.set(true);
      }
    });
    assertTrue(received.get());
  }

  @Test
  public void testHandleValue() {
    List<String> expected = Arrays.asList("Alex");
    final AtomicReference<Collection<String>> received = new AtomicReference<Collection<String>>();
    HandlingProcessor.handleResult(new ResponseAdapter<Collection<String>>() {
      @Override
      public void onResponse(Result<Collection<String>> value) {
        received.set(value.get());
      }
    }, new SimpleResult<Collection<String>>(expected));
    assertSame(expected, received.get());
  }

  @Test
  public void testHandleProgress() {
    List<String> expected = Arrays.asList("Alex");
    final AtomicReference<Collection<String>> received = new AtomicReference<Collection<String>>();
    HandlingProcessor.handleProgress(new ProgressAdapter<Collection<String>>() {
      @Override
      public void onProgress(Progress<Collection<String>> progress) {
        received.set(progress.get());
      }
    }, new SimpleProgress<Collection<String>>(expected));
    assertSame(expected, received.get());
  }

  @Test
  public void testProgressComplete() {
    final AtomicBoolean received = new AtomicBoolean();
    HandlingProcessor.complete(new ProgressAdapter<Collection<String>>() {
      @Override
      public void onComplete() {
        received.set(true);
      }
    });
    assertTrue(received.get());
  }

  @Test
  public void testComplete() {
    final AtomicBoolean received = new AtomicBoolean();
    HandlingProcessor.complete(new CompletionAdapter() {
      @Override
      public void onComplete() {
        received.set(true);
      }
    });
    assertTrue(received.get());
  }

  @Test(expected = RuntimeException.class)
  public void testCompleteTransferOnRuntimeException() {
    final AtomicBoolean received = new AtomicBoolean();
    HandlingProcessor.complete(new CompletionAdapter() {
      @Override
      @RunOn(RuntimeExceptionTransferer.class)
      public void onComplete() {
        received.set(true);
      }
    });
    assertTrue(received.get());
  }

  @Test(expected = Error.class)
  public void testCompleteTransferOnError() {
    final AtomicBoolean received = new AtomicBoolean();
    HandlingProcessor.complete(new CompletionAdapter() {
      @Override
      @RunOn(ErrorTransferer.class)
      public void onComplete() {
        received.set(true);
      }
    });
    assertTrue(received.get());
  }

  @Test
  public void testPrimitiveArrayClass() {
    final AtomicReference<int[]> received = new AtomicReference<int[]>();
    int[] value = new int[] { 1, 2, 3 };
    HandlingProcessor.handleResult(new ResponseAdapter<int[]>() {
      @Override
      public void onResponse(Result<int[]> result) {
        received.set(result.get());
      }
    }, new SimpleResult<int[]>(value));
    assertSame(value, received.get());
  }

  private static class SimpleResult<T> implements Result<T> {
    private final T value;
    SimpleResult(T value) {
      this.value = value;
    }
    @Override
    public T get() {
      return value;
    }
  }

  private static class SimpleProgress<T> extends SimpleResult<T> implements Progress<T> {
    SimpleProgress(T progress) {
      super(progress);
    }
    @Override
    public void cancel() {
      throw new UnsupportedOperationException();
    }
  }

  // Note this class needs to be public otherwise bug 4071957 could make the test fail
  public static class SneakyExceptionHandler extends ExceptionAdapter {
    final AtomicReference<Throwable> received;

    SneakyExceptionHandler(AtomicReference<Throwable> received) {
      this.received = received;
    }

    private void handleException(NullPointerException exception) {
      fail();
    }

    @Override
    public void onException(Throwable exception) {
      received.set(exception);
    }
  }

  static class RuntimeExceptionTransferer {
    public static void transfer(Runnable run) {
      throw new RuntimeException();
    }
  }

  static class ErrorTransferer {
    public static void transfer(Runnable run) {
      throw new Error();
    }
  }
}
