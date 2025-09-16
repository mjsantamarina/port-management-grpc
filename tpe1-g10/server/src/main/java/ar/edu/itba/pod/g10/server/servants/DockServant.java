package ar.edu.itba.pod.g10.server.servants;

import ar.edu.itba.pod.g10.server.models.Dock;
import ar.edu.itba.pod.g10.server.repositories.DockRespository;
import ar.edu.itba.pod.grpc.dock.AddDockRequest;
import ar.edu.itba.pod.grpc.dock.DockServiceGrpc;
import ar.edu.itba.pod.grpc.dock.DockStatusResponse;
import com.google.protobuf.UInt32Value;
import io.grpc.stub.StreamObserver;

public class DockServant extends DockServiceGrpc.DockServiceImplBase {

    private final DockRespository dockRespository;

    public DockServant(DockRespository dockRespository) {
        this.dockRespository = dockRespository;
    }

    @Override
    public void addDock(AddDockRequest request, StreamObserver<UInt32Value> responseObserver){
        int dockNumber = dockRespository.addDock(request.getDockSize());
        responseObserver.onNext(UInt32Value.of(dockNumber));
        responseObserver.onCompleted();
    }


    @Override
    public void toggleDockStatus(UInt32Value request, StreamObserver<DockStatusResponse> responseObserver){
        Dock dock;
        try {
            dock = dockRespository.toggleDock(request.getValue());
        } catch (Exception e) {
            responseObserver.onError(
                    io.grpc.Status.PERMISSION_DENIED
                            .withDescription(e.getMessage())
                            .asRuntimeException()
            );
            return;
        }
        DockStatusResponse dockStatusResponse = DockStatusResponse.newBuilder()
                .setDockNumber(dock.getNumber())
                .setDockSize(dock.getSize())
                .setDockStatus(dock.getStatus())
                .build();
    }
}
