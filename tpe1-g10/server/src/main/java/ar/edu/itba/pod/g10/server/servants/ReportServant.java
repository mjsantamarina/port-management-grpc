package ar.edu.itba.pod.g10.server.servants;

import ar.edu.itba.pod.g10.server.models.ContainerData;
import ar.edu.itba.pod.g10.server.models.Crane;
import ar.edu.itba.pod.g10.server.models.Ship;
import ar.edu.itba.pod.g10.server.repositories.ContainerRepository;
import ar.edu.itba.pod.g10.server.repositories.CraneRepository;
import ar.edu.itba.pod.g10.server.repositories.ShipRepository;
import ar.edu.itba.pod.grpc.report.*;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;

import java.awt.*;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentLinkedDeque;

public class ReportServant extends ReportServiceGrpc.ReportServiceImplBase {
    private final ShipRepository shipRepository;
    private final CraneRepository craneRepository;
    private final ContainerRepository containerRepository;

    public ReportServant(ShipRepository shipRepository, CraneRepository craneRepository, ContainerRepository containerRepository) {
        this.shipRepository = shipRepository;
        this.craneRepository = craneRepository;
        this.containerRepository = containerRepository;
    }


    @Override
    public void pendingShips(Empty request, StreamObserver<PendingResponse> responseObserver) {
        SortedSet<Ship> pendingShips = shipRepository.getPendingShips();
        PendingResponse.Builder response = PendingResponse.newBuilder();
        for (Ship ship : pendingShips) {
            response.addShips(ShipInfo.newBuilder().setShipId(ship.getId()).setContainers(ship.getContainerAmount())
                    .setSize(ship.getSize()).build());
        }
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }



    @Override
    public void unloadedContainers(UnloadedRequest request, StreamObserver<UnloadedResponse> responseObserver) {
        UnloadedResponse.Builder response = UnloadedResponse.newBuilder();
        if(request.getCrane()==0 && request.getDock()==0){
            ConcurrentLinkedDeque<ContainerData> containers = containerRepository.getContainers();
            for (ContainerData containerData : containers) {
                response.addUnloads(UnloadInfo.newBuilder()
                                .setContainerId(containerData.getId())
                                .setCraneNumber(containerData.getCrane().getId())
                                .setDockNumber(containerData.getDock().getNumber())
                                .setDockSize(containerData.getDock().getSize())
                                .setMaxContainers(containerData.getCrane().getMaxContainers())
                                .setUnloadedSinceMaintenance(containerData.getCurrentContainerAmount())
                        .build());

            }
        }
        else if(request.getCrane()!=0){
            ConcurrentLinkedDeque<ContainerData> containers = containerRepository.getContainersByCrane(request.getCrane());
            for (ContainerData containerData : containers) {
                response.addUnloads(UnloadInfo.newBuilder()
                                .setContainerId(containerData.getId())
                                .setCraneNumber(containerData.getCrane().getId())
                                .setDockNumber(containerData.getDock().getNumber())
                                .setDockSize(containerData.getDock().getSize())
                                .setMaxContainers(containerData.getCrane().getMaxContainers())
                                .setUnloadedSinceMaintenance(containerData.getCurrentContainerAmount())
                        .build());

            }
        }
        else if(request.getDock()!=0){
            ConcurrentLinkedDeque<ContainerData> containers = containerRepository.getContainersByDock(request.getDock());
            for (ContainerData containerData : containers) {
                response.addUnloads(UnloadInfo.newBuilder()
                                .setContainerId(containerData.getId())
                                .setCraneNumber(containerData.getCrane().getId())
                                .setDockNumber(containerData.getDock().getNumber())
                                .setDockSize(containerData.getDock().getSize())
                                .setMaxContainers(containerData.getCrane().getMaxContainers())
                                .setUnloadedSinceMaintenance(containerData.getCurrentContainerAmount())
                        .build());

            }
        }
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }
}