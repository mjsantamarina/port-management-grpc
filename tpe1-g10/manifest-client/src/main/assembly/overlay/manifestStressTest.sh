#!/bin/bash
# Non-blocking stress test helper pattern.
# Any command that might wait (queue, watch, stream) is launched in the background
# and its output is logged. The script continues in the foreground.

SERVER_ADDRESS="${SERVER_ADDRESS:-localhost:50051}"
PAUSE="${PAUSE:-0.1}"

run_command() {
  echo "> $1"
  eval "$1"
  local rc=$?
  sleep "$PAUSE"
  return $rc
}

# Launch possibly blocking command in background, capturing output to logfile.
# Usage: run_bg "cmd ..." "path/to/log"
run_bg() {
  local cmd="$1"
  local logfile="$2"
  mkdir -p "$(dirname "$logfile")"
  echo "> (bg) $cmd  # log: $logfile"
  ( eval "$cmd" ) >"$logfile" 2>&1 &
  local pid=$!
  BG_PIDS+=("$pid")
  BG_LOGS+=("$logfile")
  echo "   pid=$pid"
}

# Best-effort cleanup of background processes on exit.
cleanup_bg() {
  if [ ${#BG_PIDS[@]} -gt 0 ]; then
    echo ">> Stopping ${#BG_PIDS[@]} background processes..."
    for p in "${BG_PIDS[@]}"; do
      if kill -0 "$p" 2>/dev/null; then
        kill "$p" 2>/dev/null || true
      fi
    done
  fi
}
trap cleanup_bg EXIT

BG_PIDS=()
BG_LOGS=()

# Ensure a client distribution folder exists and script is executable
ensure_client() {
  local module="$1"      # e.g., dock-client
  local base_name="$2"   # e.g., tpe1-g10-dock-client-2025.2Q
  local script_name="$3" # e.g., dock.sh
  if [ ! -d "${module}/target/${base_name}" ]; then
    echo "---> Unpacking ${base_name} for ${module}..."
    (cd "${module}/target" && tar -xzf "${base_name}-bin.tar.gz") || exit 1
  fi
  chmod +x "${module}/target/${base_name}/${script_name}" 2>/dev/null || true
}

# Clients
ensure_client "dock-client" "tpe1-g10-dock-client-2025.2Q" "dock.sh"
ensure_client "ship-client" "tpe1-g10-ship-client-2025.2Q" "ship.sh"
ensure_client "crane-client" "tpe1-g10-crane-client-2025.2Q" "crane.sh"
ensure_client "manifest-client" "tpe1-g10-manifest-client-2025.2Q" "manifest.sh"

DOCK_DIR="dock-client/target/tpe1-g10-dock-client-2025.2Q"
SHIP_DIR="ship-client/target/tpe1-g10-ship-client-2025.2Q"
CRANE_DIR="crane-client/target/tpe1-g10-crane-client-2025.2Q"
MANIFEST_DIR="manifest-client/target/tpe1-g10-manifest-client-2025.2Q"
LOG_DIR="$(pwd)/logs/manifest"
OUT_DIR="$(pwd)/manifest-client/target"

mkdir -p "$LOG_DIR" "$OUT_DIR"

PENDING_TXT="${OUT_DIR}/pending.txt"
CONTAINERS_ALL_TXT="${OUT_DIR}/containers_all.txt"
CONTAINERS_DOCK2_TXT="${OUT_DIR}/containers_dock2.txt"
CONTAINERS_CRANE1_TXT="${OUT_DIR}/containers_crane1.txt"

echo "==== [SETUP] Docks & Cranes ===="
cd "$DOCK_DIR"
run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=addDock -Dsize=S" # dock 1
run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=addDock -Dsize=M" # dock 2
run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=addDock -Dsize=L" # dock 3
run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=addCrane -Ddock=1 -DmaxContainers=5"  # crane 1
run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=addCrane -Ddock=2 -DmaxContainers=5"  # crane 2

echo "==== Ships: one active, one pending (bg) ===="
cd "../../../${SHIP_DIR}"
run_command "sh ship.sh -DserverAddress=${SERVER_ADDRESS} -Daction=requestDock -Did=MAN_SHP1 -Dsize=S -Dcontainers=A1#A2#A3"
run_bg "sh ship.sh -DserverAddress=${SERVER_ADDRESS} -Daction=requestDock -Did=MAN_SHP2 -Dsize=M -Dcontainers=B1#B2#B3#B4" "${LOG_DIR}/request_ship2.log"
sleep 0.5

echo '==== Unload some containers on crane #1 ===='
cd "../../../${CRANE_DIR}"
script -q -c "sh crane.sh -DserverAddress=${SERVER_ADDRESS} -Daction=unload -Dcrane=1 -Dcontainer=A1" /dev/null <<'EOF'
A2
A3
__END__
EOF

echo "==== Reports ===="
cd "../../../${MANIFEST_DIR}"
run_command "sh manifest.sh -DserverAddress=${SERVER_ADDRESS} -Daction=pending -DoutPath=${PENDING_TXT}"
run_command "sh manifest.sh -DserverAddress=${SERVER_ADDRESS} -Daction=containers -DoutPath=${CONTAINERS_ALL_TXT}"
run_command "sh manifest.sh -DserverAddress=${SERVER_ADDRESS} -Daction=containers -DoutPath=${CONTAINERS_DOCK2_TXT} -Ddock=2"
run_command "sh manifest.sh -DserverAddress=${SERVER_ADDRESS} -Daction=containers -DoutPath=${CONTAINERS_CRANE1_TXT} -Dcrane=1"

echo "==== Done (logs in ${LOG_DIR}; reports in ${OUT_DIR}) ===="
