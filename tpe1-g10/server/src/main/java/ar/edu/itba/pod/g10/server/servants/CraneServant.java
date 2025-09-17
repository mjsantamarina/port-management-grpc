package ar.edu.itba.pod.g10.server.servants;

import ar.edu.itba.pod.g10.server.models.Crane;
import ar.edu.itba.pod.g10.server.models.Dock;
import ar.edu.itba.pod.g10.server.models.Ship;
import ar.edu.itba.pod.g10.server.repositories.CraneRepository;
import ar.edu.itba.pod.grpc.crane.CraneServiceGrpc;
import ar.edu.itba.pod.grpc.crane.CraneStatusResponse;
import ar.edu.itba.pod.grpc.crane.UnloadContainersRequest;
import ar.edu.itba.pod.grpc.crane.UnloadContainersResponse;
import ar.edu.itba.pod.grpc.utils.CraneStatus;
import com.google.protobuf.UInt32Value;
import io.grpc.stub.StreamObserver;

import javax.print.Doc;

public class CraneServant  extends CraneServiceGrpc.CraneServiceImplBase {

    CraneRepository craneRepository;


    public CraneServant(CraneRepository craneRepository) {
        this.craneRepository = craneRepository;
    }


    //TODO: MANEJAR ERRORES
    @Override
    public StreamObserver<UnloadContainersRequest> unloadContainers(StreamObserver<UnloadContainersResponse> responseObserver) {

        return new StreamObserver<>() {
            @Override
            public void onNext(UnloadContainersRequest unloadContainersRequest) {
                Crane crane = craneRepository.getCrane(unloadContainersRequest.getCraneNumber());
                Dock dock = crane.getDock();
                Ship ship = dock.getShip();
                String containerToUnload = unloadContainersRequest.getContainerId();
                if(ship.peekContainer().equals(containerToUnload) && !crane.needsMainteinance() ){
                    ship.unloadContainer();
                    crane.setCurrentContainers(crane.getCurrentContainers() + 1);
                    responseObserver.onNext(UnloadContainersResponse.newBuilder()
                            .setContainerId(containerToUnload)
                            .setCraneNumber(crane.getId())
                            .build());
                }else if(crane.needsMainteinance() || crane.getStatus().equals(CraneStatus.CRANE_STATUS_CLOSED) ){
                    crane.setCraneStatus(CraneStatus.CRANE_STATUS_CLOSED);
                    responseObserver.onError(new Throwable("Crane needs maintenance"));
                }
                else{
                    responseObserver.onError(new Throwable("Container to unload does not match the one on the ship"));
                }
            }

            @Override
            public void onError(Throwable throwable) {
                responseObserver.onError(throwable);
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
        CraneStatusResponse craneStatusResponse = CraneStatusResponse.newBuilder()
                .setCraneStatus(crane.getStatus())
                .setCraneNumber(crane.getId())
                .build();
        responseObserver.onNext(craneStatusResponse);
        responseObserver.onCompleted();
    }


    @Override
    public void fixCrane(UInt32Value request, StreamObserver<CraneStatusResponse> responseObserver) {
        Crane crane = craneRepository.getCrane(request.getValue());
        if(crane.getStatus().equals(CraneStatus.CRANE_STATUS_CLOSED)){
            crane.setCraneStatus(CraneStatus.CRANE_STATUS_OPEN);
            crane.setCurrentContainers(0);
            CraneStatusResponse craneStatusResponse = CraneStatusResponse.newBuilder()
                    .setCraneStatus(crane.getStatus())
                    .setCraneNumber(crane.getId())
                    .build();
            responseObserver.onNext(craneStatusResponse);
            responseObserver.onCompleted();
        }else {
            responseObserver.onError(new Throwable("Crane is not closed"));
        }
    }
}
