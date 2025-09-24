package ar.edu.itba.pod.g10.server.servants;

import ar.edu.itba.pod.g10.server.locks.Locks;
import ar.edu.itba.pod.g10.server.models.Crane;
import ar.edu.itba.pod.g10.server.models.Dock;
import ar.edu.itba.pod.g10.server.repositories.CraneRepository;
import ar.edu.itba.pod.grpc.notify.NotifyServiceGrpc;
import ar.edu.itba.pod.grpc.notify.EventResponse; // Agrega esta línea
import ar.edu.itba.pod.grpc.notify.WatchCraneRequest;
import com.google.protobuf.Empty;
import com.google.protobuf.UInt32Value;
import io.grpc.stub.StreamObserver;

import javax.print.Doc;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class NotifyServant extends NotifyServiceGrpc.NotifyServiceImplBase {
    private final CraneRepository craneRepository;
    private final Condition notifyChanged= Locks.NOTIFIABLE_CHANGED;


    public NotifyServant(CraneRepository craneRepository) {
        this.craneRepository = craneRepository;
    }

    @Override
    public void watchCrane(WatchCraneRequest request, StreamObserver<EventResponse> responseObserver) {
        // Placeholder: Implementa streaming de eventos para la grúa


        Crane crane = craneRepository.getCrane(request.getCraneId());
        Dock dock = crane.getDock();

        if(dock.getNumber() != request.getDockId().getValue()){
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Crane not in that dock").asRuntimeException());
        }

        if (crane == null) {
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Crane not found").asRuntimeException());
            return;
        }

        BlockingQueue<String> notificationQueue = new LinkedBlockingQueue<>();


        responseObserver.onNext(EventResponse.newBuilder().setEventMessage( startedWatchLine(crane)).build());

        try{
            while(true){
                String notif = notificationQueue.take();
                responseObserver.onNext(EventResponse.newBuilder().setEventMessage(notif).build());

            }

        }catch (InterruptedException e){

        }finally {
            responseObserver.onCompleted();
        }


        //responseObserver.onNext(EventResponse.newBuilder().setEventMessage("Placeholder event").build());
        responseObserver.onCompleted();
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
        // Placeholder: Detiene notifs
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}