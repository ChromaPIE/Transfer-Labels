package com.buuz135.transfer_labels.network;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Netty handlers run off-thread in 1.7.10; packets queue work here to be drained on the main threads. */
public class Tasks {

    public static final Queue<Runnable> CLIENT = new ConcurrentLinkedQueue<>();
    public static final Queue<Runnable> SERVER = new ConcurrentLinkedQueue<>();

    public static void drain(Queue<Runnable> queue) {
        Runnable task;
        while ((task = queue.poll()) != null) {
            task.run();
        }
    }
}
