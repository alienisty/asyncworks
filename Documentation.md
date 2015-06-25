

# Introduction #

To define what an asynchronous computation is, we need do describe what a synchronous one is.
A synchronous computation is started and performed by the same thread. For example, when you
invoke a method, the code in the method is executed before the next instruction after the invocation.
An asynchronous computation, instead, executes independently from the starting thread. If we
consider the method invocation example, invoking a method asynchronously means that it may
still be running by the time the next instruction after the invocation is executed.

While the synchronous model is quite intuitive, the asynchronous one introduces some complexity,
mainly in exchanging computation results. In fact, when you normally invoke a method that return
a value, you expect that the value is there by the time the method returns. This is not true in
the asynchronous model, and by the time the invocation returns, the value could be a long way
before being ready.

There are mainly two solution to this problem:
  1. return the value as a “promise” to provide the value sooner or later
  1. notify the caller when the computation is done and when the value, if any, is available

The first approach allows the caller to poll the promise to see if the computation is done and
get the value when it is ready or just do some more work and then wait until the promise provide
a value.

With the second approach, the caller needs to subscribe to receive the notification and specify
what to do with it.

Frameworks to handle asynchronous computations already exist in the Java world, and probably
the most notorious are:
  * From the java.concurrent package:
    * ExecutorService with Future
    * CompletionService

  * From Swing:
    * SwingWorker

Unfortunately, these frameworks offer only a limited number of aspects of asynchronous execution
patterns.

The goal of asyncworks is to implement an event-based asynchronous pattern, where interactions
with the asynchronous computation are implemented through call-backs, rather than with possibly
blocking methods.

# The Framework #

## `AsyncRunner` ##

This is the central abstraction that allows to start asynchronous computations.

## `Async` ##

This interface models a "call and forget" invocation pattern. The supported (optional) call-backs
for this type of computation are for exceptions and completion notifications.

## `Request` ##

This interface models a "request and notify" invocation pattern. The computation is expected to
produce a result that is then returned to the invoker through a (required) call-back.
Other supported call-backs are for exceptions notifications.

## `Progressive` ##

This interface models a computation that produce a result incrementally. The increments are notified
through a call-back. Other supported call-backs are for exceptions and completion notifications.

## `ExceptionHandler` ##

This is the root abstraction for all handlers. An handler provides call-backs to cooperate with
asynchronous computations.

As its name suggests, this interface provides call-backs to handle exceptions.

## `CompletionHandler` ##

Other than being an `ExceptionHandler`, this allows to receive a notification when an `Async`
completes normally, that is without throwing an exception.

## `ResponseHandler` ##

Other than being an `ExceptionHandler`, this allows to receive the result when a `Request` completes
normally, that is without throwing an exception.

## `ProgressHandler` ##

Other than being an `CompletionHandler`, this allows to receive the increments while a `Progressive`
is running.

Note that if an exception arises during the execution, the handler could have already been notified
with some increments.

## `AsyncRun` ##

Allows to query and control the execution of an asynchronous computation. An instance of this
interface is returned by any `start` method provided by the `AsyncRunner`.

## `State` ##

Allows an asynchronous computation to query the status of the execution, currently the only state
the can be queried is if the computation has been cancelled by some other thread.
As stated in the javadoc, implementations of asynchronous computations should query the state in a
fashion that would allow a prompt interruption, in case the computation is cancelled.

## `ProgressState` ##

Extends `State` functionalities for the use with `Progressive`, and it allows notifying progresses to
the registered `ProgressHandler`s.

# How does it work #

Now that you have been introduced to the core abstractions of the framework, let's see how it all
fits together.

To start any computation, we need an `AsyncRunner` instance. You usually have two choices:
  1. Write your own
  1. Use `DefaultRunner` (part of the framework)

In the rest of this document, in all the examples, we will imply the use of an instance of a
`DefaultRunner` named `runner`.

Note that `AsyncRunner`'s `start` methods all use varargs for the call-backs handler, what that
means is:
  1. Handlers are optional but (except for `Async`) if no handler is specified, the actual asynchronous
> > computation needs to be an instance of its handler (by implementing the relative interface or by
> > using the `AsyncCompletion` adapter).
  1. Handlers can be multiple per computation.

## More on `ExceptionHandler` ##

Before showing how to use the different type of computations, it is important to understand how an
`ExceptionHandler` works.

The interface provides the following two methods:
```
public void onException(Throwable exception);

public void onFinally();
```

Conceptually the flow is the following:
```
try {
  computation();
} catch(Throwable th) {
  onException(th);
} finally {
  onFinally();
}
```
Easy right? Not so fast! Remember that we are fragmenting the computation over multiple threads, and
what that means, is that the only _happens-before_ relationship is between `computation()` and either
of the handling methods, but nothing is guaranteed about the ordering in the execution of
`onException()` and `onFinally()`; in fact, the latter _**CAN**_ execute before the former in
certain situations, for example when we transfer the execution of a call-back to some other thread
(see [Transferring Callback Handling to Specific Threads](Documentation#Transferring_Callback_Handling_to_Specific_Threads.md)).

Now, you might ask: "What if I want to handle different type of exceptions in different way?" In
other words, what if you wanted something similar to a `catch(SpecificException)` block? Well one
way of doing it is to put a chain of
```
if (th instanceof SomeException) ... 
else...
```
cast and do your thing.

I can see most of you already puking from disgust, but wait! I'm with you! And that's why the
"Overloading Aware Handling" it's been devised. What that gives you, is the ability to overload
the `onException` method with a specific exception type, and the overloading method will be invoked
if the specific exception, or one its subclasses, is actually thrown by the computation.

For example if you wanted to specifically handle `IOException`s you would define the following
method in your implementation:
```
public void onException(IOException e){
  // do your handling
}
```
What happens then, is (conceptually):
```
try {
  computation();
} catch (IOException e) {
  onException(e); // the overloaded method
} catch(Throwable th) {
  onException(th); // the base method, not invoked if the specific exception is caught.
} finally {
  onFinally();
}
```

Note that the overloading method _**MUST**_ be public.

Note, also, that `onFinally()` is always invoked, even when the computation is cancelled or did not
start yet.

Let's see now how can we start asynchronous computations.

## How to use `Async` ##

This is the simplest type, it is similar to the use `ExecutorService.submit(Runnable)` from the
`java.concurrent` package, the only difference is that you don't have a way to block the current
thread until the computation is complete, like you would be able to do with `Future.get()`.
The main idea is: if you want to "synchronize" with asynchronous computations, you need to set up
call-backs. In fact, if you are willing to wait, there is no point in starting an asynchronous call!

The basic usage is as follow:
```
AsyncRun run = runner.start(new Async() {
  @Override
  public void run(State state) {
    // do something
    state.checkCancelled();
    // do more
  }
});
```

To be notified about completion, we have two ways. The first is:
```
runner.start(new AsyncCompletion() {
  @Override
  public void run(State state) {
    // do something
    state.checkCancelled();
    // do more
  }

  @Override
  public void onComplete() {
    // at this point run() has returned normally and it hasn't been cancelled.
  }  
});
```
Where the `Async` acts as a `CompletionHandler` as well.

The second is to use a separate `CompletionHandler`:
```
runner.start(new Async() {
  @Override
  public void run(State state) {
    // do something
    state.checkCancelled();
    // do more
  }
}, new CompletionAdapter() {
  @Override
  public void onComplete() {
    // at this point run() has returned normally and it hasn't been cancelled.
  }  
});
```

The second form, and in general all the variants that use a separate handler, are useful mainly
when creating asynchronous APIs, but that's a topic for another chapter.

For brevity, there are no exception handling, but remember that `AyncCompletion` and `CompletionHandler`
are `ExceptionHandler`s and you can add exception handling if necessary.

You normally want to use an adapter if are not going to provide an implementation for all the
call-backs. The framework provides adapters for all type of handlers.

Note that we could have specified multiple `CompletionHandler`s and, if the `Async` acts as a
`CompletionHandler`, it will also be notified.

## How to use `Request` ##

`Request`s are asynchronous computations that produce a result. The generated result is then notified
to the registered `ResponseHandler`.

Here it is an example:
```
runner.start(new AsyncResponse<String>() {
  @Override
  public String run(State state) {
    // a very complex string to build
    return result;
  }
  @Override
  public void onResponse(Result<String> result) {
    String theString = result.get();
    // do something with the string.
  }
});
```

The same example with a separate handler is:
```
runner.start(new Request<String>() {
  @Override
  public String run(State state) {
    // a very complex string to build
    return result;
  }
}, new ResponseAdapter<String>() {
  @Override
  public void onResponse(Result<String> result) {
    String theString = result.get();
    // do something with the string.
  }
});
```

The same notes about multiple handlers and exception handling apply to `Request`s as well.

## How to use `Progressive` ##

A `Progressive` is similar to a `Request`, except that the result, in general, is incrementally
produced and notified to the `ProgressHandler`.
When the progressive is done generating the result, the `ProgressHandler`, that is also a
`CompletionHandler`, will receive a call-back on the `onComplete()` method.

Here it is an example:
```
runner.start(new AsyncProgress<String>() {
  @Override
  public void run(ProgressState<String> state) {
    while(!done) {
      state.checkCancelled();
      String nextValue = buildNextValue();
      state.notifyProgress(nextValue);
    }
  }
  @Override
  public void onProgress(Progress<T> progress) {
    String newValue = progress.get();
    // handle the current value
  }
  @Override
  public void onComplete() {
    // At this stage the entire result has been notified.
    // Do any action necessary to consolidate
  }
});
```

The same notes about multiple handlers and exception handling apply to `Progressive`s as well.

## How to cancel a computation ##

There are two ways you can cancel an asynchronous computation: one is through the `cancel()` method
of the `AysncRun` instance received when starting an asynchronous computation.

The other is only applicable for `Progressive`. The `ProgressHandler.onProgress()` method receives
an instance of `Progress` that, other than allowing to retrieve the current progress value, it also
allows to `cancel()` the computation from inside the handling. A use case for this type of cancelling
could be when the maximum allowed number of results have been received and there is no point in
continuing generating more.

When an async is cancelled and the computation never started, the relative `AsyncRun` will always
return the following:
  * isStarted()->false
  * isCancelled()->true
  * isDone()->true

If the computation started, things get a bit more complicated, because `isCancelled()` and `isDone()`
will, in general, return a value that depends on the actual current state for the computation.

The more confusing is, possibly, `isCancelled()`; in fact, this method could return `false` even if
the computation has been cancelled and the execution completed. To understand why that is the case,
we need to explain what the cancellation protocol is.

When you call `cancel()`, a cancellation request is made, but if the computation does not go through
specified check-points, the request is not noted and the computation will just execute until its
natural end.

This check-points are the method `State.checkCancelled()` and any interruptible method. What this
means is that the framework leaves some responsibility to you for what concerns cancellation. If you
have experience with thread programming, you already know the principles and in asyncworks the
principles simplify a bit more: call `State.checkCancelled()` accordingly to allow a prompt
cancellation.

As a rule of thumbs, you should call `State.checkCancelled()`:
  1. As the first instruction of any loop
  1. Before the invocation of a potentially long running methods, unless they are interruptible.

Following these rules will increase the probability of meeting with a cancellation requests and
therefore ending the computation close to the request time.

Note that `State.checkCancelled()` will throw a `CancelledException`, if a cancellation request has
been made, causing the computation to exit immediately. Note, though that your registered
`ExceptionHanlders` won't be notified of such exception, because it is considered internal. Note, also,
that you should not catch the same exception during you computation, because that would defeat its
very purpose.

## Transferring Callback Handling to Specific Threads ##

In some situations, it is necessary to handle call-backs in specific threads, most notably, you want
to handle a call-back on the EDT (AWT Event Dispatch Thread) if you are going to update models, UI
component or firing events.

For example, let's say that you want to set the text retrieved from somewhere in a text component,
you could do it the classic way:
```
runner.start(new AyncRequest<String>() {
  @Override
  public String run() {
    String text;
    // call a very slow web service to get the text
    return text;
  }
  
  @Override
  public void onResponse(final Result<String> result) {
    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        myTextComponent.setText(result.get());
      }
    });
  }
}
```
Obviously that works, but there is quite a bit of boiler plate code. What about this:
```
runner.start(new AyncRequest<String>() {
  @Override
  public String run() {
    String text;
    // call a very slow web service to get the text
    return text;
  }
  
  @Override
  @RunOn(Edt.class)
  public void onResponse(Result<String> result) {
    myTextComponent.setText(result.get());
  }
}
```
Quite a bit neater don't you think?

The `@RunOn` annotation is supported on all methods specified in the handlers interfaces, even on
overloaded exception handling callback methods.

This annotation establish a contract with the class specified as its value, and that is that the
specified class _**MUST**_ define a static method with the following signature:
```
public static void transfer(Runnable runnable)
```
The reason for this contract is that there are probably other use cases that we cannot foresee in
general, so asyncworks allows you to specify your on "transferer" when you need it. `Edt` is the one
predefined with the framework.