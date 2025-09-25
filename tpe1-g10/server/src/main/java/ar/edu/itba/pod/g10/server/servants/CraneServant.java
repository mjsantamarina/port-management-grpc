package ar.edu.itba.pod.g10.server.servants;

import ar.edu.itba.pod.g10.server.locks.Locks;
import ar.edu.itba.pod.g10.server.models.Crane;
import ar.edu.itba.pod.g10.server.models.Dock;
import ar.edu.itba.pod.g10.server.models.Ship;
import ar.edu.itba.pod.g10.server.repositories.ContainerRepository;
import ar.edu.itba.pod.g10.server.repositories.CraneRepository;
import ar.edu.itba.pod.g10.server.repositories.NotificationRepository;
import ar.edu.itba.pod.grpc.crane.CraneServiceGrpc;
import ar.edu.itba.pod.grpc.crane.CraneStatusResponse;
import ar.edu.itba.pod.grpc.crane.UnloadContainersRequest;
import ar.edu.itba.pod.grpc.crane.UnloadContainersResponse;
import ar.edu.itba.pod.grpc.utils.CraneStatus;
import com.google.protobuf.UInt32Value;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class CraneServant extends CraneServiceGrpc.CraneServiceImplBase {

    private final CraneRepository craneRepository;
    private final NotificationRepository notificationRepository;
    private final ContainerRepository containerRepository;
    private final ExecutorService notifier;




    public CraneServant(CraneRepository craneRepository, NotificationRepository notificationRepository, ContainerRepository containerRepository, ExecutorService notifier) {
        this.containerRepository = containerRepository;
        this.craneRepository = craneRepository;
        this.notificationRepository = notificationRepository;
        this.notifier = notifier;
    }

    @Override
    public StreamObserver<UnloadContainersRequest> unloadContainers(StreamObserver<UnloadContainersResponse> responseObserver) {
        return new StreamObserver<>() {
            private Integer currentCraneId = null;
            private boolean acquired = false;

            private void fail(io.grpc.Status status, String msg) {
                if (acquired) { Locks.releaseCrane(currentCraneId); acquired = false; }
                responseObserver.onError(status.withDescription(msg).asRuntimeException());
            }
            private void complete() {
                if (acquired) { Locks.releaseCrane(currentCraneId); acquired = false; }
                responseObserver.onCompleted();
            }

            @Override
            public void onNext(UnloadContainersRequest req) {
                try {
                    if (currentCraneId == null) {
                        currentCraneId = req.getCraneNumber();
                        if (!Locks.tryAcquireCrane(currentCraneId)) {
                            fail(io.grpc.Status.FAILED_PRECONDITION, "Crane is busy unloading");
                            return;
                        }
                        acquired = true;
                    } else if (req.getCraneNumber() != currentCraneId) {
                        fail(io.grpc.Status.INVALID_ARGUMENT, "Crane number changed within stream");
                        return;
                    }

                    Crane crane = craneRepository.getCrane(currentCraneId);
                    if (crane == null) { fail(io.grpc.Status.NOT_FOUND, "Crane not found"); return; }

                    Dock dock = crane.getDock();
                    if (dock == null || dock.getShip() == null) { fail(io.grpc.Status.FAILED_PRECONDITION, "No ship in dock"); return; }

                    Ship ship = dock.getShip();
                    String expected = ship.peekContainer();
                    if (expected == null) { complete(); return; }

                    if (crane.getStatus() == CraneStatus.CRANE_STATUS_CLOSED) { fail(io.grpc.Status.FAILED_PRECONDITION, "Crane is closed"); return; }
                    if (crane.getStatus() == CraneStatus.CRANE_STATUS_MAINTENANCE) { fail(io.grpc.Status.FAILED_PRECONDITION, "Crane is in maintenance"); return; }
                    if (!expected.equals(req.getContainerId())) { fail(io.grpc.Status.FAILED_PRECONDITION, "Expected container " + expected + ", got " + req.getContainerId()); return; }

                    String unloaded = ship.unloadContainer();
                    if (unloaded != null) {
                        if (!crane.getCraneStatus().equals(CraneStatus.CRANE_STATUS_UNLOADING)) {
                            crane.setCraneStatus(CraneStatus.CRANE_STATUS_UNLOADING);
                            notifier.execute(() -> notificationRepository.publishCraneStatus(crane));
                        }
                        notifier.execute(() -> notificationRepository.publishUnloaded(crane, unloaded));
                        ship.setUnloaded();
                        crane.setCurrentContainers(crane.getCurrentContainers() + 1);
                        containerRepository.addContainer(unloaded, dock, crane);
                    }

                    if (crane.needsMainteinance()) {
                        crane.setCraneStatus(CraneStatus.CRANE_STATUS_MAINTENANCE);
                        notifier.execute(() -> notificationRepository.publishCraneStatus(crane));
                    }

                    boolean last = ship.getContainerAmount() == 0;

                    UnloadContainersResponse resp = UnloadContainersResponse.newBuilder()
                            .setContainerId(req.getContainerId())
                            .setCraneNumber(crane.getId())
                            .setDockNumber(dock.getNumber())
                            .setDockSize(dock.getSize())
                            .setUnloadedContainers(crane.getCurrentContainers())
                            .setMaxContainers(crane.getMaxContainers())
                            .setIsLast(last)
                            .build();

                    responseObserver.onNext(resp);

                    if (last) {
                        complete();
                    }

                } catch (Exception e) {
                    if (acquired) { Locks.releaseCrane(currentCraneId); acquired = false; }
                    responseObserver.onError(io.grpc.Status.INTERNAL.withDescription("Unexpected error: " + e.getMessage()).withCause(e).asRuntimeException());
                }
            }

            @Override
            public void onError(Throwable t) {
                if (acquired) { Locks.releaseCrane(currentCraneId); acquired = false; }
            }

            @Override
            public void onCompleted() {
                if (acquired) { Locks.releaseCrane(currentCraneId); acquired = false; }
                responseObserver.onCompleted();
            }
        };
    }





    @Override
    public void getCraneStatus(UInt32Value request, StreamObserver<CraneStatusResponse> responseObserver) {
        Crane crane = craneRepository.getCrane(request.getValue());
        if (crane == null) {
            responseObserver.onError(Status.NOT_FOUND.withDescription("Crane not found").asRuntimeException());
            return;
        }
        CraneStatusResponse craneStatusResponse = CraneStatusResponse.newBuilder()
                .setCraneStatus(crane.getStatus())
                .setCraneNumber(crane.getId())
                .setUnloadedContainers(crane.getCurrentContainers())
                .setMaxContainers(crane.getMaxContainers())
                .setDockNumber(crane.getDock().getNumber())
                .setDockSize(crane.getDock().getSize())
                .build();
        responseObserver.onNext(craneStatusResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void fixCrane(UInt32Value request, StreamObserver<CraneStatusResponse> responseObserver) {
        Crane crane = craneRepository.getCrane(request.getValue());
        if (crane == null) {
            responseObserver.onError(Status.NOT_FOUND.withDescription("Crane not found").asRuntimeException());
            return;
        }
        synchronized (crane) {
            if (crane.getStatus().equals(CraneStatus.CRANE_STATUS_MAINTENANCE)) {
                crane.setCraneStatus(CraneStatus.CRANE_STATUS_OPEN);
                crane.setCurrentContainers(0);
                notifier.execute(()->notificationRepository.publishCraneStatus(crane));
                CraneStatusResponse craneStatusResponse = CraneStatusResponse.newBuilder()
                        .setCraneStatus(crane.getStatus())
                        .setCraneNumber(crane.getId())
                        .build();
                responseObserver.onNext(craneStatusResponse);
                responseObserver.onCompleted();
            } else {
                responseObserver.onError(Status.FAILED_PRECONDITION.withDescription("Crane is not in maintenance").asRuntimeException());
            }
        }
    }
}
