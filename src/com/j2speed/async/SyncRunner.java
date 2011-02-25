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

import static java.util.Collections.emptyList;

import java.util.List;

/**
 * An "asynchronous" runner that uses the calling thread to execute the asyncs. This runner is
 * useful to build synchronous version of an asynchronous API.
 * 
 * @author Alex Nistico &lt;alienisty(at)gmail.com&gt;
 */
public final class SyncRunner implements AsyncRunner {

  private static final SyncRunner RUNNER = new SyncRunner();

  private static final AsyncRunner DELEGATE = new DefaultRunner(new AsyncExecutor() {
    @Override
    public void execute(Runnable command) {
      // use current thread
      command.run();
    }

    @Override
    public void shutdown() {
      // nothing to shutdown
    }
    
    public boolean cancel(Runnable task) {
      // this executor never enqueues tasks
      return false;
    }

    @Override
    public List<Runnable> cancelPendings() {
      // nothing to cancel
      return emptyList();
    }
  });

  private SyncRunner() {
    // we only need one instance
  }

  /**
   * Returns the {@link SyncRunner} instance.
   * 
   * @return the singleton instance of {@link SyncRunner}.
   */
  public static final SyncRunner get() {
    return RUNNER;
  }

  @Override
  public final AsyncRun start(Async async, CompletionHandler... handlers) {
    return DELEGATE.start(async, handlers);
  }

  @Override
  public final <T> AsyncRun start(Request<T> request, ResponseHandler<T>... handlers) {
    return DELEGATE.start(request, handlers);
  }

  @Override
  public final <T> AsyncRun start(Progressive<T> request, ProgressHandler<T>... handlers) {
    return DELEGATE.start(request, handlers);
  }

  @Override
  public final void cancelPendings() {
    DELEGATE.cancelPendings();
  }
}
