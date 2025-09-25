package ar.edu.itba.pod.g10.client;

import ar.edu.itba.pod.grpc.utils.Size;

import java.util.List;
import java.util.Optional;

public class ShipClientParams {
    private String address;
    private String port;
    private String action;
    private static final List<String> VALID_ACTIONS = List.of("requestDock", "checkDock", "leaveDock");

    private Optional<String> id;
    private Optional<Size> size;
    private Optional<String> containers;
    private Optional<Integer> dockNumber;

    private ShipClientParams(String address, String port, String action, Optional<String> id, Optional<Size> size, Optional<String> containers, Optional<Integer> dockNumber) {
        this.address = address;
        this.port = port;
        this.action = action;
        this.id = id;
        this.size = size;
        this.containers = containers;
        this.dockNumber = dockNumber;
    }

    public static ShipClientParams getParams(String addressAndPort, String action, String id, String size, String containers, String dock) {
        if (addressAndPort == null || addressAndPort.isEmpty()) {
            throw new IllegalArgumentException("A server address must be provided.");
        }

        if (action == null || action.isEmpty() || !VALID_ACTIONS.contains(action)) {
            throw new IllegalArgumentException("The action provided must be one of the following: \"requestDock\", \"checkDock\" or \"leaveDock\".");
        }

        List<String> addressAndPortParts = List.of(addressAndPort.split(":"));
        return new ShipClientParams(
                addressAndPortParts.get(0),
                addressAndPortParts.size() > 1 ? addressAndPortParts.get(1) : "50051",
                action,
                Optional.ofNullable(id),
                Optional.ofNullable(size).map(ShipClientParams::getSizeConstant),
                Optional.ofNullable(containers),
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

    public Optional<String> getId() {
        return id;
    }

    public Optional<Size> getSize() {
        return size;
    }

    public Optional<String> getContainers() {
        return containers;
    }

    public Optional<Integer> getDockNumber() {
        return dockNumber;
    }

    private static Size getSizeConstant(String size) {
        return switch (size) {
            case "S" -> Size.SIZE_SMALL;
            case "M" -> Size.SIZE_MEDIUM;
            case "L" -> Size.SIZE_LARGE;
            default -> throw new IllegalArgumentException("Invalid size: " + size);
        };
    }
}