# soabase-stages

[![Build Status](https://travis-ci.org/soabase/soabase-stages.svg?branch=master)](https://travis-ci.org/soabase/soabase-stages)

A facade that makes staged/pipelined CompletableFutures much easier to create and manage

## Use Cases

- You have a sequence of tasks that pipeline or chain
- These tasks can be executed **synchronously** or **asynchronously**
- You might need to **abort/canel** the chain in the middle
- You might need to provide a **timeout** for the tasks

Most of this can be done with Java 8's CompletableFuture/CompletionStage today. Timeouts
must be added manually (or wait for Java 9). Aborting tasks is not supported. Also, the
CompletableFuture/CompletionStage API is awkward and difficult to use.

## StagedFuture

`StagedFuture` simplifies CompletableFuture/CompletionStage so that you can write code like this:

```java
StagedFuture.async(executor)
    .then(() -> queryDatabaseFor("something")).withTimeout(Duration.ofSeconds(25))
    .thenIf(record -> applyRecord(record)) // chain aborts if no record found
    .whenComplete(result -> handleResult(result))
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

