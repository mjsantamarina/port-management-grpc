package ar.edu.itba.pod.g10.server.servants;

import ar.edu.itba.pod.g10.server.repositories.DockRespository;
import ar.edu.itba.pod.grpc.dock.AddDockRequest;
import ar.edu.itba.pod.grpc.dock.DockServiceGrpc;
import com.google.protobuf.UInt32Value;
import io.grpc.stub.StreamObserver;

public class DockServant extends DockServiceGrpc.DockServiceImplBase {

    @Override
    public void addDock(AddDockRequest request, StreamObserver<UInt32Value> responseObserver){
        int dockNumber = DockRespository.addDock(request.getDockSize());
        responseObserver.onNext(UInt32Value.of(dockNumber));
        responseObserver.onCompleted();
    }
}
