package ar.edu.itba.pod.g10.client;

import java.util.List;
import java.util.Optional;

public class WatchClientParams {
    private String address;
    private String port;
    private String action;
    private static final List<String> VALID_ACTIONS = List.of("watchCrane", "stop");

    private Optional<Integer> craneNumber;
    private Optional<Integer> dockNumber;

    private WatchClientParams(String address, String port, String action, Optional<Integer> craneNumber, Optional<Integer> dockNumber) {
        this.address = address;
        this.port = port;
        this.action = action;
        this.craneNumber = craneNumber;
        this.dockNumber = dockNumber;
    }

    public static WatchClientParams getParams(String addressAndPort, String action, String crane, String dock) {
        if (addressAndPort == null || addressAndPort.isEmpty()) {
            throw new IllegalArgumentException("A server address must be provided.");
        }

        if (action == null || action.isEmpty() || !VALID_ACTIONS.contains(action)) {
            throw new IllegalArgumentException("The action provided must be one of the following: \"watchCrane\" or \"stop\".");
        }

        List<String> addressAndPortParts = List.of(addressAndPort.split(":"));
        return new WatchClientParams(
                addressAndPortParts.get(0),
                addressAndPortParts.size() > 1 ? addressAndPortParts.get(1) : "50051",
                action,
                Optional.ofNullable(crane).map(Integer::parseInt),
                Optional.ofNullable(dock).map(Integer::parseInt)
        );
    }

    public String getAddress() {
        return address;
    }

    public String getPort() {
        return port;
    }

    public String getAction() {
        return action;
    }

    public Optional<Integer> getCraneNumber() {
        return craneNumber;
    }

    public Optional<Integer> getDockNumber() {
        return dockNumber;
    }
}