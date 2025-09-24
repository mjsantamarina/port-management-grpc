package ar.edu.itba.pod.g10.server.models;

import ar.edu.itba.pod.grpc.utils.Size;

import java.util.Deque;

public class Ship {
    private String id;
    private Size size;
    private Deque<String> containers;
    private int positionInQueue;

    public Ship(String id, Size size, Deque<String> containers) {
        this.id = id;
        this.size = size;
        this.positionInQueue = -1; // not in queue initially
        this.containers = containers;

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
}
