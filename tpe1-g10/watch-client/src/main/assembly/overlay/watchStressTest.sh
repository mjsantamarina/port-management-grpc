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
ensure_client "watch-client" "tpe1-g10-watch-client-2025.2Q" "watch.sh"

DOCK_DIR="dock-client/target/tpe1-g10-dock-client-2025.2Q"
SHIP_DIR="ship-client/target/tpe1-g10-ship-client-2025.2Q"
CRANE_DIR="crane-client/target/tpe1-g10-crane-client-2025.2Q"
WATCH_DIR="watch-client/target/tpe1-g10-watch-client-2025.2Q"
LOG_DIR="$(pwd)/logs/watch"

mkdir -p "$LOG_DIR"

echo "==== [SETUP] Dock & Crane ===="
cd "$DOCK_DIR"
run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=addDock -Dsize=S"
run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=addCrane -Ddock=1 -DmaxContainers=5"

echo "==== [BG] Request a ship (so watcher has events) ===="
cd "../../../${SHIP_DIR}"
run_bg "sh ship.sh -DserverAddress=${SERVER_ADDRESS} -Daction=requestDock -Did=WATCH_SHIP -Dsize=S -Dcontainers=W1#W2#W3" "${LOG_DIR}/request_ship.log"
sleep 0.5

echo "==== [BG] Start watch on crane #1 ===="
cd "../../../${WATCH_DIR}"
run_bg "sh watch.sh -DserverAddress=${SERVER_ADDRESS} -Daction=watchCrane -Dcrane=1" "${LOG_DIR}/watch_crane1.log"
sleep 0.5

echo "==== Trigger events ===="
cd "../../../${CRANE_DIR}"
printf 'W2\n__END__\n' | sh crane.sh -DserverAddress=${SERVER_ADDRESS} -Daction=unload -Dcrane=1 -Dcontainer=W1

cd "../../../${DOCK_DIR}"
run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=closeDock -Ddock=1"
run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=openDock -Ddock=1"

echo "==== Stop watcher ===="
cd "../../../${WATCH_DIR}"
script -q -c "sh crane.sh -DserverAddress=${SERVER_ADDRESS} -Daction=unload -Dcrane=1 -Dcontainer=W1" /dev/null <<'EOF'
W2
__END__
EOF

echo "==== Done (logs in ${LOG_DIR}) ===="
