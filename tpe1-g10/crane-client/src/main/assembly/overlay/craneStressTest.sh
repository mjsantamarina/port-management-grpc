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

DOCK_DIR="dock-client/target/tpe1-g10-dock-client-2025.2Q"
SHIP_DIR="ship-client/target/tpe1-g10-ship-client-2025.2Q"
CRANE_DIR="crane-client/target/tpe1-g10-crane-client-2025.2Q"

echo "==== [SETUP] Create dock L and crane, request a ship with containers ===="
cd ${DOCK_DIR}
run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=addDock -Dsize=L"
run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=addCrane -Ddock=1 -DmaxContainers=3"

echo "==== Request a ship ===="
cd ../../../${SHIP_DIR}
run_command "sh ship.sh -DserverAddress=${SERVER_ADDRESS} -Daction=requestDock -Did=CRANE_SHIP -Dsize=S -Dcontainers=X1#X2#X3#AA"

echo "==== Exercise checkCrane before unloading ===="
cd ../../../${CRANE_DIR}
run_command "sh crane.sh -DserverAddress=${SERVER_ADDRESS} -Daction=checkCrane -Dcrane=1"

echo "==== Start unload on Crane #1, piping container IDs ===="
printf 'X2\nX3\n' | sh crane.sh -DserverAddress=${SERVER_ADDRESS} -Daction=unload -Dcrane=1 -Dcontainer=X1

echo "==== Check crane after unload (might be MAINTENANCE or OPEN) ===="
run_command "sh crane.sh -DserverAddress=${SERVER_ADDRESS} -Daction=checkCrane -Dcrane=1"

echo "==== Try maintenance (fixCrane) ===="
run_command "sh crane.sh -DserverAddress=${SERVER_ADDRESS} -Daction=fixCrane -Dcrane=1"

echo "==== Crane stress test completed ===="
