package ar.edu.itba.pod.g10.server.repositories;

import ar.edu.itba.pod.g10.server.models.Ship;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;

public class ShipRepository {
    private int shipCount;
    private final Deque<Ship> shipQueue;
    private final ConcurrentMap<String, Ship> shipMap;
    private final List<String> containerList;


    public ShipRepository() {
        this.shipQueue = new ConcurrentLinkedDeque<>();
        this.shipMap = new ConcurrentHashMap<>();
        this.containerList = new ArrayList<>();
    }

    public void addShip(Ship ship) {
        shipMap.computeIfPresent(ship.getId(), (s, existingShip) -> {
            throw new IllegalArgumentException("Ship with ID " + ship.getId() + " already exists.");
        });
        shipMap.put(ship.getId(), ship);
        shipQueue.addLast(ship);
        shipCount++;
    }



    public Ship getShip(String id) {
        return shipMap.get(id);
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


    public SortedSet<Ship> getPendingShips() {
        SortedSet<Ship> pendingShips = new TreeSet<>();
        for (Ship ship : shipMap.values()) {
            if (!ship.getUnloaded() ) {
                pendingShips.add(ship);
            }
        }
        return pendingShips;
    }
}
