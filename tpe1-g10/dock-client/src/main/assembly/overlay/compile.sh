#!/bin/bash

# Función para ejecutar comandos con manejo de errores
run_command() {
    echo "> $1"
    eval "$1"
    sleep 0.5  # Pausa entre comandos en segundos
}

echo "==== Compilando el proyecto ===="
if [ ! -d "target" ]; then
    echo "---> No veo la carpeta compilada. Compilando el proyecto..."
    run_command "mvn clean install > /dev/null"
fi

if [ ! -d "target/tpe1-g10-dock-client-2025.2Q" ]; then
    echo "---> Descomprimiendo el cliente..."
    cd target && tar -xzf tpe1-g10-dock-client-2025.2Q-bin.tar.gz
    cd tpe1-g10-dock-client-2025.2Q && chmod +x dock.sh
    cd ../..
fi

echo "==== Compilación completada ===="
echo "El cliente está disponible en target/tpe1-g10-dock-client-2025.2Q/dock.sh"