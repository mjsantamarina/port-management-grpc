package ar.edu.itba.pod.g10.server.locks;

import java.util.concurrent.locks.ReentrantLock;

public class Locks {
    public static final ReentrantLock DOCK_LOCK = new ReentrantLock();
    public static final ReentrantLock SHIP_REPOSITORY_LOCK = new ReentrantLock();
    public static final ReentrantLock SHIP_COUNT_LOCK = new ReentrantLock();
}
