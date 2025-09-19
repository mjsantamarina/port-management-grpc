package ar.edu.itba.pod.g10.server.servants;

import ar.edu.itba.pod.g10.server.repositories.CraneRepository;
import ar.edu.itba.pod.grpc.notify.NotifyServiceGrpc;
import ar.edu.itba.pod.grpc.notify.EventResponse; // Agrega esta línea
import com.google.protobuf.Empty;
import com.google.protobuf.UInt32Value;
import io.grpc.stub.StreamObserver;

public class NotifyServant extends NotifyServiceGrpc.NotifyServiceImplBase {
    private final CraneRepository craneRepository;

    public NotifyServant(CraneRepository craneRepository) {
        this.craneRepository = craneRepository;
    }

    @Override
    public void watchCrane(UInt32Value request, StreamObserver<EventResponse> responseObserver) {
        // Placeholder: Implementa streaming de eventos para la grúa
        responseObserver.onNext(EventResponse.newBuilder().setEventMessage("Placeholder event").build());
        responseObserver.onCompleted();
    }

    @Override
    public void stopWatch(UInt32Value request, StreamObserver<Empty> responseObserver) {
        // Placeholder: Detiene notifs
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}