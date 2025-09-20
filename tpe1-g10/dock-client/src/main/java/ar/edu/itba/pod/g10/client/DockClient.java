package ar.edu.itba.pod.g10.client;

import ar.edu.itba.pod.grpc.dock.*;
import ar.edu.itba.pod.grpc.utils.Size;
import ar.edu.itba.pod.grpc.utils.DockStatus;
import com.google.protobuf.UInt32Value;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DockClient {
    private static Logger logger = LoggerFactory.getLogger(DockClient.class);
    private final DockServiceGrpc.DockServiceBlockingStub dockService;

    public DockClient(ManagedChannel channel) {
        this.dockService = DockServiceGrpc.newBlockingStub(channel);
    }

    public static void main(String[] args) {
        DockClientParams params;
        try {
            params = DockClientParams.getParams(
                    System.getProperty("serverAddress"),
                    System.getProperty("action"),
                    System.getProperty("size"),
                    System.getProperty("dock"),
                    System.getProperty("maxContainers")
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
            logger.info("tpe1-g10 Dock Client Starting ...");
            DockClient client = new DockClient(channel);
            client.run(params);
        } finally {
            channel.shutdownNow();
        }
    }

    public void run(DockClientParams params) {
        try {
            switch (params.getAction()) {
                case "addDock":
                    handleAddDock(params.getSize().orElseThrow());
                    break;
                case "addCrane":
                    handleAddCrane(params.getDockNumber().orElseThrow(), params.getContainerAmount().orElseThrow());
                    break;
                case "toggleDock":
                    handleToggleDock(params.getDockNumber().orElseThrow());
                    break;
            }
        } catch (StatusRuntimeException e) {
            System.err.printf("%s: %s%n", e.getStatus().getCode(), e.getStatus().getDescription());
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
    }

    private void handleAddDock(Size size) {
        AddDockResponse response = dockService.addDock(AddDockRequest.newBuilder().setDockSize(size).build());
        System.out.println("Added 🛟 Dock #" + response.getDockNumber() + " (" + sizeAsString(size) + ")");
    }

    private void handleAddCrane(int dockNumber, int containerAmount) {
        AddCraneToDockResponse response = dockService.addCraneToDock(AddCraneToDockRequest.newBuilder()
                .setDockNumber(dockNumber)
                .setContainerAmount(containerAmount)
                .build());
        System.out.println("Added 🏗️ Crane #" + response.getCraneNumber() + " (" + response.getLastContainerAmount() + "📦) [" + response.getProvidedContainerAmount() + "] on 🛟 Dock #" + dockNumber + " (" + sizeAsString(response.getStatus().getDockSize()) + ")");
    }

    private void handleToggleDock(int dockNumber) {
        DockStatusResponse response = dockService.toggleDockStatus(UInt32Value.of(dockNumber));
        System.out.println(dockStatusAsString(response.getDockStatus()) + " 🛟 Dock #" + response.getDockNumber() + " (" + sizeAsString(response.getDockSize()) + ")");
    }

    private String sizeAsString(Size size) {
        return switch (size) {
            case SIZE_SMALL -> "S";
            case SIZE_MEDIUM -> "M";
            case SIZE_LARGE -> "L";
            default -> "";
        };
    }

    private String dockStatusAsString(DockStatus status) {
        return switch (status) {
            case DOCK_STATUS_IDLE -> "OPEN";
            case DOCK_STATUS_CLOSED -> "CLOSED";
            default -> "";
        };
    }
}
