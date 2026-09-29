# Port Management System — Distributed Objects with gRPC

A distributed system that models a **container port**: docks, cranes and ships interact with a central **gRPC server** through several command-line clients. It supports **bidirectional streaming** (container unloading), **server streaming** (real-time notifications) and thread-safe shared state under concurrent clients.

> Team project for *Programación de Objetos Distribuidos* (ITBA, 2C 2025).

## Architecture

```
               ┌──────────────── gRPC (HTTP/2, Protobuf) ────────────────┐
dock-client ───┤                                                        │
ship-client ───┤   server  ─ DockService  ─ ShipService  ─ CraneService │
crane-client ──┤           ─ NotifyService ─ ReportService              │
watch-client ──┤   (in-memory repositories + fine-grained locks)        │
manifest-client┘                                                        │
               └────────────────────────────────────────────────────────┘
```

| Service | RPCs |
|---|---|
| `DockService` | add docks (S/M/L), attach cranes, open / close docks |
| `ShipService` | request a dock (**server-streaming** while waiting in queue), dock status, leave dock |
| `CraneService` | unload containers (**bidirectional streaming**), crane status, maintenance |
| `NotifyService` | watch a crane and receive events in real time (**server streaming**), stop watching |
| `ReportService` | pending ships, unloaded containers per ship/dock |

Key points:
- **Concurrency**: the server handles many clients at once; shared state (docks, cranes, queues) is protected with explicit locks so dock assignment respects priority and each crane is used by only one ship at a time.
- **Non-blocking notifications**: events are pushed to subscribers without blocking the operation that produced them.
- **Error handling**: domain errors are mapped to gRPC status codes and shown clearly by the clients.

## Tech stack

`Java 17` · `gRPC 1.54` · `Protocol Buffers` · `Maven` (multi-module) · `Java concurrency (locks, concurrent collections)`

## Build & run

```bash
cd tpe1-g10
mvn clean install

# server
cd server/target && tar -xzf tpe1-g10-server-2025.2Q-bin.tar.gz
cd tpe1-g10-server-2025.2Q && ./run-server.sh          # listens on :50051

# example client calls
./dock.sh  -DserverAddress=localhost:50051 -Daction=addDock  -Dsize=M
./dock.sh  -DserverAddress=localhost:50051 -Daction=addCrane -Ddock=1 -DmaxContainers=10
```

Full usage of every client (in Spanish): [docs/USO.md](docs/USO.md).

## Team

Manuel Santamarina ([@ManuSanta12](https://github.com/ManuSanta12)) · [@smesarubio](https://github.com/smesarubio) · [@maurovella](https://github.com/maurovella) · [@ariasnico](https://github.com/ariasnico)
