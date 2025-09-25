package ar.edu.itba.pod.g10.client;

import ar.edu.itba.pod.grpc.notify.*;
import com.google.protobuf.UInt32Value;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CountDownLatch;

public class WatchClient {
    private static Logger logger = LoggerFactory.getLogger(WatchClient.class);
    private final NotifyServiceGrpc.NotifyServiceBlockingStub blockingStub;
    private final NotifyServiceGrpc.NotifyServiceStub asyncStub;

    public WatchClient(ManagedChannel channel) {
        this.blockingStub = NotifyServiceGrpc.newBlockingStub(channel);
        this.asyncStub = NotifyServiceGrpc.newStub(channel);
    }


    public static void main(String[] args) throws InterruptedException {
        WatchClientParams params;
        try {
            params = WatchClientParams.getParams(
                    System.getProperty("serverAddress"),
                    System.getProperty("action"),
                    System.getProperty("crane"),
                    System.getProperty("dock")
            );
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(1);
            return;
        }

        ManagedChannel channel = ManagedChannelBuilder.forAddress(params.getAddress(), Integer.parseInt(params.getPort()))
                .usePlaintext()
                .build();

        try {
            logger.info("tpe1-g10 Watch Client Starting ...");
            WatchClient client = new WatchClient(channel);
            client.run(params);
        } finally {
            channel.shutdownNow();
        }
    }


    public void run(WatchClientParams params) throws InterruptedException {
        try {
            switch (params.getAction()) {
                case "watchCrane":
                    handleWatchCrane(params.getCraneNumber().orElseThrow(), params.getDockNumber().orElseThrow());
                    break;
                case "stopWatch":
                    handleStopWatch(params.getCraneNumber().orElseThrow());
                    break;
            }
        } catch (StatusRuntimeException e) {
            System.err.printf("%s: %s%n", e.getStatus().getCode(), e.getStatus().getDescription());
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
    }

    private void handleWatchCrane(int craneNumber, int dockNumber) throws InterruptedException {
        WatchCraneRequest request = WatchCraneRequest.newBuilder()
                .setCraneId(craneNumber)
                .setDockId(UInt32Value.of(dockNumber)) // Assuming dock ID is 1 for simplicity; adjust as needed
                .build();
        CountDownLatch latch = new CountDownLatch(1);
        asyncStub.watchCrane(request, new StreamObserver<EventResponse>() {
            @Override
            public void onNext(EventResponse response) {
                System.out.println(response.getEventMessage());
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Error: " + t.getMessage());
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                latch.countDown();
            }
        });
        latch.await();
    }

    private void handleStopWatch(int craneNumber) {
        UInt32Value request = UInt32Value.of(craneNumber);
        blockingStub.stopWatch(request);
        System.out.println("Notificaciones detenidas para grúa #" + craneNumber);
    }
}