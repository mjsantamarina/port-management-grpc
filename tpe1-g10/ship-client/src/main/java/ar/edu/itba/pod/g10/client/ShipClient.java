package ar.edu.itba.pod.g10.client;

import ar.edu.itba.pod.grpc.ship.*;
import ar.edu.itba.pod.grpc.dock.*;
import ar.edu.itba.pod.grpc.utils.Size;
import ar.edu.itba.pod.grpc.utils.DockStatus;
import com.google.protobuf.UInt32Value;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

public class ShipClient {
    private static Logger logger = LoggerFactory.getLogger(ShipClient.class);
    private final ShipServiceGrpc.ShipServiceBlockingStub blockingStub;
    private final ShipServiceGrpc.ShipServiceStub asyncStub;

    public ShipClient(ManagedChannel channel) {
        this.blockingStub = ShipServiceGrpc.newBlockingStub(channel);
        this.asyncStub = ShipServiceGrpc.newStub(channel);
    }

    public static void main(String[] args) throws InterruptedException {
        ShipClientParams params;
        try {
            params = ShipClientParams.getParams(
                    System.getProperty("serverAddress"),
                    System.getProperty("action"),
                    System.getProperty("id"),
                    System.getProperty("size"),
                    System.getProperty("containers"),
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
            logger.info("tpe1-g10 Ship Client Starting ...");
            ShipClient client = new ShipClient(channel);
            client.run(params);
        } finally {
            channel.shutdownNow();
        }
    }

    public void run(ShipClientParams params) throws InterruptedException {
        try {
            switch (params.getAction()) {
                case "requestDock":
                    handleRequestDock(params.getId().orElseThrow(), params.getSize().orElseThrow(), params.getContainers().orElseThrow());
                    break;
                case "checkDock":
                    handleCheckDock(params.getDockNumber().orElseThrow());
                    break;
                case "leaveDock":
                    handleLeaveDock(params.getId().orElseThrow(), params.getDockNumber().orElseThrow());
                    break;
            }
        } catch (StatusRuntimeException e) {
            System.err.printf("%s: %s%n", e.getStatus().getCode(), e.getStatus().getDescription());
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
    }

    private void handleRequestDock(String id, Size size, String containers) throws InterruptedException {
        DockRequest.Builder builder = DockRequest.newBuilder();
        builder.setShipId(id);
        builder.setShipSize(size);
        builder.setShipContainers(containers);
        DockRequest request = builder.build();
        CountDownLatch latch = new CountDownLatch(1);
        asyncStub.requestDock(request, new StreamObserver<DockResponse>() {
            @Override
            public void onNext(DockResponse response) {
                int ahead = response.getShipsAhead();
                String sizeStr = sizeAsString(size);
                int conts = request.getShipContainers().split("#").length;
                if (ahead > 0) {
                    System.out.println("⚓ " + id + " (" + sizeStr + ") (" + conts + "📦) is waiting for a dock with " + ahead + " ships ahead");
                } else if (response.getDockNumber() == 0) {
                    System.out.println("⚓ " + id + " (" + sizeStr + ") (" + conts + "📦) is waiting for a dock with 0 ships ahead");
                } else {
                    System.out.println("⚓ " + id + " (" + sizeStr + ") (" + conts + "📦) in 🛟 Dock #" + response.getDockNumber() + " (" + sizeAsString(response.getDockSize()) + ")");
                }
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

    private void handleCheckDock(int dockNumber) {
        UInt32Value request = UInt32Value.of(dockNumber);
        DockStatusResponse response = blockingStub.getDockStatus(request);
        String statusStr = dockStatusAsString(response.getDockStatus());
        String sizeStr = sizeAsString(response.getDockSize());
        System.out.println(statusStr + " 🛟 Dock #" + response.getDockNumber() + " (" + sizeStr + ")"); // No ship info in proto, so only status
    }

    private void handleLeaveDock(String id, int dockNumber) {
        LeaveDockRequest.Builder builder = LeaveDockRequest.newBuilder();
        builder.setShipId(id);
        builder.setDockNumber(dockNumber);
        LeaveDockResponse response = blockingStub.leaveDock(builder.build());
        System.out.println("⚓ " + response.getShipId() + " (" + sizeAsString(response.getShipSize()) + ") (0📦) left 🛟 Dock #" + response.getDockNumber() + " (" + sizeAsString(response.getDockSize()) + ")");
    }

    private String sizeAsString(Size size) {
        if (size == Size.SIZE_SMALL) return "S";
        if (size == Size.SIZE_MEDIUM) return "M";
        if (size == Size.SIZE_LARGE) return "L";
        return "";
    }

    private String dockStatusAsString(DockStatus status) {
        if (status == DockStatus.DOCK_STATUS_IDLE) return "IDLE";
        if (status == DockStatus.DOCK_STATUS_CLOSED) return "CLOSED";
        if (status == DockStatus.DOCK_STATUS_BUSY) return "BUSY";
        return "";
    }
}