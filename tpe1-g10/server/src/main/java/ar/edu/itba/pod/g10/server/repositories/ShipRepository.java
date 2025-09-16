package ar.edu.itba.pod.g10.server.repositories;

import ar.edu.itba.pod.g10.server.models.Ship;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public class ShipRepository {
    private int shipCount;
    private final Deque<Ship> shipQueue;

    public ShipRepository() {
        this.shipQueue = new ConcurrentLinkedDeque<>();
    }

    public void addShip(Ship ship) {
        shipQueue.addLast(ship);
        shipCount++;
    }

    public Deque<Ship> getShipQueue() {
        return shipQueue;
    }

    public int getShipCount() {
        return shipCount;
    }


    public boolean addShipIfAbsent(Ship ship) {
        if (shipQueue.contains(ship)) {
            return false;
        } else {
            shipQueue.addLast(ship);
            shipCount++;
            return true;
        }
    }
}
