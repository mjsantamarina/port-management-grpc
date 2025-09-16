package ar.edu.itba.pod.g10.server.models;

import ar.edu.itba.pod.g10.server.servants.DockServant;
import ar.edu.itba.pod.grpc.utils.DockStatus;
import ar.edu.itba.pod.grpc.utils.Size;

public class Dock {
    private final int number;
    private final Size size;
    private DockStatus status;
    private Ship ship;

    public Dock(final int number, final Size size, final DockStatus status) {
        this.number = number;
        this.size = size;
        this.status = status;
    }

    public Ship getShip() {
        return ship;
    }
    public void setShip(final Ship ship) {
        this.ship = ship;
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
