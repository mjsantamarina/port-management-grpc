package ar.edu.itba.pod.g10.server;

import ar.edu.itba.pod.g10.server.repositories.CraneRepository;
import ar.edu.itba.pod.g10.server.repositories.DockRepository;
import ar.edu.itba.pod.g10.server.repositories.NotificationRepository;
import ar.edu.itba.pod.g10.server.repositories.ShipRepository;
import ar.edu.itba.pod.g10.server.servants.CraneServant;
import ar.edu.itba.pod.g10.server.servants.DockServant;
import ar.edu.itba.pod.g10.server.servants.NotifyServant;
import ar.edu.itba.pod.g10.server.servants.ReportServant;
import ar.edu.itba.pod.g10.server.servants.ShipServant;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Server {
    private static Logger logger = LoggerFactory.getLogger(Server.class);

    public static void main(String[] args) throws InterruptedException, IOException {
        logger.info(" Server Starting ...");
        DockRepository dockRepository = new DockRepository();
        ShipRepository shipRepository = new ShipRepository();
        CraneRepository craneRepository = new CraneRepository();
        NotificationRepository notificationRepository = new NotificationRepository();
        int port = 50051;
        io.grpc.Server server = ServerBuilder.forPort(port)
                .addService(new DockServant(dockRepository, craneRepository, notificationRepository))
                .addService(new ShipServant(shipRepository, dockRepository, notificationRepository))
                .addService(new CraneServant(craneRepository, notificationRepository))
                .addService(new NotifyServant(craneRepository,notificationRepository))
                .addService(new ReportServant(shipRepository, craneRepository))
                .build();
        server.start();
        logger.info("Server started, listening on " + port);
        server.awaitTermination();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down gRPC server since JVM is shutting down");
            server.shutdown();
            logger.info("Server shut down");
        }));
    }
}