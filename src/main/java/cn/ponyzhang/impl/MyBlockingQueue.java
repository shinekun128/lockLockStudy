package cn.ponyzhang.impl;

import java.security.acl.LastOwnerException;
import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class MyBlockingQueue<E> {
    private static final int DEFAULT_CAPACITY=10;
    private ReentrantLock lock = new ReentrantLock();
    private Condition notFull = lock.newCondition();
    private Condition notEmpty = lock.newCondition();
    private E[] elements;
    private int capacity;
    private int size;
    private int head;
    private int tail;
    public MyBlockingQueue(int capacity){
        if(capacity<0){
            throw new IndexOutOfBoundsException("index" + capacity);
        }
        this.capacity = capacity;
        head = 0;
        tail = 0;
        elements = (E[])new Object[capacity];
    }
    public MyBlockingQueue(){
        this(DEFAULT_CAPACITY);
    }
    public void put(E e) throws InterruptedException {
        lock.lock();
        try {
            while(size == capacity){
                notFull.await();
            }
            elements[tail] = e;
            ++size;
            ++tail;
            tail = tail == capacity ? 0 : tail;
        } finally {
            notEmpty.signalAll();
            lock.unlock();
        }
    }
    public E take() throws InterruptedException {
        lock.lock();
        E e;
        try {
            while(size == 0){
                notEmpty.await();
            }
            --size;
            e = elements[head++];
            head = head==capacity? 0 : head;
            return e;
        } finally {
            notFull.signalAll();
            lock.unlock();
        }
    }


    public static void main(String[] args) {
        MyBlockingQueue<Integer> blockingQueue = new MyBlockingQueue<>();
        Producer producer = new Producer(blockingQueue);
        Consumer consumer = new Consumer(blockingQueue);
        for(int i=0;i<5;i++){
            new Thread(producer).start();
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for(int i=0;i<5;i++){
            new Thread(consumer).start();
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        producer.shutDown();
        consumer.shutDown();
    }
}

class Consumer implements Runnable{
    private final MyBlockingQueue<Integer> blockingQueue ;
    private volatile boolean flag;
    public Consumer(MyBlockingQueue<Integer> blockingQueue){
        flag = false;
        this.blockingQueue = blockingQueue;
    }
    @Override
    public void run() {
        while(!flag){
            int info;
            try {
                info = blockingQueue.take();
                System.out.println(Thread.currentThread().getName() + " consumer " + info);
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    public void shutDown(){
        flag =true;
    }
}

class Producer implements Runnable{
    private final MyBlockingQueue<Integer> blockingQueue;
    private volatile boolean flag;
    private Random random;

    public Producer(MyBlockingQueue<Integer> blockingQueue){
        this.blockingQueue = blockingQueue;
        flag = false;
        random = new Random();
    }

    @Override
    public void run() {
        while(!flag){
            try {
                int i = random.nextInt(100);
                blockingQueue.put(i);
                System.out.println(Thread.currentThread().getName() + " producer " + i);
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    public void shutDown(){
        flag =true;
    }
}
