package ar.edu.itba.pod.g10.server.servants;

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

public class CraneServant extends CraneServiceGrpc.CraneServiceImplBase {

    private final CraneRepository craneRepository;
    private final NotificationRepository notificationRepository;
    private final ContainerRepository containerRepository;

    public CraneServant(CraneRepository craneRepository, NotificationRepository notificationRepository, ContainerRepository containerRepository) {
        this.containerRepository = containerRepository;
        this.craneRepository = craneRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public StreamObserver<UnloadContainersRequest> unloadContainers(StreamObserver<UnloadContainersResponse> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(UnloadContainersRequest req) {
                try {
                    Crane crane = craneRepository.getCrane(req.getCraneNumber());
                    System.out.println("crane number en server"+req.getCraneNumber());
                    if (crane == null) {
                        responseObserver.onError(Status.NOT_FOUND.withDescription("Crane not found").asRuntimeException());
                        return;
                    }

                    Dock dock = crane.getDock();
                    if (dock == null || dock.getShip() == null) {
                        responseObserver.onError(Status.FAILED_PRECONDITION.withDescription("No ship in dock").asRuntimeException());
                        return;
                    }

                    Ship ship = dock.getShip();
                    String expected = ship.peekContainer();


                    if (expected == null) {
                        responseObserver.onCompleted();
                        return;
                    }



                    if (crane.getStatus() == CraneStatus.CRANE_STATUS_CLOSED) {
                        responseObserver.onError(Status.FAILED_PRECONDITION.withDescription("Crane is closed").asRuntimeException());
                        return;
                    }

                    if(crane.getStatus() == CraneStatus.CRANE_STATUS_MAINTENANCE){
                        responseObserver.onError(Status.FAILED_PRECONDITION.withDescription("Crane is in maintenance").asRuntimeException());
                        return;
                    }

                    if (!expected.equals(req.getContainerId())) {
                        responseObserver.onCompleted();
                        return;
                    }

                    // descarga válida
                    String unloaded = ship.unloadContainer();
                    if(unloaded != null){
                        notificationRepository.publishUnloaded(crane, unloaded);
                        ship.setUnloaded();
                        crane.setCurrentContainers(crane.getCurrentContainers() + 1);
                        crane.setCraneStatus(CraneStatus.CRANE_STATUS_UNLOADING);
                        notificationRepository.publishCraneStatus(crane);
                        containerRepository.addContainer(unloaded, dock, crane);
                    }
                    if(crane.needsMainteinance()){
                        crane.setCraneStatus(CraneStatus.CRANE_STATUS_MAINTENANCE);
                        notificationRepository.publishCraneStatus(crane);
                    }

                    if(ship.getContainerAmount()==0) {
                        responseObserver.onNext(UnloadContainersResponse.newBuilder()
                        .setContainerId(req.getContainerId())
                                .setCraneNumber(crane.getId())
                                .setDockNumber(dock.getNumber())
                                .setDockSize(dock.getSize())
                                .setUnloadedContainers(crane.getCurrentContainers())
                                .setMaxContainers(crane.getMaxContainers())
                                .setIsLast(dock.getShip().getContainerAmount()==0).build());
                        responseObserver.onCompleted();

                    }

                    if(crane.needsMainteinance()){
                        crane.setCraneStatus(CraneStatus.CRANE_STATUS_MAINTENANCE);
                        notificationRepository.publishCraneStatus(crane);
                        responseObserver.onError(Status.FAILED_PRECONDITION.withDescription("Crane is in maintenance").asRuntimeException());
                        return;
                    }

                    responseObserver.onNext(UnloadContainersResponse.newBuilder()
                            .setContainerId(req.getContainerId())
                            .setCraneNumber(crane.getId())
                                    .setDockNumber(dock.getNumber())
                                    .setDockSize(dock.getSize())
                                    .setUnloadedContainers(crane.getCurrentContainers())
                                    .setMaxContainers(crane.getMaxContainers())
                                    .setIsLast(dock.getShip().getContainerAmount()==0)
                            .build());

                } catch (Exception e) {
                    responseObserver.onError(Status.INTERNAL.withDescription("Unexpected error: " + e.getMessage()).withCause(e).asRuntimeException());
                }
            }

            @Override
            public void onError(Throwable t) {
                // TODO: loggear si hace falta
            }

            @Override
            public void onCompleted() {
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
        if (crane.getStatus().equals(CraneStatus.CRANE_STATUS_MAINTENANCE)) {
            crane.setCraneStatus(CraneStatus.CRANE_STATUS_OPEN);
            notificationRepository.publishCraneStatus(crane);
            crane.setCurrentContainers(0);
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
