#!/bin/bash
# Stress test for SHIP client
# Uses README actions as reference and dockStressTest.sh as style guide

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
  chmod +x "${module}/target/${base_name}/${script_name}"
}


# Ensure needed clients
ensure_client "dock-client" "tpe1-g10-dock-client-2025.2Q" "dock.sh"
ensure_client "ship-client" "tpe1-g10-ship-client-2025.2Q" "ship.sh"

DOCK_DIR="dock-client/target/tpe1-g10-dock-client-2025.2Q"
SHIP_DIR="ship-client/target/tpe1-g10-ship-client-2025.2Q"

echo "==== [SETUP] Create docks and cranes for ship tests ===="
cd ${DOCK_DIR}
# Create three docks: S, M, L
run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=addDock -Dsize=S"
run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=addDock -Dsize=M"
run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=addDock -Dsize=L"
# Add a crane to each dock so ships can unload later
run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=addCrane -Ddock=1 -DmaxContainers=10"
run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=addCrane -Ddock=2 -DmaxContainers=20"
run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=addCrane -Ddock=3 -DmaxContainers=15"

echo '==== Request docks for multiple ships (queue + auto assignment) ===='
cd ../../../${SHIP_DIR}
run_command "sh ship.sh -DserverAddress=${SERVER_ADDRESS} -Daction=requestDock -Did=SHIP_S1 -Dsize=S -Dcontainers=A1#A2#A3"
run_command "sh ship.sh -DserverAddress=${SERVER_ADDRESS} -Daction=requestDock -Did=SHIP_M1 -Dsize=M -Dcontainers=B1#B2#B3#B4#B5"
run_command "sh ship.sh -DserverAddress=${SERVER_ADDRESS} -Daction=requestDock -Did=SHIP_L1 -Dsize=L -Dcontainers=C1#C2"
run_command "sh ship.sh -DserverAddress=${SERVER_ADDRESS} -Daction=requestDock -Did=SHIP_S2 -Dsize=S -Dcontainers=D1"
run_command "sh ship.sh -DserverAddress=${SERVER_ADDRESS} -Daction=requestDock -Did=SHIP_M2 -Dsize=M -Dcontainers=E1#E2"

echo '==== Check dock statuses (1..3) ===='
for d in 1 2 3; do
  run_command "sh ship.sh -DserverAddress=${SERVER_ADDRESS} -Daction=checkDock -Ddock=${d}"
done

echo '==== Attempt to leave a dock (SHIP_S1 from Dock #1) ===='
# This may fail if containers are not yet unloaded; that's fine to exercise error paths per spec
run_command "sh ship.sh -DserverAddress=${SERVER_ADDRESS} -Daction=leaveDock -Did=SHIP_S1 -Ddock=1"

echo "==== Ship stress test completed ===="
