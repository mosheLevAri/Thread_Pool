
/*
* ThreadPool is a specific implementation of an Executor that maintains a pool of worker threads.
* It allows for the execution of multiple threads of code concurrently,
* by maintaining a pool of worker threads. Thread pool reuse existing threads instead of creating
*  new threads every time a task needs to be executed,
* which can improve performance and reduce resource consumption.
* */

package il.co.lird.FS133.Projects.ThreadPool;

import il.co.lird.FS133.Projects.ThreadPool.WaitablePQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadPool<V> implements Executor {

    private List<Thread> threadList;
    private WaitablePQueue<Task> taskPQ;
    private boolean isShutdown; /* (for throwing exception in submit) */
    private boolean isPaused;
    private AtomicInteger numberOfThreads;
    private final Object lock;

    /*-------------------------------------------------------------------------*/

    public ThreadPool(int numberOfThreads) {

        threadList = new ArrayList<>(numberOfThreads);
        taskPQ = new WaitablePQueue<>();
        lock = new Object();
        this.numberOfThreads = new AtomicInteger(numberOfThreads);

        initThreadPool();
    }

    @Override
    public void execute(Runnable runnable) {
        submit(runnable, Priority.MEDIUM);
    }

    //this submit convert Runnable to Callable
    public Future<Void> submit(Runnable runnable, Priority priority) {

        Callable<Void> new_callable = () -> {
            runnable.run();
            return null;
        };

        return submit(new_callable, priority);
    }

    //this submit convert Runnable to Callable
    public <V> Future<V> submit(Runnable runnable, Priority priority, V value) {

        Callable<V> new_callable = () -> {
            V val = value;
            runnable.run();
            return val;
        };

        return submit(new_callable, priority);
    }

    public <V> Future<V> submit(Callable<V> callable) {

        return submit(callable, Priority.MEDIUM);
    }

    public <V> Future<V> submit(Callable<V> callable, Priority priority) {

        if (isShutdown) {
            throw new RejectedExecutionException("thread pool is Shutdown");
        }

        if (isPaused) {
            throw new IllegalStateException("thread pool is paused");
        }

        Task<V> task = new Task<>(callable, priority.getValue());

        taskPQ.enqueue(task);

        return task.getFuture();
    }


    public void setNumOfThreads(int numThreads) {

        if (numThreads > numberOfThreads.get()) {
            WorkerThread new_worker_thread = null;

            for (int i = numberOfThreads.get(); i < numThreads; ++i) {
                new_worker_thread = new WorkerThread();
                threadList.add(new_worker_thread);
                new_worker_thread.start();
            }
        } else {
            for (int i = numberOfThreads.get(); i > numThreads; --i) {
                taskPQ.enqueue(new Task<>(suicideMission(), 4));
            }
        }

        numberOfThreads.set(numThreads);

    }

    public void pause() {

        if (isShutdown) {
            throw new RejectedExecutionException("thread pool is Shutdown");
        }

        if (isPaused) {
            throw new IllegalStateException("thread pool is already paused");
        }

        this.isPaused = true;
    }

    public void resume() {

        if (isShutdown) {
            throw new RejectedExecutionException("thread pool is Shutdown");
        }

        if (!isPaused) {
            throw new IllegalStateException("thread pool is NOT paused");
        }


        this.isPaused = false;

        synchronized (lock) {
            this.lock.notifyAll();
        }

    }

    public boolean isShutdown() {
        return isShutdown;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void shutdown() {

        isShutdown = true;
        if (isPaused) {
            resume();
        }

        Task<V> task = new Task<>(suicideMission(), 0);

        for (int i = 0; i < threadList.size(); ++i)
        {

            taskPQ.enqueue(task);
        }

    }

    public void awaitTermination() throws InterruptedException {

        if (!isShutdown) {
            throw new InterruptedException();
        }


        for (int i = 0; i < numberOfThreads.get(); ++i)
        {
            try {
                threadList.get(i).join();
            } catch (InterruptedException e) {
                // continue waiting for thread to terminate
            }
        }

    }

    private class Task<V> implements Comparable<Task> {

        private Callable<V> callable = null;
        private Future<V> future = null;
        private int priority = 0;

        private Task(Callable<V> callable, int priority) {
            this.callable = callable;
            this.priority = priority;
            this.future = new Taskfuture(this);
        }

        @Override
        public int compareTo(Task task) {

            return task.getPriority() - this.priority;
        }

        private Future<V> getFuture() {

            return this.future;
        }

        private Callable<V> getCallable() {

            return this.callable;
        }

        public int getPriority() {
            return priority;
        }

        private class Taskfuture implements Future<V> {

            V result = null;
            boolean isCancelled = false;
            boolean isDone = false;
            private final Semaphore sem;
            private Task current_task = null;

            Taskfuture(Task current_task) {
                this.current_task = current_task;
                sem = new Semaphore(0);
            }

            @Override
            public boolean cancel(boolean b) {

                if (taskPQ.remove(current_task)) {
                    isCancelled = true;
                    sem.release();
                }

                return isCancelled;
            }

            @Override
            public boolean isCancelled() {
                return isCancelled;
            }

            @Override
            public boolean isDone() {
                return isDone;
            }

            @Override
            public V get() throws InterruptedException, ExecutionException {

                sem.acquire();
                return result;
            }

            @Override
            public V get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {

                if (!sem.tryAcquire(l, timeUnit)) {
                    throw new TimeoutException();
                }

                return result;
            }

            private void setResult(V result) {
                this.result = result;
            }

            private void setDone(boolean done) {
                isDone = done;

                sem.release();

            }
        }
    }

    /*-----------------------------------------------------------------------------*/

    private class WorkerThread extends Thread {

        V result = null;
        boolean isRunning = true;
        private Callable callable = null;
        private Future future = null;
        private Lock obj = new ReentrantLock();

        @Override
        public void run() {

            while (isRunning) {

                Task task = taskPQ.dequeue();

                try {
                    result = (V) task.getCallable().call();
                }catch (Exception e) {
                    throw new RuntimeException(e);
                }

                future = task.getFuture();
                ((Task.Taskfuture) future).setDone(true);
                ((Task.Taskfuture) future).setResult(result);

                if (isPaused) {

                    synchronized (lock) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }

            obj.lock();
            threadList.remove(this);
            obj.unlock();

            numberOfThreads.getAndDecrement();
        }

        public void setRunning(boolean running) {
            isRunning = running;
        }
    }

    public int getNumberOfThreads() {
        return numberOfThreads.get();
    }


    private Callable suicideMission() {
        Callable new_callable = new Callable() {
            @Override
            public Object call() throws Exception {

                ((WorkerThread) Thread.currentThread()).setRunning(false);
                return null;
            }
        };

        return new_callable;
    }

    private void initThreadPool() {
        WorkerThread new_worker_thread = null;

        for (int i = 0; i < numberOfThreads.get(); ++i) {
            new_worker_thread = new WorkerThread();
            threadList.add(new_worker_thread);
            new_worker_thread.start();
        }
    }
}



