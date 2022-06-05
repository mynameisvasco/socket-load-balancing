package shared;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Fifo<T> {
    private final int size;
    private final Lock lock = new ReentrantLock();
    private final Condition isEmptyCondition = lock.newCondition();
    private final LinkedList<T> list = new LinkedList<>();

    public Fifo(int size) {
        this.size = size;
    }

    public Fifo() {
        this.size = -1;
    }

    public boolean enqueue(T item) {
        try {
            lock.lock();

            if (list.size() == size) {
                return false;
            }

            list.add(item);
            isEmptyCondition.signal();
            return true;
        } finally {
            lock.unlock();
        }
    }

    public T dequeue() {
        try {
            lock.lock();

            while (list.size() == 0) {
                try {
                    isEmptyCondition.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            var item = list.pop();
            return item;
        } finally {
            lock.unlock();
        }
    }
}