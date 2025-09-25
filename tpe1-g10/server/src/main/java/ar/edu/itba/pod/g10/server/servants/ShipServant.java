package ar.edu.itba.pod.g10.server.servants;

import ar.edu.itba.pod.g10.server.locks.Locks;
import ar.edu.itba.pod.g10.server.models.Dock;
import ar.edu.itba.pod.g10.server.models.Ship;
import ar.edu.itba.pod.g10.server.repositories.DockRepository;
import ar.edu.itba.pod.g10.server.repositories.NotificationRepository;
import ar.edu.itba.pod.g10.server.repositories.ShipRepository;
import ar.edu.itba.pod.grpc.dock.DockStatusResponse;
import ar.edu.itba.pod.grpc.ship.*;
import ar.edu.itba.pod.grpc.utils.DockEventType;
import ar.edu.itba.pod.grpc.utils.Size;
import com.google.protobuf.UInt32Value;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;


import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ShipServant extends ShipServiceGrpc.ShipServiceImplBase {

    private final ShipRepository shipRepository;
    private final DockRepository dockRepository;
    private final NotificationRepository notificationRepository;

    public ShipServant(ShipRepository shipRepository, DockRepository dockRepository, NotificationRepository notificationRepository) {
        this.shipRepository = shipRepository;
        this.dockRepository = dockRepository;
        this.notificationRepository = notificationRepository;
    }

    private static Deque<String> countContainers(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        Deque<String> containers = new ConcurrentLinkedDeque<>();
             containers.addAll( Arrays.stream(input.split("#")).toList());
        return containers;
    }

    @Override
    public void getDockStatus(UInt32Value request, StreamObserver<DockStatusResponse> responseObserver) {
        DockStatusResponse.Builder responseBuilder = DockStatusResponse.newBuilder();
        try {
            Dock dock = dockRepository.getDockByNumber(request.getValue())
                    .orElseThrow(() -> new IllegalArgumentException("Dock not found: " + request.getValue()));
            responseBuilder.setDockNumber(dock.getNumber())
                    .setDockSize(dock.getSize())
                    .setDockStatus(dock.getStatus());
            if (dock.getShip() != null) {
                Ship ship = dock.getShip();
                responseBuilder.setContainersLeft(ship.getContainerAmount())
                        .setShipId(ship.getId())
                        .setShipSize(ship.getSize());
            }
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void requestDock(DockRequest request, StreamObserver<DockResponse> responseObserver) {
        final java.util.concurrent.locks.ReentrantLock repoLock = Locks.SHIP_REPOSITORY_LOCK;
        final java.util.concurrent.locks.ReentrantLock dockLock = Locks.DOCK_LOCK;
        final Ship ship = new Ship(request.getShipId(), request.getShipSize(), countContainers(request.getShipContainers()));

        repoLock.lock();
        try {
            shipRepository.addShip(ship);
        }catch (Exception e){
            responseObserver.onError(io.grpc.Status.ALREADY_EXISTS.withDescription("Ship already requested: " + ship.getId()).asRuntimeException());
            return;
        }finally {
            repoLock.unlock();
        }
        final Condition CHANGED = Locks.CHANGED;
        repoLock.lock();
        try {
            int lastAhead = shipsAhead(shipRepository.getShipQueue(), ship);
            responseObserver.onNext(DockResponse.newBuilder()
                    .setShipId(ship.getId())
                    .setShipSize(ship.getSize())
                    .setDockSize(Size.SIZE_UNSPECIFIED)
                    .setShipsAhead(lastAhead)
                    .setEventType(DockEventType.WAITING)
                    .build());

            while (true) {
                // intento de asignación si corresponde
                if (!shipRepository.getShipQueue().isEmpty() && shipRepository.getShipQueue().peekFirst() == ship) {
                    dockLock.lock();
                    try {
                        if (dockRepository.hasAvailableDock(ship.getSize())) {
                            Dock dock = dockRepository.assignDock(ship);
                            notificationRepository.publishDockOccupied(dock, ship, dock.getCraneList().stream().map(crane -> crane.getId()).toList());
                            shipRepository.getShipQueue().pollFirst();
                            responseObserver.onNext(DockResponse.newBuilder()
                                    .setShipId(ship.getId())
                                    .setShipSize(ship.getSize())
                                    .setDockNumber(dock.getNumber())
                                    .setDockSize(dock.getSize())
                                    .setShipsAhead(0)
                                    .setEventType(DockEventType.ASSIGNED)
                                    .build());
                            responseObserver.onCompleted();
                            CHANGED.signalAll();
                            return;
                        }
                    } finally {
                        dockLock.unlock();
                    }
                }

                int ahead = shipsAhead(shipRepository.getShipQueue(), ship);
                if (ahead != lastAhead) {
                    lastAhead = ahead;
                    responseObserver.onNext(DockResponse.newBuilder()
                            .setShipId(ship.getId())
                            .setShipSize(ship.getSize())
                            .setDockSize(Size.SIZE_UNSPECIFIED)
                            .setShipsAhead(ahead)
                            .setEventType(DockEventType.QUEUE_UPDATE)
                            .build());
                }

                CHANGED.await();
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            responseObserver.onError(Status.CANCELLED.withDescription("interrupted").asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        } finally {
            repoLock.unlock();
        }
    }


    @Override
    public void leaveDock(LeaveDockRequest request, StreamObserver<LeaveDockResponse> responseObserver) {
        final java.util.concurrent.locks.ReentrantLock repoLock = Locks.SHIP_REPOSITORY_LOCK;
        final java.util.concurrent.locks.ReentrantLock dockLock = Locks.DOCK_LOCK;
        final Condition CHANGED = Locks.CHANGED;
        repoLock.lock();
        try {
            Dock dock = dockRepository.getDockByNumber(request.getDockNumber()).orElseThrow(() -> new IllegalArgumentException("Dock not found: " + request.getDockNumber()));
            Ship ship = dock.getShip();
            if(dock.getShip()==null || !dock.getShip().equals(request.getShipId())){
                throw new RuntimeException("Ship not " + request.getShipId() + " not in Dock");
            }
            dockLock.lock();
            try {
                dockRepository.releaseDock(dock.getNumber());
                notificationRepository.publishShipLeft(dock, ship, dock.getCraneList().stream().map(crane -> crane.getId()).toList());
                responseObserver.onNext(LeaveDockResponse.newBuilder()
                        .setShipId(request.getShipId())
                        .setDockNumber(dock.getNumber())
                        .setDockSize(dock.getSize())
                        .build());
                responseObserver.onCompleted();
                CHANGED.signalAll();
            } finally {
                dockLock.unlock();
            }
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (IllegalStateException e) {
            responseObserver.onError(Status.FAILED_PRECONDITION.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        } finally {
            repoLock.unlock();
        }

    }

    private static int shipsAhead(Deque<Ship> q, Ship s) {
        int i = 0;
        for (Ship x : q) {
            if (x == s) return i;
            i++;
        }
        return i;
    }



}
