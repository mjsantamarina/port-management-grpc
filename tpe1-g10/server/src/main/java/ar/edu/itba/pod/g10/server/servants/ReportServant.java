package ar.edu.itba.pod.g10.server.servants;

import ar.edu.itba.pod.g10.server.models.Ship;
import ar.edu.itba.pod.g10.server.repositories.CraneRepository;
import ar.edu.itba.pod.g10.server.repositories.ShipRepository;
import ar.edu.itba.pod.grpc.report.*;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;

import java.util.List;
import java.util.SortedSet;

public class ReportServant extends ReportServiceGrpc.ReportServiceImplBase {
    private final ShipRepository shipRepository;
    private final CraneRepository craneRepository;

    public ReportServant(ShipRepository shipRepository, CraneRepository craneRepository) {
        this.shipRepository = shipRepository;
        this.craneRepository = craneRepository;
    }


    @Override
    public void pendingShips(Empty request, StreamObserver<PendingResponse> responseObserver) {
        SortedSet<Ship> pendingShips = shipRepository.getPendingShips();
        PendingResponse.Builder response = PendingResponse.newBuilder();
        for (Ship ship : pendingShips) {
            response.addShips(ShipInfo.newBuilder().setShipId(ship.getId()).setContainers(ship.getContainerAmount())
                    .setSize(ship.getSize()).build());
        }
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }



    @Override
    public void unloadedContainers(UnloadedRequest request, StreamObserver<UnloadedResponse> responseObserver) {
        // Placeholder: Lista unloaded con filters
        responseObserver.onNext(UnloadedResponse.newBuilder().build());
        responseObserver.onCompleted();
    }
}