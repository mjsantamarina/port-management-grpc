package ar.edu.itba.pod.g10.server.models;

import javax.print.Doc;
import java.awt.*;

public class ContainerData {
    private String id;
    private Crane crane;
    private int currentContainerAmount = 0;
    private Dock dock;

    public ContainerData(String id, Crane crane, Dock dock) {
        this.id = id;
        this.crane = crane;
        this.dock = dock;
        this.currentContainerAmount = crane.getCurrentContainers();
    }

    public String getId() {
        return id;
    }

    public Crane getCrane() {
        return crane;
    }

    public int getCurrentContainerAmount() {
        return currentContainerAmount;
    }

    public Dock getDock() {
        return dock;
    }
}
