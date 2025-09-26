#!/bin/bash
# Non-blocking SHIP stress test for macOS (PTY-friendly).
# - Usa 'script -q /dev/null' para alimentar sesiones interactivas (crane unload).
# - Lanza en background los requestDock que podrían quedar en cola.

SERVER_ADDRESS="${SERVER_ADDRESS:-localhost:50051}"
PAUSE="${PAUSE:-0.1}"

run_command() {
  echo "> $1"
  eval "$1"
  local rc=$?
  sleep "$PAUSE"
  return $rc
}

# Ejecuta en background y loguea salida
run_bg() {
  local cmd="$1"
  local logfile="$2"
  mkdir -p "$(dirname "$logfile")"
  echo "> (bg) $cmd  # log: $logfile"
  ( eval "$cmd" ) >"$logfile" 2>&1 &
  local pid=$!
  BG_PIDS+=("$pid"); BG_LOGS+=("$logfile")
  echo "   pid=$pid"
}

cleanup_bg() {
  if [ ${#BG_PIDS[@]} -gt 0 ]; then
    echo ">> Stopping ${#BG_PIDS[@]} background processes..."
    for p in "${BG_PIDS[@]}"; do
      if kill -0 "$p" 2>/dev/null; then kill "$p" 2>/dev/null || true; fi
    done
  fi
}
trap cleanup_bg EXIT

BG_PIDS=(); BG_LOGS=()

# Unpack helper
ensure_client() {
  local module="$1" base_name="$2" script_name="$3"
  if [ ! -d "${module}/target/${base_name}" ]; then
    echo "---> Unpacking ${base_name} for ${module}..."
    (cd "${module}/target" && tar -xzf "${base_name}-bin.tar.gz") || exit 1
  fi
  chmod +x "${module}/target/${base_name}/${script_name}" 2>/dev/null || true
}

# Clients
ensure_client "dock-client"   "tpe1-g10-dock-client-2025.2Q"   "dock.sh"
ensure_client "ship-client"   "tpe1-g10-ship-client-2025.2Q"   "ship.sh"
ensure_client "crane-client"  "tpe1-g10-crane-client-2025.2Q"  "crane.sh"

DOCK_DIR="dock-client/target/tpe1-g10-dock-client-2025.2Q"
SHIP_DIR="ship-client/target/tpe1-g10-ship-client-2025.2Q"
CRANE_DIR="crane-client/target/tpe1-g10-crane-client-2025.2Q"
LOG_DIR="$(pwd)/logs/ship"; mkdir -p "$LOG_DIR"

echo "==== [SETUP] Docks & Cranes ===="
cd "$DOCK_DIR"
run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=addDock -Dsize=S"   # dock 1
run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=addDock -Dsize=M"   # dock 2
run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=addDock -Dsize=L"   # dock 3
run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=addCrane -Ddock=1 -DmaxContainers=10" # crane 1
run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=addCrane -Ddock=2 -DmaxContainers=20" # crane 2
run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=addCrane -Ddock=3 -DmaxContainers=15" # crane 3

echo "==== Request multiple ships (bg donde puedan quedar en cola) ===="
cd "../../../${SHIP_DIR}"
# Probablemente amarran de inmediato:
run_command "sh ship.sh -DserverAddress=${SERVER_ADDRESS} -Daction=requestDock -Did=SHIP_S1 -Dsize=S -Dcontainers=A1#A2#A3"
run_command "sh ship.sh -DserverAddress=${SERVER_ADDRESS} -Daction=requestDock -Did=SHIP_M1 -Dsize=M -Dcontainers=B1#B2#B3#B4#B5"
# Estos podrían esperar -> background:
run_bg "sh ship.sh -DserverAddress=${SERVER_ADDRESS} -Daction=requestDock -Did=SHIP_L1 -Dsize=L -Dcontainers=C1#C2" "${LOG_DIR}/request_ship_L1.log"
run_bg "sh ship.sh -DserverAddress=${SERVER_ADDRESS} -Daction=requestDock -Did=SHIP_S2 -Dsize=S -Dcontainers=D1"     "${LOG_DIR}/request_ship_S2.log"
run_bg "sh ship.sh -DserverAddress=${SERVER_ADDRESS} -Daction=requestDock -Did=SHIP_M2 -Dsize=M -Dcontainers=E1#E2"  "${LOG_DIR}/request_ship_M2.log"
sleep 0.5

echo "==== Descargar SHIP_S1 en Dock #1 con Crane #1 (PTY) ===="
cd "../../../${CRANE_DIR}"
# A1 por arg; luego A2, A3; '__END__' (inválido) cierra la sesión limpiamente.
script -q /dev/null sh -c "sh crane.sh -DserverAddress=${SERVER_ADDRESS} -Daction=unload -Dcrane=1 -Dcontainer=A1" <<'EOF'
A2
A3
__END__
EOF

echo "==== leaveDock de SHIP_S1 (camino feliz, sin carga pendiente) ===="
cd "../../../${SHIP_DIR}"
run_command "sh ship.sh -DserverAddress=${SERVER_ADDRESS} -Daction=leaveDock -Did=SHIP_S1 -Ddock=1"

echo "==== Check dock statuses (1..3) ===="
for d in 1 2 3; do
  run_command "sh ship.sh -DserverAddress=${SERVER_ADDRESS} -Daction=checkDock -Ddock=${d}"
done

echo "==== Ship stress test completed (bg logs en ${LOG_DIR}) ===="
printf "BG logs:\n - %s\n" "${BG_LOGS[@]}"