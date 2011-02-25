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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A synchronous queue with a buffer queue used to keep rejected task to reschedule when threads
 * free up.
 * <p>
 * Buffered tasks are polled before the task in the underling queue so that scheduling sequence is
 * maintained, even if that is not strictly necessary considering the asynchronous nature of the
 * result.
 * </p>
 * 
 * @author Alex Nistico &lt;alienisty(at)gmail.com&gt;
 */
class ExecutionQueue extends SynchronousQueue<Runnable> implements RejectedExecutionHandler {

  private static final long serialVersionUID = 1L;

  /**
   * The buffer queue.
   */
  @NonNull
  private final Queue<Runnable> bufferQueue = new ConcurrentLinkedQueue<Runnable>();

  @Override
  public void clear() {
    super.clear();
    bufferQueue.clear();
  }

  @Override
  public Runnable poll(long timeout, TimeUnit unit) throws InterruptedException {
    Runnable run;
    // Note the buffer either has or not a task, so we don't want to wait on the buffer
    if ((run = bufferQueue.poll()) == null) {
      return super.poll(timeout, unit);
    }
    return run;
  }

  @Override
  public Runnable poll() {
    Runnable run;
    if ((run = bufferQueue.poll()) == null) {
      return super.poll();
    }
    return run;
  }

  @Override
  public Runnable take() throws InterruptedException {
    Runnable run;
    if ((run = bufferQueue.poll()) == null) {
      return super.take();
    }
    return run;
  }

  @Override
  public void rejectedExecution(Runnable run, ThreadPoolExecutor executor) {
    if (!bufferQueue.offer(run)) {
      throw new RejectedExecutionException();
    }
  }
}