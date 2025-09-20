package ar.edu.itba.pod.g10.client;

import ar.edu.itba.pod.grpc.utils.Size;

import java.util.List;
import java.util.Optional;

public class DockClientParams {
    private String address;
    private String port;
    private String action;
    private static final List<String> VALID_ACTIONS = List.of("addDock", "addCrane", "toggleDock");

    private Optional<Size> size;
    private Optional<Integer> dockNumber;
    private Optional<Integer> containerAmount;

    private DockClientParams(String address, String port, String action, Optional<Size> size, Optional<Integer> dockNumber, Optional<Integer> containerAmount) {
        this.address = address;
        this.port = port;
        this.action = action;
        this.size = size;
        this.dockNumber = dockNumber;
        this.containerAmount = containerAmount;
    }

    public static DockClientParams getParams(String addressAndPort, String action, String size, String dock, String maxContainers) {
        if (addressAndPort == null || addressAndPort.isEmpty()) {
            throw new IllegalArgumentException("A server address must be provided.");
        }

        if (action == null || action.isEmpty() || !VALID_ACTIONS.contains(action)) {
            throw new IllegalArgumentException("The action provided must be one of the following: \"addDock\", \"addCrane\" or \"toggleDock\".");
        }

        List<String> addressAndPortParts = List.of(addressAndPort.split(":"));
        return new DockClientParams(
                addressAndPortParts.get(0),
                addressAndPortParts.size() > 1 ? addressAndPortParts.get(1) : "50051",
                action,
                Optional.ofNullable(size).map(DockClientParams::getSizeConstant),
                Optional.ofNullable(dock).map(Integer::parseInt),
                Optional.ofNullable(maxContainers).map(Integer::parseInt)
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

    public Optional<Size> getSize() {
        return size;
    }

    public Optional<Integer> getDockNumber() {
        return dockNumber;
    }

    public Optional<Integer> getContainerAmount() {
        return containerAmount;
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