package ar.edu.itba.pod.g10.client;


import ar.edu.itba.pod.grpc.report.*;
import ar.edu.itba.pod.grpc.utils.Size;
import com.google.protobuf.Empty;
import com.google.protobuf.UInt32Value;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ManifestClient {
    private static Logger logger = LoggerFactory.getLogger(ManifestClient.class);
    private final ar.edu.itba.pod.grpc.report.ReportServiceGrpc.ReportServiceBlockingStub reportService;

    public ManifestClient(ManagedChannel channel) {
        this.reportService = ar.edu.itba.pod.grpc.report.ReportServiceGrpc.newBlockingStub(channel);
    }

    public static void main(String[] args) {
        ManifestClientParams params;
        try {
            params = ManifestClientParams.getParams(
                    System.getProperty("serverAddress"),
                    System.getProperty("action"),
                    System.getProperty("outPath"),
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
            logger.info("tpe1-g10 Manifest Client Starting ...");
            ManifestClient client = new ManifestClient(channel);
            client.run(params);
        } finally {
            channel.shutdownNow();
        }
    }

    public void run(ManifestClientParams params) {
        try {
            String outPath = params.getOutPath().orElseThrow(() -> new IllegalArgumentException("outPath required"));
            switch (params.getAction()) {
                case "pending":
                    handlePending(outPath);
                    break;
                case "containers":
                    handleContainers(outPath, params.getCrane(), params.getDock());
                    break;
            }
        } catch (StatusRuntimeException e) {
            System.err.printf("%s: %s%n", e.getStatus().getCode(), e.getStatus().getDescription());
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
    }

    private void handlePending(String outPath) {
        PendingResponse response = reportService.pendingShips(Empty.getDefaultInstance());
        List<ShipInfo> ships = response.getShipsList();
        if (ships.isEmpty()) {
            return; // No crea file
        }
        List<String> lines = ships.stream()
                .map(ship -> "⚓️ " + ship.getShipId() + " (" + sizeAsString(ship.getSize()) + ") (" + ship.getContainers()+ "📦)")
                .collect(Collectors.toList());
        try {
            Files.write(Paths.get(outPath), lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("Error writing file: " + e.getMessage());
        }
    }

    private void handleContainers(String outPath, Optional<Integer> crane, Optional<Integer> dock) {
        UnloadedRequest.Builder builder = UnloadedRequest.newBuilder();

        if(crane.isPresent() && dock.isPresent()) {
            return;
        }

        crane.ifPresent(c -> builder.setCrane(c));
        dock.ifPresent(d -> builder.setDock(d));



        UnloadedResponse response = reportService.unloadedContainers(builder.build());
        List<UnloadInfo> unloads = response.getUnloadsList();
        if (unloads.isEmpty()) {
            return; // No crea file
        }
        List<String> lines = unloads.stream()
                .map(unload -> "📦 " + unload.getContainerId() + " unloaded from 🏗️ Crane #" + unload.getCraneNumber() + " (" + unload.getUnloadedSinceMaintenance() + "📦) [" + unload.getMaxContainers() + "] on 🛟 Dock #" + unload.getDockNumber() + " (" + sizeAsString(unload.getDockSize()) + ")")
                .collect(Collectors.toList());
        try {
            Files.write(Paths.get(outPath), lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("Error writing file: " + e.getMessage());
        }
    }

    private String sizeAsString(Size size) {
        if (size == Size.SIZE_SMALL) return "S";
        if (size == Size.SIZE_MEDIUM) return "M";
        if (size == Size.SIZE_LARGE) return "L";
        return "";
    }
}