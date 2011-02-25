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

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A runner for asynchronous operations. The invoking thread will be freed up as soon as the request
 * is sent to the executor.
 * <p>
 * The call-backs will be executed in a thread that is implementation dependent, so, synchronisation
 * should be always taken into account.
 * <p>
 * Note also, that potential deadlocks can arise with the use of specific programming patterns, that
 * is, when the calling thread tries to synchronise with the async or the call-back. Such patterns
 * are strongly discouraged because contrary to the asynchronous model and they should be used only
 * for testing purposes. If you want to synchronously call an asynchronous interface you should use
 * the {@link SyncRunner}
 * <p>
 * For example, if we have a runner that uses a single thread, and we try to execute something like:
 * 
 * <pre>
 *   ....
 *   final Exchanger&lt;Thread&gt; synch = new Exchanger&lt;Thread&gt;();
 *   final Exchanger&lt;Thread&gt; synch2 = new Exchanger&lt;Thread&gt;();
 * 
 *   runner.start(new Async() { // Call 1
 *     public void run(State state) throws InterruptedException {
 *       synch.exchange(Thread.currentThread());
 *     }
 *   });
 * 
 *   runner.start(new Async() { // Call 2
 *     public void run(State state) throws InterruptedException {
 *       synch2.exchange(Thread.currentThread());
 *     }
 *   });
 *   Thread received2 = synch2.exchange(null);
 *   Thread received = synch.exchange(null);
 *   ....
 * </pre>
 * 
 * <p>
 * Then {@code synch} in Call 1 will never exchange because {@code synch2} is trying to exchange
 * beforehand, in the calling thread, while the only thread available to the runner is blocking
 * waiting for the exchange on {@code synch} in Call 1. Therefore Call 2 cannot execute making
 * impossible to unblock the call:
 * 
 * <pre>
 *   Thread received2 = synch2.exchange(null)
 * </pre>
 * 
 * </p>
 * 
 * @author Alex Nistico &lt;alienisty(at)gmail.com&gt;
 */
public interface AsyncRunner {

  /**
   * Asynchronously invokes the specified {@link Async}.
   * <p>
   * Note that if the passed {@link Async} also implements {@link CompletionHandler}, it will
   * receive call-backs notification from the computation.
   * </p>
   * <p>
   * This feature is provided as a convenience to allow the use of a single instance to define the
   * computation and the call-backs, making the syntax neater when used as an anonymous class, eg:
   * 
   * <pre>
   * runner.start(new AsyncCompletion() {
   *   &#064;Override
   *   public void run(State state) {
   *     // do something
   *   }
   *   &#064;Override
   *   public void complete() {
   *     // do something else
   *   }
   *   &#064;Override
   *   public void onException(Throwable exception) {
   *     // handle problems
   *   }
   *   &#064;Override
   *   public void onFinally() {
   *     // do something at the very end
   *   }
   * });
   * </pre>
   * 
   * Note that, except for {@link Async#run(State)}, not all other call-backs are necessary, you can
   * only use the one you want to act on.
   * <p>
   * 
   * @param async
   *          the {@link Async} to be started.
   * @param handlers
   *          {@link CompletionHandler}s registered to handle call-backs from the computation.
   * 
   * @return The {@link AsyncRun} instance for the started {@link Async}.
   */
  @NonNull
  public AsyncRun start(@NonNull Async async, @NonNull CompletionHandler... handlers);

  /**
   * Asynchronously invokes the specified {@link Request}.
   * <p>
   * Note that if the passed {@link Request} also implements {@link ResponseHandler}, it will
   * receive call-backs notification from the computation.
   * </p>
   * <p>
   * This feature is provided as a convenience to allow the use a single instance to define the
   * execution and the handling of the response, making the syntax neater when used as an anonymous
   * class, eg:
   * 
   * <pre>
   * runner.start(new AsyncResponse&lt;String&gt;() {
   *   &#064;Override
   *   public String run(State state) {
   *     // do something
   *   }
   *   &#064;Override
   *   public void onResponse(Result&lt;String&gt; response) {
   *     // do something else
   *   }
   *   &#064;Override
   *   public void onException(Throwable exception) {
   *     // handle problems
   *   }
   *   &#064;Override
   *   public void onFinally() {
   *     // do something at the very end
   *   }
   * });
   * </pre>
   * 
   * Note that, except for {@link Request#run(State)}, not all other call-backs are necessary, you
   * can only use the one you want to act on.
   * <p>
   * 
   * @param <T>
   *          the type for the requested value.
   * 
   * @param request
   *          the {@link Request} instance to run.
   * @param handlers
   *          {@link ResponseHandler}s registered to handle call-backs from the computation.
   * 
   * @return The {@link AsyncRun} instance for the started {@link Request}.
   * 
   * @throws IllegalArgumentException
   *           if handlers are not provided and the {@code request} instance does not implement
   *           {@link ResponseHandler}.
   */
  @NonNull
  public <T> AsyncRun start(@NonNull Request<T> request, @NonNull ResponseHandler<T>... handlers);

  /**
   * Asynchronously invokes the specified {@link Progressive}.
   * <p>
   * Note that if the passed {@link Progressive} also implements {@link ProgressHandler}, it will
   * receive call-backs notification from the computation.
   * </p>
   * <p>
   * This feature is provided as a convenience to allow the use a single instance to define the
   * execution and the handling of the progress, making the syntax neater when used as an anonymous
   * class, eg:
   * 
   * <pre>
   * runner.start(new AsyncProgress&lt;String&gt;() {
   *   &#064;Override
   *   public String run(ProgressState state) {
   *     // do something
   *   }
   *   &#064;Override
   *   public void onProgress(Progress&lt;String&gt; response) {
   *     // do something else
   *   }
   *   &#064;Override
   *   public void complete() {
   *     // do something else again
   *   }
   *   &#064;Override
   *   public void onException(Throwable exception) {
   *     // handle problems
   *   }
   *   &#064;Override
   *   public void onFinally() {
   *     // do something at the very end
   *   }
   * });
   * </pre>
   * 
   * Note that, except for {@link Progressive#run(ProgressState)}, not all other call-backs are
   * necessary, you can only use the one you want to act on.
   * <p>
   * 
   * @param <T>
   *          the type for the requested value.
   * 
   * @param progressive
   *          the {@link Progressive} instance to run.
   * @param handlers
   *          {@link ProgressHandler}s registered to handle call-backs from the computation.
   * 
   * @return The {@link AsyncRun} instance for the started {@link Progressive}.
   * 
   * @throws IllegalArgumentException
   *           if handlers are not provided and the {@code progressive} instance does not implement
   *           {@link ProgressHandler}.
   */
  @NonNull
  public <T> AsyncRun start(@NonNull Progressive<T> progressive,
    @NonNull ProgressHandler<T>... handlers);

  /**
   * Cancels pending asynchronous computations;
   */
  public void cancelPendings();
}
