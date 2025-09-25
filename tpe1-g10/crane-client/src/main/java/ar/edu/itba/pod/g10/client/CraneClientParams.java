package ar.edu.itba.pod.g10.client;

import java.util.List;
import java.util.Optional;

public class CraneClientParams {
    private String address;
    private String port;
    private String action;
    private static final List<String> VALID_ACTIONS = List.of("unload", "checkCrane", "fixCrane");

    private Optional<Integer> craneNumber;
    private Optional<String> container;

    private CraneClientParams(String address, String port, String action, Optional<Integer> craneNumber, Optional<String> container) {
        this.address = address;
        this.port = port;
        this.action = action;
        this.craneNumber = craneNumber;
        this.container = container;
    }

    public static CraneClientParams getParams(String addressAndPort, String action, String crane, String container) {
        if (addressAndPort == null || addressAndPort.isEmpty()) {
            throw new IllegalArgumentException("A server address must be provided.");
        }

        if (action == null || action.isEmpty() || !VALID_ACTIONS.contains(action)) {
            throw new IllegalArgumentException("The action provided must be one of the following: \"unload\", \"checkCrane\" or \"fixCrane\".");
        }

        List<String> addressAndPortParts = List.of(addressAndPort.split(":"));
        return new CraneClientParams(
                addressAndPortParts.get(0),
                addressAndPortParts.size() > 1 ? addressAndPortParts.get(1) : "50051",
                action,
                Optional.ofNullable(crane).map(Integer::parseInt),
                Optional.ofNullable(container)
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

    public Optional<String> getContainer() {
        return container;
    }
}