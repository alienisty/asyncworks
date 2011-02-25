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

import static java.lang.Thread.currentThread;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnegative;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * The default implementation of the {@link AsyncExecutor} interface.
 * 
 * @author Alex Nistico &lt;alienisty(at)gmail.com&gt;
 */
public class ReentrantExecutor extends ThreadPoolExecutor implements AsyncExecutor {
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
   * @param queue
   *          The custom blocking queue to be used.
   */
  private ReentrantExecutor(@Nonnegative int minimumPoolSize, @Nonnegative int maximumPoolSize,
    @Nonnegative long keepAlive, @NonNull TimeUnit unit, @NonNull String baseName,
    @NonNull ExecutionQueue queue) {
    super(minimumPoolSize, maximumPoolSize, keepAlive, unit, queue, queue);
    setThreadFactory(new ReentrantThreadFactory(baseName));
  }

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
  public ReentrantExecutor(@Nonnegative int minimumPoolSize, @Nonnegative int maximumPoolSize,
    @Nonnegative long keepAlive, @NonNull TimeUnit unit, @NonNull String baseName) {
    this(minimumPoolSize, maximumPoolSize, keepAlive, unit, baseName, new ExecutionQueue());
  }

  /**
   * Construct an executor with a specified pool size of threads that never die.
   * 
   * @param maximumPoolSize
   *          the maximum number of live threads.
   * @param baseName
   *          The base name for the threads built by this executor
   */
  public ReentrantExecutor(@Nonnegative int maximumPoolSize, @NonNull String baseName) {
    this(0, maximumPoolSize, Long.MAX_VALUE, TimeUnit.NANOSECONDS, baseName, new ExecutionQueue());
  }

  /**
   * Executes the task in the current thread if the current thread is one owned by the executor,
   * otherwise the command is scheduled it to be executed in the executor normally.
   * 
   * @param command
   *          the {@link Runnable} to execute.
   */
  @Override
  public final void execute(Runnable command) {
    if (currentThread().getClass() == Reentrant.class) {
      // This reentrance is necessary to avoid thread exhaustion deadlock. If we don't do this, a
      // command could decide
      // to execute recursively through the executor, and if the recursion is deeper than the
      // available number of
      // thread, the call will never get to an end.
      command.run();
    } else {
      super.execute(command);
    }
  }
  
  @Override
  public boolean cancel(Runnable task) {
    return getQueue().remove(task);
  }

  @Override
  public List<Runnable> cancelPendings() {
    List<Runnable> cancelled = new ArrayList<Runnable>();
    getQueue().drainTo(cancelled);
    return cancelled;
  }

  @Override
  public void shutdown() {
    cancelPendings();
    super.shutdownNow();
  }

  /**
   * A factory for reentrant threads
   */
  private static final class ReentrantThreadFactory implements ThreadFactory {
    /**
     * The currently building thread.
     */
    private final AtomicInteger threadCount = new AtomicInteger();

    /**
     * The base name for the threads built by this executor.
     */
    @NonNull
    private final String baseName;

    ReentrantThreadFactory(String baseName) {
      this.baseName = baseName;
    }

    @Override
    public Thread newThread(Runnable r) {
      return new Reentrant(r, baseName + threadCount.getAndIncrement());
    }
  }

  /**
   * Marker class to identify a thread as created by this executor and that is has to be used in a
   * reentrant fashion.
   */
  private static final class Reentrant extends Thread {
    Reentrant(Runnable target, String name) {
      super(target, name);
      setDaemon(true);
    }
  }
}