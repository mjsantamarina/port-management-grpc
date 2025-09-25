package ar.edu.itba.pod.g10.server.repositories;


import ar.edu.itba.pod.g10.server.models.ContainerData;
import ar.edu.itba.pod.g10.server.models.Crane;
import ar.edu.itba.pod.g10.server.models.Dock;

import java.util.concurrent.ConcurrentLinkedDeque;

public class ContainerRepository {
    ConcurrentLinkedDeque<ContainerData> containers = new ConcurrentLinkedDeque<>();

    public void addContainer(String container, Dock dock, Crane crane) {
        containers.add(new ContainerData(container, crane, dock));
    }

    public ConcurrentLinkedDeque<ContainerData> getContainers() {
        return containers;
    }

    public ConcurrentLinkedDeque<ContainerData> getContainersByDock(int dockNumber) {
        ConcurrentLinkedDeque<ContainerData> result = new ConcurrentLinkedDeque<>();
        for (ContainerData container : containers) {
            if (container.getDock().getNumber() == dockNumber) {
                result.add(container);
            }
        }
        return result;
    }

    public ConcurrentLinkedDeque<ContainerData> getContainersByCrane(int crane) {
        ConcurrentLinkedDeque<ContainerData> result = new ConcurrentLinkedDeque<>();
        for (ContainerData container : containers) {
            if (container.getCrane().getId() == crane) {
                result.add(container);
            }
        }
        return result;
    }
}
