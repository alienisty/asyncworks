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

import static java.lang.reflect.Modifier.isPublic;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * @author Alex Nistico &lt;alienisty(at)gmail.com&gt;
 */
abstract class HandlingProcessor {

  private HandlingProcessor() {}

  @CheckForNull
  private static final Method runOn(@NonNull Class<?> clazz, @NonNull String name,
    @NonNull Class<?>... parameterTypes) {
    try {
      return runOn(clazz.getMethod(name, parameterTypes));
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @CheckForNull
  private static final Method runOn(@NonNull Method method) {
    try {
      // this call should never cause an exception because the methods checked are always public
      RunOn runOn = method.getAnnotation(RunOn.class);
      if (runOn != null) {
        return runOn.value().getDeclaredMethod("transfer", Runnable.class);
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return null;
  }

  private static final void transfer(@NonNull Method transferer, @NonNull Runnable run) {
    try {
      transferer.invoke(null, run);
    } catch (InvocationTargetException e) {
      handleInvocationTargetException(e);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  @NonNull
  private static final Method findBestMatch(@NonNull ExceptionHandler handler,
    @NonNull Throwable throwable) {
    final Class<?> handlerClass = handler.getClass();
    Class<?> throwableClass = throwable.getClass();
    do {
      try {
        return handlerClass.getMethod("onException", throwableClass);
      } catch (NoSuchMethodException e) {
        throwableClass = throwableClass.getSuperclass();
        if (throwableClass == Object.class) {
          // this should never happen because onException(Throwable) must be present for an instance
          // of ExceptionHandler
          throw new UnknownError("Expected method onException(Throwable) not found");
        }
      }
    } while (true);
  }

  private static final void handleException(@NonNull ExceptionHandler handler,
    @NonNull Method handlerMethod, @NonNull Throwable throwable) {
    if (handlerMethod.getParameterTypes()[0] == Throwable.class) {
      // The handler method is the one defined in the interface, fallback to it
      handler.onException(throwable);
    } else {
      try {
        if (!isPublic(handlerMethod.getDeclaringClass().getModifiers())) {
          // There is a bug in reflection where a public method from a non public class cannot be
          // invoked even if the method is an implementation of an interface and the non public
          // class
          // is used as an instance of that interface.
          // See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4071957
          handlerMethod.setAccessible(true);
        }
        handlerMethod.invoke(handler, throwable);
      } catch (InvocationTargetException e) {
        // The execution of the handling caused an exception
        handleInvocationTargetException(e);
      } catch (SecurityException e) {
        throw e;
      } catch (IllegalAccessException e) {
        // This should never happen, selected methods are only public
        throw new RuntimeException(e);
      }
    }
  }

  private static void handleInvocationTargetException(InvocationTargetException e) {
    Throwable cause = e.getCause();
    if (cause instanceof RuntimeException) {
      throw (RuntimeException) cause;
    } else if (cause instanceof Error) {
      throw (Error) cause;
    }
    throw new RuntimeException(cause);
  }

  static final void handleException(@NonNull final ExceptionHandler handler,
    final @NonNull Throwable throwable) {
    final Method bestMatch = findBestMatch(handler, throwable);
    Method tranferer = runOn(bestMatch);
    if (tranferer != null) {
      transfer(tranferer, new Runnable() {
        @Override
        public void run() {
          handleException(handler, bestMatch, throwable);
        }
      });
    } else {
      handleException(handler, bestMatch, throwable);
    }
  }

  static final <T> void handleResult(final @NonNull ResponseHandler<T> handler,
    final Result<T> value) {
    Method transferer = runOn(handler.getClass(), "onResponse", Result.class);
    if (transferer != null) {
      transfer(transferer, new Runnable() {
        @Override
        public void run() {
          handler.onResponse(value);
        }
      });
    } else {
      handler.onResponse(value);
    }
  }

  static final <T> void handleProgress(final @NonNull ProgressHandler<T> handler,
    @CheckForNull final Progress<T> value) {
    Method transferer = runOn(handler.getClass(), "onProgress", Progress.class);
    if (transferer != null) {
      transfer(transferer, new Runnable() {
        @Override
        public void run() {
          handler.onProgress(value);
        }
      });
    } else {
      handler.onProgress(value);
    }
  }

  static final void complete(final @NonNull CompletionHandler handler) {
    Method transferer = runOn(handler.getClass(), "onComplete");
    if (transferer != null) {
      transfer(transferer, new Runnable() {
        @Override
        public void run() {
          handler.onComplete();
        }
      });
    } else {
      handler.onComplete();
    }
  }

  static final void handleFinally(final @NonNull ExceptionHandler handler) {
    Method transferer = runOn(handler.getClass(), "onFinally");
    if (transferer != null) {
      transfer(transferer, new Runnable() {
        @Override
        public void run() {
          handler.onFinally();
        }
      });
    } else {
      handler.onFinally();
    }
  }
}
