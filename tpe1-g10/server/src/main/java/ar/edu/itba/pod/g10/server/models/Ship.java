package ar.edu.itba.pod.g10.server.models;

import ar.edu.itba.pod.grpc.utils.Size;

import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;

public class Ship implements Comparable<Ship> {
    private String id;
    private int shipNumber;
    private static final AtomicInteger counter = new AtomicInteger(0);;
    private Size size;
    private Deque<String> containers;
    boolean unloaded;
    private int positionInQueue;

    public Ship(String id, Size size, Deque<String> containers) {
        this.id = id;
        this.size = size;
        this.positionInQueue = -1; // not in queue initially
        this.containers = containers;
        this.shipNumber = counter.incrementAndGet();
        this.unloaded = false;
    }


    public String peekContainer() {
        return containers.peekFirst();
    }


    public boolean hasContainers() {
        return !containers.isEmpty();
    }

    public String unloadContainer() {
        if (!containers.isEmpty()) {
            return containers.pollFirst();
        }
        return null;
    }

    public boolean getUnloaded() {
        return unloaded;
    }

    public String getId() {
        return id;
    }
    public Size getSize() {
        return size;
    }
    public int getContainerAmount() {
        return containers.size();
    }




    public void setPositionInQueue(int positionInQueue) {
        this.positionInQueue = positionInQueue;
    }

    public void setUnloaded() {
        this.unloaded = true;
    }

    @Override
    public int compareTo(Ship o) {
        return shipNumber - o.shipNumber;
    }
}
