package il.co.lird.FS133.Projects.WaitablePQueue;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;


public class WaitablePQueue<T> {

    Semaphore sem = new Semaphore(0);
    private final ReentrantLock lock = new ReentrantLock();
    private PriorityQueue<T> queue;

    public WaitablePQueue(Comparator<? super T> comparator) {
        this.queue = new PriorityQueue<>(comparator);
    }

    public WaitablePQueue() {
        this.queue = new PriorityQueue<>();
    }

    public void enqueue(T element) {

        lock.lock();

        queue.add(element);

        lock.unlock();

        sem.release();

    }

    public T dequeue() {

        T return_value;

        try {
            sem.acquire();

            lock.lock();

            return_value = queue.poll();

            lock.unlock();

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return return_value;
    }

    //timeout in milliseconds
    public T dequeue(int timeout) throws TimeoutException {

        T return_value;
        boolean time_sem;
        try {
            time_sem = sem.tryAcquire(timeout, TimeUnit.MILLISECONDS);

            if (!time_sem) {
                throw new TimeoutException();
            }

            lock.lock();

            return_value = queue.poll();

            lock.unlock();

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return return_value;
    }

    public boolean remove(T element) {

        boolean has_found = false;

        if (sem.tryAcquire()) {
            lock.lock();


            has_found = queue.remove(element);

            if (!has_found) {
                sem.release();
            }

            lock.unlock();
        }


        return has_found;
    }

    public boolean isEmpty() {

        return queue.isEmpty();
    }
}