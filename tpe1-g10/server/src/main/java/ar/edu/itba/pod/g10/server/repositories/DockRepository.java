package ar.edu.itba.pod.g10.server.repositories;

import ar.edu.itba.pod.g10.server.locks.Locks;
import ar.edu.itba.pod.g10.server.models.Dock;
import ar.edu.itba.pod.g10.server.models.Ship;
import ar.edu.itba.pod.grpc.utils.DockStatus;
import ar.edu.itba.pod.grpc.utils.Size;

import java.util.Collection;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

public class DockRepository {
    private int dockAmount;
    private final ConcurrentMap<Size, Deque<Dock>> docksBySize;
    private final ConcurrentMap<Integer, Dock> dockMap;


    public DockRepository() {
        this.dockAmount = 0;
        this.docksBySize = new ConcurrentHashMap<>();
        for(Size size : Size.values()){
            docksBySize.put(size, new ConcurrentLinkedDeque<>());
        }
        this.dockMap = new ConcurrentHashMap<>();

    }

    public synchronized int addDock(Size dockSize) {
        dockAmount++;
        Dock toAdd = new Dock(dockAmount, dockSize, DockStatus.DOCK_STATUS_IDLE);
        this.docksBySize.get(dockSize).add(toAdd);
        return dockAmount;
    }

    public Dock dockById(int dockNumber) {
        return dockMap.get(dockNumber);
    }


    public Dock toggleDock(int dockNumber) {
        Optional<Dock> maybeDock = getDockByNumber(dockNumber);
        if(maybeDock.isEmpty()){
            //TODO hacer la excepcion
        }
        Dock dock = maybeDock.get();
        //TODO Lockear esta funcion y hacer los locks
        try {
            switch (dock.getStatus()) {
                case DOCK_STATUS_BUSY -> throw new IllegalStateException("El dock ya esta ocupado");
                //TODO ver msj de error
                case DOCK_STATUS_CLOSED -> dock.setStatus(DockStatus.DOCK_STATUS_IDLE);
                case DOCK_STATUS_IDLE -> dock.setStatus(DockStatus.DOCK_STATUS_CLOSED);
            }
            return dock;
        } finally {
            return dock;
        }
    }

    public Optional<Dock> getDockByNumber(int dockNumber) {
        return docksBySize.values().stream().flatMap(Collection::stream)
                .filter(dock -> dock.getNumber() == dockNumber).findFirst();
    }

    public boolean hasAvailableDock(Size shipSize) {
        Locks.DOCK_LOCK.lock();
        try {
            return findAvailableDock(shipSize).isPresent();
        } finally {
            Locks.DOCK_LOCK.unlock();
        }
    }

    public Dock assignDock(Ship ship) {
        Locks.DOCK_LOCK.lock();
        try {
            Dock d = findAvailableDock(ship.getSize()).orElseThrow(() -> new IllegalStateException("No available dock"));
            d.setStatus(DockStatus.DOCK_STATUS_BUSY);
            d.setShip(ship);
            return d;
        } finally {
            Locks.DOCK_LOCK.unlock();
        }
    }

    private Optional<Dock> findAvailableDock(Size shipSize) {
        return sizesOrderFor(shipSize)
                .stream()
                .flatMap(sz -> {
                    Deque<Dock> dq = docksBySize.get(sz);
                    return dq == null ? Stream.empty() : dq.stream();
                })
                .filter(d -> d.getStatus() == DockStatus.DOCK_STATUS_IDLE)
                .min((a, b) -> Integer.compare(a.getNumber(), b.getNumber()));
    }

    private java.util.List<Size> sizesOrderFor(Size shipSize) {
        return switch (shipSize) {
            case SIZE_SMALL -> java.util.List.of(Size.SIZE_SMALL, Size.SIZE_MEDIUM, Size.SIZE_LARGE);
            case SIZE_MEDIUM -> java.util.List.of(Size.SIZE_MEDIUM, Size.SIZE_LARGE);
            case SIZE_LARGE -> java.util.List.of(Size.SIZE_LARGE);
            default -> java.util.List.of();
        };
    }


    public void releaseDock(int dockNumber) {
        Locks.DOCK_LOCK.lock();
        try {
            Dock dock = getDockByNumber(dockNumber).orElseThrow(() -> new IllegalArgumentException("Dock not found: " + dockNumber));
            if (dock.getStatus() != DockStatus.DOCK_STATUS_BUSY) {
                throw new IllegalStateException("Dock " + dockNumber + " is not busy");
            }
            if(dock.getShip().hasContainers()){
                throw new IllegalStateException("Ship still has containers to unload");
            }
            dock.setStatus(DockStatus.DOCK_STATUS_IDLE);
            dock.setShip(null);
        } finally {
            Locks.DOCK_LOCK.unlock();
        }
    }


    //TODO: CHECKEAR ESTO
    public Optional<Ship> getAttachedShipById(String shipId) {
        return docksBySize.values().stream().flatMap(Collection::stream)
                .filter(dock -> dock.getShip() != null && dock.getShip().getId().equals(shipId))
                .map(Dock::getShip)
                .findFirst();
    }

    public Optional<Dock> getDockByShipId(String shipId) {
        return docksBySize.values().stream().flatMap(Collection::stream)
                .filter(dock -> dock.getShip() != null && dock.getShip().getId().equals(shipId))
                .findFirst();
    }


}
