package ar.edu.itba.pod.g10.server.repositories;

import ar.edu.itba.pod.g10.server.models.Dock;
import ar.edu.itba.pod.grpc.dock.AddDockRequest;
import ar.edu.itba.pod.grpc.utils.DockStatus;
import ar.edu.itba.pod.grpc.utils.Size;
import com.google.protobuf.UInt32Value;
import io.grpc.stub.StreamObserver;

import java.util.Collection;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

public class DockRespository {
    private int dockAmmount;
    private final ConcurrentMap<Size, Deque<Dock>> docksBySize;

    public DockRespository() {
        this.dockAmmount = 0;
        this.docksBySize = new ConcurrentHashMap<>();
    }

    public synchronized int addDock(Size dockSize) {
        dockAmmount++;
        Dock toAdd = new Dock(dockAmmount, dockSize, DockStatus.DOCK_STATUS_IDLE);
        this.docksBySize.get(dockSize).add(toAdd);
        return dockAmmount;
    }

    public Dock toggleDock(int dockNumber) {
        Optional<Dock> maybeDock = getDockByNumber(dockNumber);
        if(maybeDock.isEmpty()){
            //TODO hacer la excepcion
        }
        Dock dock = maybeDock.get();
        //TODO Lockear esta funcion y hacer los locks
        try {
            switch (dock.getStatus()) {
                case DOCK_STATUS_BUSY -> throw new IllegalStateException("El dock ya esta ocupado");
                //TODO ver msj de error
                case DOCK_STATUS_CLOSED -> dock.setStatus(DockStatus.DOCK_STATUS_IDLE);
                case DOCK_STATUS_IDLE -> dock.setStatus(DockStatus.DOCK_STATUS_CLOSED);
            }
            return dock;
        } finally {
            return dock;
        }
    }

    public Optional<Dock> getDockByNumber(int dockNumber) {
        return docksBySize.values().stream().flatMap(Collection::stream)
                .filter(dock -> dock.getNumber() == dockNumber).findFirst();
    }

}
