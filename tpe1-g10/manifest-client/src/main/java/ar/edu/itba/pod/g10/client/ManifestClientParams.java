package ar.edu.itba.pod.g10.client;

import java.util.List;
import java.util.Optional;

public class ManifestClientParams {
    private String address;
    private String port;
    private String action;
    private static final List<String> VALID_ACTIONS = List.of("pending", "containers");

    private Optional<String> outPath;
    private Optional<Integer> crane;
    private Optional<Integer> dock;

    private ManifestClientParams(String address, String port, String action, Optional<String> outPath, Optional<Integer> crane, Optional<Integer> dock) {
        this.address = address;
        this.port = port;
        this.action = action;
        this.outPath = outPath;
        this.crane = crane;
        this.dock = dock;
    }

    public static ManifestClientParams getParams(String addressAndPort, String action, String outPath, String crane, String dock) {
        if (addressAndPort == null || addressAndPort.isEmpty()) {
            throw new IllegalArgumentException("A server address must be provided.");
        }

        if (action == null || action.isEmpty() || !VALID_ACTIONS.contains(action)) {
            throw new IllegalArgumentException("The action provided must be one of the following: \"pending\" or \"containers\".");
        }

        List<String> addressAndPortParts = List.of(addressAndPort.split(":"));
        return new ManifestClientParams(
                addressAndPortParts.get(0),
                addressAndPortParts.size() > 1 ? addressAndPortParts.get(1) : "50051",
                action,
                Optional.ofNullable(outPath),
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

    public Optional<String> getOutPath() {
        return outPath;
    }

    public Optional<Integer> getCrane() {
        return crane;
    }

    public Optional<Integer> getDock() {
        return dock;
    }
}