package ar.edu.itba.pod.g10.server.servants;

import ar.edu.itba.pod.g10.server.models.Crane;
import ar.edu.itba.pod.g10.server.models.Dock;
import ar.edu.itba.pod.g10.server.repositories.CraneRepository;
import ar.edu.itba.pod.g10.server.repositories.DockRepository;
import ar.edu.itba.pod.grpc.dock.*;
import com.google.protobuf.UInt32Value;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DockServant extends DockServiceGrpc.DockServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(DockServant.class);
    private final DockRepository dockRepository;
    private final CraneRepository craneRepository;

    public DockServant(DockRepository dockRepository, CraneRepository craneRepository) {
        this.dockRepository = dockRepository;
        this.craneRepository = craneRepository;
    }


    @Override
    public void addDock(AddDockRequest request, StreamObserver<AddDockResponse> responseObserver){
        int dockNumber = dockRepository.addDock(request.getDockSize());
        responseObserver.onNext(AddDockResponse.newBuilder()
                .setDockNumber(dockNumber)
                .setDockSize(request.getDockSize())
                .build());
        responseObserver.onCompleted();
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
    public void toggleDockStatus(UInt32Value request, StreamObserver<DockStatusResponse> responseObserver){
        logger.info("toggleDockStatus called for dock number: {}", request.getValue());
        Dock dock;
        try {
            logger.info("Attempting to toggle dock status...");
            dock = dockRepository.toggleDock(request.getValue());
            logger.info("Successfully toggled dock. New status: {}, Size: {}", dock.getStatus(), dock.getSize());
        } catch (Exception e) {
            logger.error("Error toggling dock status: {}", e.getMessage(), e);
            responseObserver.onError(
                    io.grpc.Status.PERMISSION_DENIED
                            .withDescription(e.getMessage())
                            .asRuntimeException()
            );
            return;
        }
        logger.info("Building response...");
        DockStatusResponse dockStatusResponse = DockStatusResponse.newBuilder()
                .setDockNumber(dock.getNumber())
                .setDockSize(dock.getSize())
                .setDockStatus(dock.getStatus())
                .build();
        logger.info("Sending response back to client...");
        responseObserver.onNext(dockStatusResponse);
        responseObserver.onCompleted();
        logger.info("toggleDockStatus completed successfully");
    }
}
