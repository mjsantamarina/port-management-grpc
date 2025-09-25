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
ensure_client "manifest-client" "tpe1-g10-manifest-client-2025.2Q" "manifest.sh"

DOCK_DIR="dock-client/target/tpe1-g10-dock-client-2025.2Q"
SHIP_DIR="ship-client/target/tpe1-g10-ship-client-2025.2Q"
CRANE_DIR="crane-client/target/tpe1-g10-crane-client-2025.2Q"
MANIFEST_DIR="manifest-client/target/tpe1-g10-manifest-client-2025.2Q"

OUT_DIR="$(pwd)/manifest-client/target"
PENDING_TXT="${OUT_DIR}/pending.txt"
CONTAINERS_ALL_TXT="${OUT_DIR}/containers_all.txt"
CONTAINERS_DOCK2_TXT="${OUT_DIR}/containers_dock2.txt"
CONTAINERS_CRANE1_TXT="${OUT_DIR}/containers_crane1.txt"

mkdir -p "${OUT_DIR}"

echo "==== [SETUP] Create docks, cranes, and ships ===="
cd ${DOCK_DIR}
run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=addDock -Dsize=S" # dock 1
run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=addDock -Dsize=M" # dock 2
run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=addDock -Dsize=L" # dock 3
run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=addCrane -Ddock=1 -DmaxContainers=5"  # crane 1
run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=addCrane -Ddock=2 -DmaxContainers=5"  # crane 2

cd ../../../${SHIP_DIR}
run_command "sh ship.sh -DserverAddress=${SERVER_ADDRESS} -Daction=requestDock -Did=MAN_SHP1 -Dsize=S -Dcontainers=A1#A2#A3"
run_command "sh ship.sh -DserverAddress=${SERVER_ADDRESS} -Daction=requestDock -Did=MAN_SHP2 -Dsize=M -Dcontainers=B1#B2#B3#B4"

echo '==== Create some unloaded containers history on crane #1 ===='
cd ../../../${CRANE_DIR}
printf 'A2\nA3\n' | sh crane.sh -DserverAddress=${SERVER_ADDRESS} -Daction=unload -Dcrane=1 -Dcontainer=A1

echo "==== Generate PENDING report ===="
cd ../../../${MANIFEST_DIR}
run_command "sh manifest.sh -DserverAddress=${SERVER_ADDRESS} -Daction=pending -DoutPath=${PENDING_TXT}"

echo "==== Generate CONTAINERS report (all) ===="
run_command "sh manifest.sh -DserverAddress=${SERVER_ADDRESS} -Daction=containers -DoutPath=${CONTAINERS_ALL_TXT}"

echo "==== Generate CONTAINERS report filtered by dock=2 ===="
run_command "sh manifest.sh -DserverAddress=${SERVER_ADDRESS} -Daction=containers -DoutPath=${CONTAINERS_DOCK2_TXT} -Ddock=2"

echo "==== Generate CONTAINERS report filtered by crane=1 ===="
run_command "sh manifest.sh -DserverAddress=${SERVER_ADDRESS} -Daction=containers -DoutPath=${CONTAINERS_CRANE1_TXT} -Dcrane=1"

echo "==== Manifest stress test completed ===="
echo "Reports:"
echo " - ${PENDING_TXT}"
echo " - ${CONTAINERS_ALL_TXT}"
echo " - ${CONTAINERS_DOCK2_TXT}"
echo " - ${CONTAINERS_CRANE1_TXT}"
