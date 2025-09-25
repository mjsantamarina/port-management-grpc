package ar.edu.itba.pod.g10.server.servants;

import ar.edu.itba.pod.g10.server.models.Crane;
import ar.edu.itba.pod.g10.server.models.Dock;
import ar.edu.itba.pod.g10.server.repositories.CraneRepository;
import ar.edu.itba.pod.g10.server.repositories.NotificationRepository;
import ar.edu.itba.pod.grpc.notify.EventResponse;
import ar.edu.itba.pod.grpc.notify.NotifyServiceGrpc;
import ar.edu.itba.pod.grpc.notify.WatchCraneRequest;
import com.google.protobuf.Empty;
import com.google.protobuf.UInt32Value;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;

public class NotifyServant extends NotifyServiceGrpc.NotifyServiceImplBase {
    private final CraneRepository craneRepository;
    private final NotificationRepository notificationRepository;

    public NotifyServant(CraneRepository craneRepository, NotificationRepository notificationRepository) {
        this.craneRepository = craneRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void watchCrane(WatchCraneRequest request, StreamObserver<EventResponse> responseObserver) {
        Crane crane = craneRepository.getCrane(request.getCraneId());
        if (crane == null) {
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Crane not found").asRuntimeException());
            return;
        }
        Dock dock = crane.getDock();
        if (request.hasDockId() && dock.getNumber() != request.getDockId().getValue()) {
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Crane not in that dock").asRuntimeException());
            return;
        }
        ServerCallStreamObserver<EventResponse> serverObs = (ServerCallStreamObserver<EventResponse>) responseObserver;
        notificationRepository.addObserver(crane.getId(), serverObs);
        serverObs.setOnCancelHandler(() -> notificationRepository.removeObserver(crane.getId(), serverObs));
        serverObs.onNext(EventResponse.newBuilder().setEventMessage(startedWatchLine(crane)).build());
    }

    private static String startedWatchLine(Crane c) {
        Dock d = c.getDock();
        return c.getStatus() + " 🏗️ Crane #" + c.getId() + " (" + c.getCurrentContainers() + "📦) [" +
                c.getMaxContainers() + "] on 🛟 Dock #" + d.getNumber() + " (" +
                d.getSize().name().charAt(0) + ") started watch";
    }

    private static String unloadedLine(Crane c, String containerId) {
        Dock d = c.getDock();
        return "📦 Unloaded " + containerId + " from 🏗️ Crane #" + c.getId() + " (" +
                c.getCurrentContainers() + "📦) [" + c.getMaxContainers() + "] on 🛟 Dock #" +
                d.getNumber() + " (" + d.getSize().name().charAt(0) + ")";
    }

    private static String unloadingEndedLine(Crane c) {
        Dock d = c.getDock();
        return "Unloading Ended on 🛟 Dock #" + d.getNumber() + " (" + d.getSize().name().charAt(0) + ")";
    }

    @Override
    public void stopWatch(UInt32Value request, StreamObserver<Empty> responseObserver) {
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
