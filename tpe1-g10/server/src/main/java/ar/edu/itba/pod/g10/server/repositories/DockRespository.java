package ar.edu.itba.pod.g10.server.repositories;

import ar.edu.itba.pod.grpc.dock.AddDockRequest;
import ar.edu.itba.pod.grpc.utils.Size;
import com.google.protobuf.UInt32Value;
import io.grpc.stub.StreamObserver;

import java.util.Deque;
import java.util.stream.Stream;

public class DockRespository {
    private int dockAmmount;
    private final ConcurrentMap<Size, Deque<Dock>> dockBySize;

}
