package ar.edu.itba.pod.g10.server.models;

import ar.edu.itba.pod.g10.server.servants.DockServant;
import ar.edu.itba.pod.grpc.utils.DockStatus;
import ar.edu.itba.pod.grpc.utils.Size;

import java.util.ArrayList;
import java.util.List;

public class Dock {
    private final int number;
    private final Size size;
    private DockStatus status;
    private Ship ship;
    private List<Crane> craneList;

    public Dock(final int number, final Size size, final DockStatus status) {
        this.number = number;
        this.size = size;
        this.status = status;
        craneList = new ArrayList<>();
    }

    public Ship getShip() {
        return ship;
    }
    public void setShip(final Ship ship) {
        this.ship = ship;
    }
    public List<Crane> getCraneList() {
        return craneList;
    }

    public synchronized Crane addCrane(int containerCapacity, int craneNumber) {
        Crane crane = new Crane(craneNumber, this, containerCapacity);
        craneList.add(crane);
        return crane;
    }

    public int getNumber() {
        return number;
    }
    public Size getSize() {
        return size;
    }
    public DockStatus getStatus() {
        return status;
    }
    public void setStatus(final DockStatus status) {
        this.status = status;
    }
}
