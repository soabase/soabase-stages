# soabase-stages

[![Build Status](https://travis-ci.org/soabase/soabase-stages.svg?branch=master)](https://travis-ci.org/soabase/soabase-stages)
[![Maven Central](https://img.shields.io/maven-central/v/io.soabase.stages/soabase-stages.svg)](http://search.maven.org/#search%7Cga%7C1%7Csoabase-stages)

A tiny library that makes staged/pipelined CompletableFutures much easier to create and manage.

## Use Cases

- You have a sequence of tasks that pipeline or chain
- These tasks can be executed **synchronously** or **asynchronously**
- You might need to **abort/cancel** the chain in the middle
- You might need to provide a **timeout** for the tasks

Most of this can be done with Java 8's CompletableFuture/CompletionStage today. Timeouts
must be added manually (or wait for Java 9). Aborting tasks is not supported. Also, the
CompletableFuture/CompletionStage API is awkward and difficult to use.

## StagedFuture

`StagedFuture` simplifies CompletableFuture/CompletionStage so that you can write code like this:

```java
StagedFuture.async(executor)
    .thenIf(() -> queryDatabaseFor("something"))
        .withTimeout(Duration.ofSeconds(25))
    .thenIf(record -> applyRecord(record)) // chain aborts if no record found
    .thenIf(result -> returnNextRecord(result))
    .whenSucceeded(nextResult -> handleResult(nextResult))
    .whenAborted(() -> handleAbort())
    .whenFailed(e -> handleFailure(e));
``` 

### Benefits Over Raw CompletableFuture/CompletionStage

- Can easily set timeouts and timeouts with default values for tasks (without waiting for Java 9)
- Allows setting the executor once instead of with each chained call
- Allows a task to signal that the remainder of the chain should be canceled
- Simplified API

Note: you can easily access the managed `CompletionStage` when needed by calling `StagedFuture#unwrap()`.

### Using Stages

Stages is available from [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Csoabase-stages). Use your favorite build tool and specify:

| GroupId | ArtifactId |
|---------|------------|
| io.soabase.stages | soabase-stages |

[Change Log](https://github.com/soabase/soabase-stages/blob/master/CHANGELOG.md)

#### Starting a chain

Similarly to the builders in `CompletableFuture` you start a chain using the builders in `StagedFuture`. There are syncrhonous and asynchronous builders:

- `StagedFuture.sync()` - starts a StagedFuture chain that executes tasks synchronously
- `StagedFuture.async(executor)` - starts a StagedFuture chain that executes tasks asynchronously using the given executor
- `StagedFuture.asyncPool()` - starts a StagedFuture chain that executes tasks asynchronously using the ForkJoin pool

#### Adding tasks to the chain

Tasks are added to the chain using one of the "thenIf" methods. The first task added is specified via a supplier and subsequent tasks are specified via functions that take the result of the previous task:

_Initial Task_

- `thenIf(Supplier<Optional<U>> proc)` - Execute the given task synchronously or asynchronously depending on how the StagedFuture was built. The given task returns an optional value that indicates whether or not the next stage can execute. If `Optional.empty()` is returned, the entire StagedFuture chain is considered to be aborted and no future tasks will execute. The `StagedFuture.whenAborted()` completer will get called.

_Subsequent Tasks_

- `thenIf(Function<T, Optional<U>> proc)` - If the chain has not been aborted or errored, the result of the current task is passed to this new task synchronously or asynchronously depending on how the StagedFuture was built. The given task returns an optional value that indicates whether or not the next stage can execute. If `Optional.empty()` is returned, the entire StagedFuture chain is considered to be aborted and no future tasks will execute. The `StagedFuture.whenAborted()` completer will get called.

_Timeouts_

The "then" methods (see above) can optional be assigned a timeout or a timeout and default value:

- `thenIf(X).withTimeout(Duration timeout)` - Sets a timeout for this stage's task. If the given timeout elapses before the task completes this stage is completed exceptionally with a `TimeoutException`.
- `thenIf(X).withTimeout(Duration timeout, Supplier<T> defaultValue)` - Sets a timeout for this stage's task. If the given timeout elapses before the task completes this stage is completed with the given default value.

_Completers_

At any point in the chain, you can add handlers for successful completions, failures or aborts:

- `whenSucceeded(Consumer<T> handler)` - if the chain completes successfully the handler is called.
- `whenSucceededYield(Function<T, U> handler)` - same as `whenSucceeded()` but allows mapping the return type.
- `whenAborted(Runnable handler)` - if the chain is aborted (i.e. one of the `thenIf()` tasks returns empty) the handler is called.
- `whenFailed(Consumer<Throwable> handler)` - if there is an exception or failure in the chain the handler is called.
- `whenFinal(Runnable handler)` - calls the handler when the chain completes in any way (success, abort, exception, etc.).

_Chaining Other Stages_

You can include external stages into the chain:

- `thenStageIf(Function<T, CompletionStage<Optional<U>>> stage)` - executes the given stage asynchronously as the next task in the chain. If the stage returns an empty Optional the chain is aborted.

_Access The Internal CompletionStage_

You can access the internally managed `CompletionStage` via:

- `unwrap()` - returns the `CompletionStage<Optional<T>>`.

### Tracing

The tasks submitted to StagedFuture can optionally be traced via the `Tracing` interface. The library comes with an SLF4J tracer and a `System.out` tracer. You can also write your own. Pass an instace of the tracer to the StagedFuture builder. E.g.

```java
StagedFuture.async(executor, Tracing.debug(logger)).
    then(...)
    ...
```

#### Cancelable Tracer

The special purpose tracer, `Cancelable`, can be used to enable canceling a running chain.
It keeps track of the threads in use by the StagedFuture it is associated with. At any time you can call cancelChain(boolean) to interrupt currently running tasks and prevent new tasks from running. E.g.

```java
Cancelable cancelable = new Cancelable();
StagedFuture.async(executor, cancelable)
    .thenIf(() -> worker("1"))
    .thenIf(s -> hangingWorker("2"))
    .thenIf(s -> worker("3"))
    .thenIf(s -> worker("4"));

cancelable.cancel(true);    // hangingWorker() gets interrupted 
```

### Manual Wrappers

The CompletionStage wrappers that StagedFuture uses internally can be used directly without having to use `StagedFuture`.

#### Timeout

The `Timeout` class has methods that wrap `CompletionStage` adding timeouts and timeouts with default values. It roughly emulates the forthcoming Java 9 timeout features for CompletableFuture.

#### Aborted

The `Aborted` class has methods that wrap `CompletionStage` and call given handlers when the stage completes with an empty `Optional`.
