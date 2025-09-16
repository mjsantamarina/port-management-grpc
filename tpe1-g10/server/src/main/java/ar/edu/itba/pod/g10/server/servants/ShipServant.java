package ar.edu.itba.pod.g10.server.servants;

import ar.edu.itba.pod.g10.server.locks.Locks;
import ar.edu.itba.pod.g10.server.models.Dock;
import ar.edu.itba.pod.g10.server.models.Ship;
import ar.edu.itba.pod.g10.server.repositories.DockRespository;
import ar.edu.itba.pod.g10.server.repositories.ShipRepository;
import ar.edu.itba.pod.grpc.ship.DockRequest;
import ar.edu.itba.pod.grpc.ship.DockResponse;
import ar.edu.itba.pod.grpc.ship.ShipServiceGrpc;
import ar.edu.itba.pod.grpc.utils.DockEventType;
import ar.edu.itba.pod.grpc.utils.Size;
import io.grpc.stub.StreamObserver;


import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ShipServant extends ShipServiceGrpc.ShipServiceImplBase {

    private final ShipRepository shipRepository;
    private final DockRespository dockRespository;
    private static final Condition CHANGED = Locks.SHIP_REPOSITORY_LOCK.newCondition();

    public ShipServant(ShipRepository shipRepository, DockRespository dockRespository) {
        this.shipRepository = shipRepository;
        this.dockRespository = dockRespository;
    }

//    @Override
//    public void requestDock(DockRequest request, StreamObserver<DockResponse> responseObserver) {
//        DockResponse dockResponse;
//        try {
//            Ship myShip = new Ship(request.getShipId(), request.getShipSize(), countContainers(request.getShipContainers()));
//            shipRepository.addShip(myShip);
//            myShip.setPositionInQueue(shipRepository.getShipCount());
//
//            //si estoy primero en la cola y hay un dock disponible de mi tamaño, me asigno el dock y salgo de la cola
//            while (true) {
//
//            }
//
//
//
//
//
//            while( shipRepository.getShipQueue().peekFirst() != myShip || !dockRespository.isThereAvailableDock(myShip.getSize()) ) {
//                // busy wait
//
//            }
//
//
//        } catch (Exception e) {
//            responseObserver.onError(
//                    io.grpc.Status.PERMISSION_DENIED
//                            .withDescription(e.getMessage())
//                            .asRuntimeException()
//            );
//            return;
//        }
//    }

    private static int countContainers(String input) {
        if (input == null || input.isEmpty()) {
            return 0;
        }
        return input.split("#").length;
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
        final java.util.concurrent.locks.Condition changed = CHANGED;

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
                        if (dockRespository.hasAvailableDock(ship.getSize())) {
                            Dock dock = dockRespository.assignDock(ship.getSize());
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
                            changed.signalAll();
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

                changed.await();
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            responseObserver.onError(io.grpc.Status.CANCELLED.withDescription("interrupted").asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        } finally {
            repoLock.unlock();
        }
    }

    private static int shipsAhead(java.util.Deque<Ship> q, Ship s) {
        int i = 0;
        for (Ship x : q) {
            if (x == s) return i;
            i++;
        }
        return i;
    }


}
