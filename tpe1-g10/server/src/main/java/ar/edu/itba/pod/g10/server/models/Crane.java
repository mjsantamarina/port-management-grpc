package ar.edu.itba.pod.g10.server.models;

import ar.edu.itba.pod.grpc.utils.CraneStatus;

import javax.print.Doc;

public class Crane {
    private final int id;
    private CraneStatus craneStatus;
    private Dock dock;
    private int maxContainers;
    private int currentContainers;

    public Crane(int id, Dock dock, int maxContainers) {
        this.id = id;
        craneStatus = CraneStatus.CRANE_STATUS_OPEN;
        this.dock = dock;
        this.maxContainers = maxContainers;
        this.currentContainers = 0;
    }

    public int getId() {
        return id;
    }


    public Dock getDock() {
        return dock;
    }

    public boolean needsMainteinance() {
        return  currentContainers >= maxContainers;
    }

    public int getCurrentContainers() {
        return currentContainers;
    }

    public void setCurrentContainers(int currentContainers) {
        this.currentContainers = currentContainers;
    }

    public void setCraneStatus(CraneStatus craneStatus) {
        this.craneStatus = craneStatus;
    }

    public CraneStatus getStatus() {
        return craneStatus;
    }
}
