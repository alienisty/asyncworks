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

import static com.j2speed.async.HandlingProcessor.complete;
import static com.j2speed.async.HandlingProcessor.handleException;
import static com.j2speed.async.HandlingProcessor.handleFinally;
import static com.j2speed.async.HandlingProcessor.handleProgress;
import static com.j2speed.async.HandlingProcessor.handleResult;

import java.lang.reflect.Array;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnegative;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * An {@link AsyncRunner} that uses a bound number of maximum threads. The asynchrounous
 * computations will be run in one of these threads.
 * 
 * @author Alex Nistico &lt;alienisty(at)gmail.com&gt;
 */
public final class DefaultRunner implements AsyncRunner {
  /**
   * The executor that runs the asynchronous tasks.
   */
  @NonNull
  private final AsyncExecutor executor;

  /**
   * Constructor of the class.
   * 
   * @param minimumPoolSize
   *          the minimum number of live threads.
   * @param maximumPoolSize
   *          the maximum number of live threads.
   * @param keepAlive
   *          how long to keep them alive.
   * @param unit
   *          the time unit.
   * @param baseName
   *          The base name for the threads built by this executor
   */
  public DefaultRunner(@Nonnegative int minimumPoolSize, @Nonnegative int maximumPoolSize,
    @Nonnegative long keepAlive, @NonNull TimeUnit unit, @NonNull String baseName) {
    this(new ReentrantExecutor(minimumPoolSize, maximumPoolSize, keepAlive, unit, baseName));
  }

  /**
   * Constructs an runner with a specified number of threads that never die.
   * 
   * @param maximumPoolSize
   *          the maximum number of live threads.
   * @param baseName
   *          The base name for the threads built by this executor
   */
  public DefaultRunner(@Nonnegative int maximumPoolSize, @NonNull String baseName) {
    this(new ReentrantExecutor(maximumPoolSize, baseName));
  }

  DefaultRunner(@NonNull AsyncExecutor executor) {
    if (executor == null) {
      throw new NullPointerException();
    }
    this.executor = executor;
  }

  @Override
  public AsyncRun start(Async async, CompletionHandler... handlers) {
    AsyncJob task = new AsyncJob(async, handlers);
    executor.execute(task);
    return task;
  }

  @Override
  public <T> AsyncRun start(Request<T> request, ResponseHandler<T>... handlers) {
    if (handlers.length == 0 && !(request instanceof ResponseHandler<?>)) {
      throw new IllegalArgumentException();
    }
    RequestJob<T> task = new RequestJob<T>(request, handlers);
    executor.execute(task);
    return task;
  }

  @Override
  public <T> AsyncRun start(Progressive<T> request, ProgressHandler<T>... handlers) {
    if (handlers.length == 0 && !(request instanceof ProgressHandler<?>)) {
      throw new IllegalArgumentException();
    }
    ProgressiveJob<T> task = new ProgressiveJob<T>(request, handlers);
    executor.execute(task);
    return task;
  }

  @Override
  public void cancelPendings() {
    List<Runnable> cancelled = executor.cancelPendings();
    // Note that all these Runnables have been removed from the executor's queue, so they will never
    // be run.
    for (Runnable r : cancelled) {
      // Safe cast because the executor does not escape this runner, so we know what is the content.
      AbstractJob<?, ?> cancelledTask = (AbstractJob<?, ?>) r;
      cancelledTask.cancelUnstarted();
    }
  }

  /**
   * Shut down the this runner. All the pending asynchronous computations will be cancelled.
   */
  public final void shutdown() {
    executor.cancelPendings();
    executor.shutdown();
  }

  /**
   * An abstract computation job.
   * 
   * @param <A>
   *          the async type.
   * @param <H>
   *          the handler type
   */
  private abstract class AbstractJob<A, H extends ExceptionHandler> implements Runnable, AsyncRun, State {

    /**
     * Whether a request to cancel this task has been made.
     */
    @NonNull
    private final AtomicBoolean cancelRequested = new AtomicBoolean();

    /**
     * Whether this task has been started or not.
     */
    private volatile boolean started;

    /**
     * Whether this task has been cancelled or not.
     */
    private volatile boolean cancelled;

    /**
     * Whether this task is done or not.
     */
    private volatile boolean done;

    /**
     * The thread that is running this task.
     */
    @CheckForNull
    private volatile Thread runner;

    /**
     * The async to execute.
     */
    @NonNull
    private final A async;

    @NonNull
    final H[] handlers;

    private AbstractJob(@NonNull A async, @NonNull H... handlers) {
      this.async = async;
      this.handlers = prepare(async, handlers);
    }

    @NonNull
    @SuppressWarnings("unchecked")
    private final H[] prepare(@NonNull A async, @NonNull H... handlers) {
      Class<?> handlerClass = handlers.getClass().getComponentType();
      if (handlerClass.isInstance(async)) {
        H[] handlers2 = (H[]) Array.newInstance(handlerClass, handlers.length + 1);
        System.arraycopy(handlers, 0, handlers2, 1, handlers.length);
        handlers2[0] = (H) async;
        return handlers2;
      }
      return handlers;
    }

    @Override
    public final void run() {
      started = true;
      try {
        try {
          // set the reference to the running thread
          runner = Thread.currentThread();
          checkCancelled();
          doRun(async);
        } catch (CancelledException e) {
          cancelled = true;
        } catch (InterruptedException e) {
          if (!(cancelled = cancelRequested.get())) {
            // something else interrupted the thread, so we need to propagate the interruption
            Thread.currentThread().interrupt();
          }
        } finally {
          // at this point the execution is done, exception handling and finally is not considered
          // part of the execution.
          done = true;
          runner = null;
          if (cancelRequested.get()) {
            // the task has been cancelled in the meantime, clear the thread interrupted flag, in
            // case cancel managed to call interrupt on it.
            Thread.interrupted();
          }
        }
        if (CompletionHandler.class.isAssignableFrom(handlers.getClass().getComponentType())) {
          for (H handler : handlers) {
            complete((CompletionHandler) handler);
          }
        }
      } catch (Exception e) {
        for (H handler : handlers) {
          handleException(handler, e);
        }
      } finally {
        doFinnally();
      }
    }

    @Override
    public final void cancel() {
      if (done || !cancelRequested.compareAndSet(false, true)) {
        return;
      }
      if (executor.cancel(this)) {
        // this task will never start
        cancelUnstarted();
      } else {
        // this task is already on the way to execute
        Thread runner = this.runner;
        if (runner != null) {
          try {
            runner.interrupt();
          } catch (SecurityException e) {}// can't interrupt
        }
      }
    }

    @Override
    public final boolean isStarted() {
      return started;
    }

    @Override
    public final boolean isDone() {
      return done;
    }

    @Override
    public final void checkCancelled() {
      if (cancelRequested.get()) {
        throw new CancelledException();
      }
    }

    @Override
    public boolean isCancelled() {
      return cancelled;
    }

    final void cancelUnstarted() {
      cancelled = true;
      done = true;
      doFinnally();
    }

    abstract void doRun(@NonNull A async) throws Exception;

    final void doFinnally() {
      for (H handler : handlers) {
        handleFinally(handler);
      }
    }
  }

  private final class AsyncJob extends AbstractJob<Async, CompletionHandler> {
    private AsyncJob(@NonNull Async async, @NonNull CompletionHandler... handlers) {
      super(async, handlers);
    }

    @Override
    final void doRun(Async async) throws Exception {
      async.run(this);
    }
  }

  private final class RequestJob<T> extends AbstractJob<Request<T>, ResponseHandler<T>> implements Result<T> {

    /**
     * The result generated by the request.
     */
    @CheckForNull
    private volatile T value;

    private RequestJob(@NonNull Request<T> request, @NonNull ResponseHandler<T>... handlers) {
      super(request, handlers);
    }

    @Override
    final void doRun(Request<T> async) throws Exception {
      value = async.run(this);
      for (ResponseHandler<T> handler : handlers) {
        handleResult(handler, this);
      }
    }

    @Override
    public T get() {
      return value;
    }
  }

  private final class ProgressiveJob<T> extends AbstractJob<Progressive<T>, ProgressHandler<T>> implements ProgressState<T>, Progress<T> {

    /**
     * The current value notified as progress.
     */
    @CheckForNull
    private volatile T current;

    private ProgressiveJob(@NonNull Progressive<T> request, @NonNull ProgressHandler<T>... handlers) {
      super(request, handlers);
    }

    @Override
    final void doRun(Progressive<T> async) throws Exception {
      async.run(this);
    }

    @Override
    public final void notifyProgress(T value) {
      current = value;
      for (ProgressHandler<T> handler : handlers) {
        handleProgress(handler, this);
      }
    }

    @Override
    public T get() {
      return current;
    }
  }
}
