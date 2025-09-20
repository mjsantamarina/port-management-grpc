package ar.edu.itba.pod.g10.client;

import ar.edu.itba.pod.grpc.crane.*;
import ar.edu.itba.pod.grpc.utils.CraneStatus;
import com.google.protobuf.UInt32Value;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CraneClient {
    private static Logger logger = LoggerFactory.getLogger(CraneClient.class);
    private final CraneServiceGrpc.CraneServiceBlockingStub blockingStub;
    private final CraneServiceGrpc.CraneServiceStub asyncStub;

    public CraneClient(ManagedChannel channel) {
        this.blockingStub = CraneServiceGrpc.newBlockingStub(channel);
        this.asyncStub = CraneServiceGrpc.newStub(channel);
    }

    public static void main(String[] args) throws InterruptedException {
        CraneClientParams params;
        try {
            params = CraneClientParams.getParams(
                    System.getProperty("serverAddress"),
                    System.getProperty("action"),
                    System.getProperty("crane"),
                    System.getProperty("container")
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
            logger.info("tpe1-g10 Crane Client Starting ...");
            CraneClient client = new CraneClient(channel);
            client.run(params);
        } finally {
            channel.shutdownNow();
        }
    }

    public void run(CraneClientParams params) throws InterruptedException {
        try {
            switch (params.getAction()) {
                case "unload":
                    handleUnload(params.getCraneNumber().orElseThrow(), params.getContainer().orElseThrow());
                    break;
                case "checkCrane":
                    handleCheckCrane(params.getCraneNumber().orElseThrow());
                    break;
                case "fixCrane":
                    handleFixCrane(params.getCraneNumber().orElseThrow());
                    break;
            }
        } catch (StatusRuntimeException e) {
            System.err.printf("%s: %s%n", e.getStatus().getCode(), e.getStatus().getDescription());
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
    }

    private void handleUnload(int craneNumber, String firstContainer) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        StreamObserver<UnloadContainersRequest> requestObserver = asyncStub.unloadContainers(new StreamObserver<UnloadContainersResponse>() {
            @Override
            public void onNext(UnloadContainersResponse response) {
                System.out.println("📦 Unloaded " + response.getContainerId() + " from 🏗️ Crane #" + response.getCraneNumber());
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Error: " + t.getMessage());
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                System.out.println("Unloading Ended");
                latch.countDown();
            }
        });

        // Send first container
        requestObserver.onNext(UnloadContainersRequest.newBuilder().setCraneNumber(craneNumber).setContainerId(firstContainer).build());

        // Interactive loop
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("Enter Container ID: ");
            String id = scanner.nextLine().trim();
            if (id.isEmpty()) continue;
            requestObserver.onNext(UnloadContainersRequest.newBuilder().setCraneNumber(craneNumber).setContainerId(id).build());
            // Server calls onCompleted when end
        }
    }

    private void handleCheckCrane(int craneNumber) {
        CraneStatusResponse response = blockingStub.getCraneStatus(UInt32Value.of(craneNumber));
        System.out.println(craneStatusAsString(response.getCraneStatus()) + " 🏗️ Crane #" + response.getCraneNumber());
    }

    private void handleFixCrane(int craneNumber) {
        CraneStatusResponse response = blockingStub.fixCrane(UInt32Value.of(craneNumber));
        System.out.println("OPEN 🏗️ Crane #" + response.getCraneNumber() + " (0📦) [" + "max" + "] on 🛟 Dock # " + "dock" + " (S)");
    }

    private String craneStatusAsString(CraneStatus status) {
        if (status == CraneStatus.CRANE_STATUS_UNLOADING) return "UNLOADING";
        if (status == CraneStatus.CRANE_STATUS_CLOSED) return "CLOSED";
        if (status == CraneStatus.CRANE_STATUS_OPEN) return "OPEN";
        return "";
    }
}