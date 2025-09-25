package ar.edu.itba.pod.g10.server.servants;

import ar.edu.itba.pod.g10.server.locks.Locks;
import ar.edu.itba.pod.g10.server.models.Crane;
import ar.edu.itba.pod.g10.server.models.Dock;
import ar.edu.itba.pod.g10.server.repositories.CraneRepository;
import ar.edu.itba.pod.g10.server.repositories.DockRepository;
import ar.edu.itba.pod.g10.server.repositories.NotificationRepository;
import ar.edu.itba.pod.grpc.dock.*;
import ar.edu.itba.pod.grpc.utils.DockStatus;
import com.google.protobuf.UInt32Value;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class DockServant extends DockServiceGrpc.DockServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(DockServant.class);
    private final DockRepository dockRepository;
    private final CraneRepository craneRepository;
    private final NotificationRepository notificationRepository;

    public DockServant(DockRepository dockRepository, CraneRepository craneRepository, NotificationRepository notificationRepository) {
        this.dockRepository = dockRepository;
        this.craneRepository = craneRepository;
        this.notificationRepository = notificationRepository;
    }


    @Override
    public void addDock(AddDockRequest request, StreamObserver<AddDockResponse> responseObserver){
        final java.util.concurrent.locks.ReentrantLock repoLock = Locks.SHIP_REPOSITORY_LOCK;
        final Condition CHANGED = Locks.CHANGED;

        repoLock.lock();
        try {
            int dockNumber = dockRepository.addDock(request.getDockSize());
            CHANGED.signalAll();
            AddDockResponse resp = AddDockResponse.newBuilder()
                    .setDockNumber(dockNumber)
                    .setDockSize(request.getDockSize())
                    .build();

            responseObserver.onNext(resp);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(
                    io.grpc.Status.INTERNAL
                            .withDescription("Unexpected error adding dock: " + e.getMessage())
                            .withCause(e)
                            .asRuntimeException()
            );
        } finally {
            repoLock.unlock();
        }
    }


    @Override
    public void addCraneToDock(AddCraneToDockRequest request, StreamObserver<AddCraneToDockResponse> responseObserver) {
        Dock dock = dockRepository.getDockByNumber(request.getDockNumber()).orElseThrow(() -> new RuntimeException("Dock not found"));
        Crane crane = dock.addCrane(request.getContainerAmount(), craneRepository.getNextCraneNumber());
        craneRepository.addCrane(crane);
        responseObserver.onNext(AddCraneToDockResponse.newBuilder()
                .setCraneNumber(crane.getId()).setLastContainerAmount(Integer.valueOf(0))
                .setProvidedContainerAmount(request.getContainerAmount()).build()
        );
        responseObserver.onCompleted();
    }

    @Override
    public void openDock(UInt32Value request, StreamObserver<DockStatusResponse> responseObserver) {
        try {
            Dock dock = dockRepository.getDockByNumber(request.getValue())
                    .orElseThrow(() -> new IllegalArgumentException("Dock not found"));

            synchronized (dock) {
                if (dock.getStatus() == DockStatus.DOCK_STATUS_BUSY) {
                    responseObserver.onError(Status.FAILED_PRECONDITION.withDescription("Dock is busy").asRuntimeException());
                    return;
                }
                if (dock.getStatus() == DockStatus.DOCK_STATUS_CLOSED) {
                    dockRepository.openDock(dock, notificationRepository);
                }

                DockStatusResponse resp = DockStatusResponse.newBuilder()
                        .setDockNumber(dock.getNumber())
                        .setDockSize(dock.getSize())
                        .setDockStatus(dock.getStatus())
                        .build();
                responseObserver.onNext(resp);
                responseObserver.onCompleted();
            }
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }



    @Override
    public void closeDock(UInt32Value request, StreamObserver<DockStatusResponse> responseObserver) {
        try {
            Dock dock = dockRepository.getDockByNumber(request.getValue())
                    .orElseThrow(() -> new IllegalArgumentException("Dock not found"));

            synchronized (dock) {
                if (dock.getStatus() == DockStatus.DOCK_STATUS_BUSY) {
                    responseObserver.onError(Status.PERMISSION_DENIED.withDescription("Dock is busy").asRuntimeException());
                    return;
                }
                if (dock.getStatus() == DockStatus.DOCK_STATUS_IDLE) {
                    dockRepository.closeDock(dock, notificationRepository);
                }

                DockStatusResponse resp = DockStatusResponse.newBuilder()
                        .setDockNumber(dock.getNumber())
                        .setDockSize(dock.getSize())
                        .setDockStatus(dock.getStatus())
                        .build();
                responseObserver.onNext(resp);
                responseObserver.onCompleted();
            }
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

}
