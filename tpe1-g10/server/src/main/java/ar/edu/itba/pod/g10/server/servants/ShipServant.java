package ar.edu.itba.pod.g10.server.servants;

import ar.edu.itba.pod.g10.server.locks.Locks;
import ar.edu.itba.pod.g10.server.models.Dock;
import ar.edu.itba.pod.g10.server.models.Ship;
import ar.edu.itba.pod.g10.server.repositories.DockRepository;
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

public class ShipServant extends ShipServiceGrpc.ShipServiceImplBase {

    private final ShipRepository shipRepository;
    private final DockRepository dockRepository;

    public ShipServant(ShipRepository shipRepository, DockRepository dockRepository) {
        this.shipRepository = shipRepository;
        this.dockRepository = dockRepository;
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


        final Ship ship = new Ship(request.getShipId(), request.getShipSize(), countContainers(request.getShipContainers()));
        if (!shipRepository.addShipIfAbsent(ship)) {
            responseObserver.onError(io.grpc.Status.ALREADY_EXISTS.withDescription("Ship already requested: " + ship.getId()).asRuntimeException());
            return;
        }

        final java.util.concurrent.locks.ReentrantLock repoLock = Locks.SHIP_REPOSITORY_LOCK;
        final java.util.concurrent.locks.ReentrantLock dockLock = Locks.DOCK_LOCK;

        // condición compartida a nivel de clase (definila como static en tu servicio):
        // private static final Condition CHANGED = Locks.SHIP_REPOSITORY_LOCK.newCondition();
        // la usamos acá:
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
            Dock dock = dockRepository.getDockByShipId(request.getShipId()).orElseThrow(() -> new RuntimeException("Ship not found: " + request.getShipId()));

            dockLock.lock();
            try {
                dockRepository.releaseDock(dock.getNumber());
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
