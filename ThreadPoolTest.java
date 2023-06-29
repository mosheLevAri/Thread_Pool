package il.co.lird.FS133.Projects.ThreadPool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.*;

class ThreadPoolTest {

    private ThreadPool<Void> threadPool;

    @BeforeEach
    void setUp() {
        threadPool = new ThreadPool<>(5);
    }

    @Test
    void execute() {
        threadPool.execute(() -> System.out.println("Hello from thread pool!"));
    }



    Callable<Integer> callable = new Callable()
    {
        @Override
        public Object call() throws Exception {
            System.out.println("(MEDIUM priority )the thread is: " + Thread.currentThread().getName());

            sleep(1000);
            return 5;
        }
    };

    Callable<Integer> callable2 = new Callable()
    {
        @Override
        public Object call() throws Exception {
            System.out.println("(HIGH priority )the thread is: " + Thread.currentThread().getName());
            sleep(1000);

            return 5;
        }
    };

    @Test
    void submit() throws ExecutionException, InterruptedException {

        Future<Integer> future = null;
        Integer res = 0;
        
        for(int i = 0; i < 10 ; ++i)
        {
            threadPool.submit(callable2);
        /*    threadPool.submit(callable);*/
        }

        System.out.println("\n");
        sleep(5000);
        threadPool.setNumOfThreads(10);

        for(int i = 0; i < 20 ; ++i)
        {
            threadPool.submit(callable);
        }

        sleep(5000);

        System.out.println("set to 10");
        threadPool.setNumOfThreads(3);
        threadPool.setNumOfThreads(10);

        for(int i = 0; i < 20 ; ++i)
        {
            threadPool.submit(callable);
        }


        threadPool.shutdown();
        threadPool.awaitTermination();
        System.out.println("finish prog");


    }

    @Test
    void cancel() throws ExecutionException, InterruptedException {


        for(int i = 0; i < 5 ; ++i)
        {
            threadPool.submit(() ->
                    {
                        System.out.println("Hello world");
                        sleep(1000);
                        return null;
                    });
        }

        Future<String> future2 = threadPool.submit(() -> "im cencel method!");

        System.out.println("\nis canceled : " + future2.cancel(true));
        System.out.println("future2 get : " + future2.get());
        System.out.println("future2.isDone() : " + future2.isDone());
        System.out.println("future2.isCancelled() :" + future2.isCancelled());


        threadPool.shutdown();
        threadPool.awaitTermination();
    }

    @Test
    void testSubmit() throws ExecutionException, InterruptedException {
        Future<String> future = threadPool.submit(() -> "Hello from testSubmit!");
        assertEquals("Hello from testSubmit!", future.get());
    }

    @Test
    void testSubmit1() throws ExecutionException, InterruptedException {
        Future<Integer> future = threadPool.submit(() -> 2 + 2);
        assertEquals(4, future.get());
    }

    @Test
    void testSubmit2() throws ExecutionException, InterruptedException {
        Callable<String> callable = () -> {
            sleep(1000);
            return "Hello from testSubmit2!";
        };
        Future<String> future = threadPool.submit(callable);
        assertFalse(future.isDone()); // The task is still running
        sleep(1500);
        assertTrue(future.isDone()); // The task has finished
        assertEquals("Hello from testSubmit2!", future.get());
    }

    @Test
    void setNumOfThreads() {
/*        threadPool.setNumOfThreads(5);
        assertEquals(5, threadPool);*/
    }

    @Test
    void pause() throws InterruptedException {
        for(int i = 0; i < 10 ; ++i)
        {
            threadPool.submit(callable);
        }
        threadPool.pause();
        assertTrue(threadPool.isPaused());

        sleep(5000);

        threadPool.resume();

        sleep(5000);

    }

    @Test
    void resume() {

        threadPool.pause();
        threadPool.resume();
        assertFalse(threadPool.isPaused());
    }

    @Test
    void shutdown() throws InterruptedException {

        for(int i = 0; i < 10 ; ++i)
        {
            threadPool.submit(callable);
        }

        threadPool.shutdown();
        //assertTrue(threadPool.isShutdown());
        sleep(10000); // Wait for the threads to finish
        System.out.println("shut down finsh");
    }

    @Test
    void awaitTermination() throws InterruptedException {
        for(int i = 0; i < 10 ; ++i)
        {
            threadPool.submit(callable);
        }

        threadPool.shutdown();
        threadPool.awaitTermination(); // Wait for the threads to finish
    }
}