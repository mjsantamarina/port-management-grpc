package ar.edu.itba.pod.g10.server.servants;

import ar.edu.itba.pod.g10.server.repositories.CraneRepository;
import ar.edu.itba.pod.g10.server.repositories.ShipRepository;
import ar.edu.itba.pod.grpc.report.ReportServiceGrpc;
import ar.edu.itba.pod.grpc.report.PendingResponse;
import ar.edu.itba.pod.grpc.report.UnloadedResponse;
import ar.edu.itba.pod.grpc.report.UnloadedRequest;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;

public class ReportServant extends ReportServiceGrpc.ReportServiceImplBase {
    private final ShipRepository shipRepository;
    private final CraneRepository craneRepository;

    public ReportServant(ShipRepository shipRepository, CraneRepository craneRepository) {
        this.shipRepository = shipRepository;
        this.craneRepository = craneRepository;
    }

    @Override
    public void pendingShips(Empty request, StreamObserver<PendingResponse> responseObserver) {
        // Placeholder: Lista ships pendientes
        responseObserver.onNext(PendingResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void unloadedContainers(UnloadedRequest request, StreamObserver<UnloadedResponse> responseObserver) {
        // Placeholder: Lista unloaded con filters
        responseObserver.onNext(UnloadedResponse.newBuilder().build());
        responseObserver.onCompleted();
    }
}