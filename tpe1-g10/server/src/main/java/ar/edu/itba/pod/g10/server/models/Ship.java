package ar.edu.itba.pod.g10.server.models;

import ar.edu.itba.pod.grpc.utils.Size;

public class Ship {
    private String id;
    private Size size;
    private int containerAmount;
    private int positionInQueue;

    public Ship(String id, Size size, int containerAmount) {
        this.id = id;
        this.size = size;
        this.containerAmount = containerAmount;
        this.positionInQueue = -1; // not in queue initially
    }


    public String getId() {
        return id;
    }
    public Size getSize() {
        return size;
    }
    public int getContainerAmount() {
        return containerAmount;
    }

    public void setContainerAmount(int containerAmount) {
        this.containerAmount = containerAmount;
    }

    public void unloadContainers(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount to unload cannot be negative");
        }
        if (amount > containerAmount) {
            throw new IllegalArgumentException("Cannot unload more containers than the ship has");
        }
        this.containerAmount -= amount;
    }

    public void setPositionInQueue(int positionInQueue) {
        this.positionInQueue = positionInQueue;
    }
}
