#!/bin/bash

# Configuración de la prueba
SERVER_ADDRESS="localhost:50051"
ITERATIONS=5  # Número de iteraciones para cada operación
PAUSE=0.1  # Pausa entre comandos en segundos

# Función para ejecutar comandos con manejo de errores
run_command() {
    echo "> $1"
    eval "$1"
    sleep $PAUSE
}

cd target/tpe1-g10-dock-client-2025.2Q && chmod +x dock.sh

echo "==== Agregando 10 muelles (dock) ===="
for i in $(seq 1 10); do
    run_command "sh dock.sh -DserverAddress=localhost:50051 -Daction=addDock -Dsize=$(echo SML | cut -c$(( (i-1)%3 + 1 )))"
done

echo "==== Creando muelles de diferentes tamaños ===="
run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=addDock -Dsize=S"
run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=addDock -Dsize=M"
run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=addDock -Dsize=L"

echo "==== Agregando grúas a los muelles ===="
for i in $(seq 1 8); do
    dock_number=$i
    capacity=$((10 * (i % 3 + 1)))  # Capacidades de 10, 20 o 30 contenedores
    run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=addCrane -Ddock=${dock_number} -DmaxContainers=${capacity}"
done

echo "==== Alternando estados de muelles ===="
for i in $(seq 1 5); do
    # Abrir muelles pares
    run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=openDock -Ddock=$((i*2))"
    # Cerrar muelles impares
    run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=closeDock -Ddock=$((i*2-1))"
done

for i in $(seq 1 3); do
    run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=closeDock -Ddock=$((i*2))"
    run_command "sh dock.sh -DserverAddress=${SERVER_ADDRESS} -Daction=openDock -Ddock=$((i*2-1))"
done

echo "==== Prueba de estrés completada ===="
echo "Se han realizado pruebas de creación de muelles, adición de grúas y cambios de estado"
