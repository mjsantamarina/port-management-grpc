package ar.edu.itba.pod.g10.server.locks;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Locks {
    public static final ReentrantLock DOCK_LOCK = new ReentrantLock();
    public static final ReentrantLock SHIP_REPOSITORY_LOCK = new ReentrantLock();
    public static final ReentrantLock SHIP_COUNT_LOCK = new ReentrantLock();
    public static final Condition CHANGED = SHIP_REPOSITORY_LOCK.newCondition();
    public static final ReentrantLock NOTIFICATIONS_LOCK = new ReentrantLock();
    public static final Condition NOTIFIABLE_CHANGED = NOTIFICATIONS_LOCK.newCondition();

    private static final ConcurrentHashMap<Integer, AtomicBoolean> CRANE_IN_USE = new ConcurrentHashMap<>();

    public static boolean tryAcquireCrane(int craneId) {
        return CRANE_IN_USE.computeIfAbsent(craneId, k -> new AtomicBoolean(false))
                .compareAndSet(false, true);
    }
    public static void releaseCrane(int craneId) {
        AtomicBoolean b = CRANE_IN_USE.get(craneId);
        if (b != null) b.set(false);
    }
}
