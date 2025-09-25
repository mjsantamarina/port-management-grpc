package ar.edu.itba.pod.g10.server.repositories;

import ar.edu.itba.pod.g10.server.models.Crane;
import ar.edu.itba.pod.g10.server.models.Dock;
import ar.edu.itba.pod.g10.server.models.Ship;
import ar.edu.itba.pod.grpc.notify.EventResponse;
import io.grpc.stub.ServerCallStreamObserver;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class NotificationRepository {

    private final ConcurrentHashMap<Integer, CopyOnWriteArraySet<ServerCallStreamObserver<EventResponse>>> observersByCrane =
            new ConcurrentHashMap<>();

    public void addObserver(int craneId, ServerCallStreamObserver<EventResponse> obs) {
        observersByCrane.computeIfAbsent(craneId, k -> new CopyOnWriteArraySet<>()).add(obs);
    }

    public void removeObserver(int craneId, ServerCallStreamObserver<EventResponse> obs) {
        Set<ServerCallStreamObserver<EventResponse>> set = observersByCrane.get(craneId);
        if (set != null) {
            set.remove(obs);
            if (set.isEmpty()) observersByCrane.remove(craneId, set);
        }
    }

    public void publishToCrane(int craneId, String line) {
        Set<ServerCallStreamObserver<EventResponse>> set = observersByCrane.get(craneId);
        if (set == null || set.isEmpty()) return;
        EventResponse ev = EventResponse.newBuilder().setEventMessage(line).build();
        for (ServerCallStreamObserver<EventResponse> s : set) {
            if (!s.isCancelled()) s.onNext(ev);
        }
    }

    public void publishToCranes(Collection<Integer> craneIds, String line) {
        if (craneIds == null || craneIds.isEmpty()) return;
        for (Integer id : craneIds) {
            if (id != null) publishToCrane(id, line);
        }
    }

    public void publishStartedWatch(Crane c) {
        publishToCrane(c.getId(), lineStartedWatch(c));
    }

    public void publishCraneStatus(Crane c) {
        publishToCrane(c.getId(), lineCraneStatus(c));
    }

    public void publishUnloaded(Crane c, String containerId) {
        publishToCrane(c.getId(), lineUnloaded(c, containerId));
    }

    public void publishUnloadingEnded(Crane c) {
        publishToCrane(c.getId(), lineUnloadingEnded(c));
    }

    public void publishDockClosed(Dock d, Collection<Integer> craneIds) {
        publishToCranes(craneIds, lineDockClosed(d));
    }

    public void publishDockOpened(Dock d, Collection<Integer> craneIds) {
        publishToCranes(craneIds, lineDockOpened(d));
    }

    public void publishDockOccupied(Dock d, Ship s, Collection<Integer> craneIds) {
        publishToCranes(craneIds, lineDockOccupied(d, s));
    }

    public void publishShipLeft(Dock d, Ship s, Collection<Integer> craneIds) {
        publishToCranes(craneIds, lineShipLeft(d, s));
    }

    private static String lineStartedWatch(Crane c) {
        Dock d = c.getDock();
        return c.getStatus() + " 🏗️ Crane #" + c.getId() + " (" + c.getCurrentContainers() + "📦) [" +
                c.getMaxContainers() + "] on 🛟 Dock #" + d.getNumber() + " (" +
                d.getSize().name().charAt(0) + ") started watch";
    }

    private static String lineCraneStatus(Crane c) {
        Dock d = c.getDock();
        return c.getStatus() + " 🏗️ Crane #" + c.getId() + " (" + c.getCurrentContainers() + "📦) [" +
                c.getMaxContainers() + "] on 🛟 Dock #" + d.getNumber() + " (" +
                d.getSize().name().charAt(0) + ")";
    }

    private static String lineUnloaded(Crane c, String containerId) {
        Dock d = c.getDock();
        return "📦 Unloaded " + containerId + " from 🏗️ Crane #" + c.getId() + " (" +
                c.getCurrentContainers() + "📦) [" + c.getMaxContainers() + "] on 🛟 Dock #" +
                d.getNumber() + " (" + d.getSize().name().charAt(0) + ")";
    }

    private static String lineUnloadingEnded(Crane c) {
        Dock d = c.getDock();
        return "Unloading Ended on 🛟 Dock #" + d.getNumber() + " (" + d.getSize().name().charAt(0) + ")";
    }

    private static String lineDockClosed(Dock d) {
        return "CLOSED 🛟 Dock #" + d.getNumber() + " (" + d.getSize().name().charAt(0) + ")";
    }

    private static String lineDockOpened(Dock d) {
        return "OPEN 🛟 Dock #" + d.getNumber() + " (" + d.getSize().name().charAt(0) + ")";
    }

    private static String lineDockOccupied(Dock d, Ship s) {
        return "⚓ " + s.getId() + " (" + s.getSize().name().charAt(0) + ") (" + s.getContainerAmount() +
                "📦) in 🛟 Dock #" + d.getNumber() + " (" + d.getSize().name().charAt(0) + ")";
    }

    private static String lineShipLeft(Dock d, Ship s) {
        return "⚓ " + s.getId() + " (" + s.getSize().name().charAt(0) + ") (" + s.getContainerAmount() +
                "📦) left 🛟 Dock #" + d.getNumber() + " (" + d.getSize().name().charAt(0) + ")";
    }
}
