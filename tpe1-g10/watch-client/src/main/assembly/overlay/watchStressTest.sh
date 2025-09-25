#!/bin/bash
# Re-adapted to follow the updated shipStressTest.sh model

SERVER_ADDRESS="localhost:50051"
PAUSE=0.1

run_command() {
  echo "> $1"
  eval "$1"
  sleep $PAUSE
}

# Helper: ensure a client distribution is extracted and executable
ensure_client() {
  local module="$1"           # e.g., dock-client
  local base_name="$2"        # e.g., tpe1-g10-dock-client-2025.2Q
  local script_name="$3"      # e.g., dock.sh

  if [ ! -d "${module}/target/${base_name}" ]; then
    echo "---> Unpacking ${base_name}..."
    (cd "${module}/target" && tar -xzf "${base_name}-bin.tar.gz") || exit 1
  fi
  # Try both possible chmod locations to mirror the ship model
  if [ -f "target/${base_name}/${script_name}" ]; then
    chmod +x "target/${base_name}/${script_name}"
  elif [ -f "${module}/target/${base_name}/${script_name}" ]; then
    chmod +x "${module}/target/${base_name}/${script_name}"
  fi
}


# Ensure needed clients
ensure_client "dock-client" "tpe1-g10-dock-client-2025.2Q" "dock.sh"
ensure_client "ship-client" "tpe1-g10-ship-client-2025.2Q" "ship.sh"
ensure_client "crane-client" "tpe1-g10-crane-client-2025.2Q" "crane.sh"
ensure_client "watch-client" "tpe1-g10-watch-client-2025.2Q" "watch.sh"

DOCK_DIR="dock-client/target/tpe1-g10-dock-client-2025.2Q"
SHIP_DIR="ship-client/target/tpe1-g10-ship-client-2025.2Q"
CRANE_DIR="crane-client/target/tpe1-g10-crane-client-2025.2Q"
WATCH_DIR="watch-client/target/tpe1-g10-watch-client-2025.2Q"

echo "==== [SETUP] Create dock and crane, request a ship ===="
cd ${DOCK_DIR}
run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=addDock -Dsize=S"   # dock 1
run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=addCrane -Ddock=1 -DmaxContainers=5" # crane 1

cd ../../../${SHIP_DIR}
run_command "sh ship.sh -DserverAddress=${SERVER_ADDRESS} -Daction=requestDock -Did=WATCH_SHIP -Dsize=S -Dcontainers=W1#W2#W3"

LOG_FILE="$(pwd)/watch_crane1.log"
echo "==== Start watch on crane #1 (background; output -> ${LOG_FILE}) ===="
cd ../../../${WATCH_DIR}
( sh watch.sh -DserverAddress=${SERVER_ADDRESS} -Daction=watchCrane -Dcrane=1 ) > "${LOG_FILE}" 2>&1 &
WATCH_PID=$!
sleep 0.5

echo "==== Trigger events: unload one container, close/open dock ===="
cd ../../../${CRANE_DIR}
printf 'W2\n' | sh crane.sh -DserverAddress=${SERVER_ADDRESS} -Daction=unload -Dcrane=1 -Dcontainer=W1

cd ../../../${DOCK_DIR}
run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=closeDock -Ddock=1"
run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=openDock -Ddock=1"

echo "==== Stop watching crane #1 ===="
cd ../../../${WATCH_DIR}
run_command "sh watch.sh -DserverAddress=${SERVER_ADDRESS} -Daction=stopWatch -Dcrane=1"

# Give time for background process to flush
sleep 0.5
echo "==== Watch stress test completed. See ${LOG_FILE} for emitted events. ===="
