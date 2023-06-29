# Thread Pool

ThreadPool is a specific implementation of an Executor that maintains a pool of worker threads. It allows for the execution of multiple threads of code concurrently by maintaining a pool of worker threads.
Thread pool reuses existing threads instead of creating new threads every time a task needs to be executed, which can improve performance and reduce resource consumption.


## Installation
To use the Thread Pool code, follow these steps:

1. Clone the repository or download the source code.
2. Ensure you have Java Development Kit (JDK) installed on your system.
3. Import the code into your Java project.

## Usage
To use the Thread Pool in your application, follow these steps:

1. Create an instance of the ThreadPool class, providing the desired number of threads.

``` java
ThreadPool<ReturnType> threadPool = new ThreadPool<>(numberOfThreads);
```

2. Implement the Runnable or Callable interface for the task you want to execute concurrently.

``` java
Runnable task = () -> {
    // Task implementation
};

Callable<ReturnType> task = () -> {
    // Task implementation
    return result;
};
```

3. Submit the task to the thread pool for execution.

``` java
Future<ReturnType> future = threadPool.submit(task);
```

4. Optionally, you can set the priority of the task using the Priority enum.

``` java
Future<ReturnType> future = threadPool.submit(task, Priority.MEDIUM);
```

If needed, you can pause and resume the thread pool using the pause() and resume() methods.

``` java
threadPool.pause();

// Perform any desired operations while the thread pool is paused

threadPool.resume();
```

6. To gracefully shut down the thread pool, call the shutdown() method.

``` java
threadPool.shutdown();
```
