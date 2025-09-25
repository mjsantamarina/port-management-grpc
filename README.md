# TPE-1 POD - Sistema de Puerto de Contenedores

Un sistema distribuido de gestión portuaria construido con gRPC y Java, que incluye gestión de muelles, operaciones de grúas, manejo de barcos y notificaciones en tiempo real.

## Estructura del Proyecto

```
tpe1-g10/
├── api/                    # Definiciones de Protocol Buffer y código generado
├── server/                 # Implementación del servidor gRPC
├── dock-client/            # Cliente de gestión de muelles
├── crane-client/           # Cliente de operaciones de grúas
├── ship-client/            # Cliente de gestión de barcos
├── watch-client/           # Cliente de notificaciones en tiempo real
└── manifest-client/        # Cliente de reportes de manifiestos
```

## Compilar el Proyecto

Para compilar todos los módulos y crear paquetes de distribución:

```bash
cd tpe1-g10
mvn clean install
```

Esto hará:
- Compilar todas las definiciones de Protocol Buffer
- Generar stubs y clases gRPC
- Compilar todos los archivos fuente Java
- Crear paquetes de distribución ejecutables para cada cliente y servidor

## Ejecutar el Servidor

### 1. Navegar al directorio de distribución del servidor y descomprimir el archivo tpe1-g10-server-2025.2Q-bin.tar.gz:
```bash
cd server/target/ && tar -xzf tpe1-g10-server-2025.2Q-bin.tar.gz
```

### 2. Hacer el script ejecutable e iniciar el servidor:
```bash
cd tpe1-g10-server-2025.2Q && chmod +x run-server.sh && ./run-server.sh
```

El servidor iniciará en el puerto `50051` por defecto y mostrará:
```
Server started, listening on 50051
```

## Ejecutar el Cliente de Muelles

El Cliente de Muelles gestiona las operaciones de muelles incluyendo agregar muelles, agregar grúas a muelles y alternar el estado de muelles.

### Configuración

1. **Navegar al directorio de distribución del cliente de muelles y descromprimir el archivo tpe1-g10-dock-client-2025.2Q-bin.tar.gz:**
   ```bash
   cd dock-client/target/ && tar -xzf tpe1-g10-dock-client-2025.2Q-bin.tar.gz
   ```

2. **Hacer el script ejecutable:**
   ```bash
   cd tpe1-g10-dock-client-2025.2Q/ && chmod +x dock.sh
   ```

### Operaciones Disponibles

#### Agregar un Muelle
Crea un nuevo muelle con el tamaño especificado.

```bash
./dock.sh -DserverAddress=localhost:50051 -Daction=addDock -Dsize=<TAMAÑO>
```

<u>Parámetros:</u>
- `serverAddress`: Dirección del servidor y puerto (ej., `localhost:50051`)
- `action`: Debe ser `addDock`
- `size`: Tamaño del muelle - `S` (Pequeño), `M` (Mediano), o `L` (Grande)

<u>Ejemplos:</u>
```bash
# Agregar un muelle pequeño
./dock.sh -DserverAddress=localhost:50051 -Daction=addDock -Dsize=S

# Agregar un muelle mediano
./dock.sh -DserverAddress=localhost:50051 -Daction=addDock -Dsize=M

# Agregar un muelle grande
./dock.sh -DserverAddress=localhost:50051 -Daction=addDock -Dsize=L
```

**Salida Esperada:</u>
```
Added 🛟 Dock #1 (S)
```

#### Agregar una Grúa a un Muelle
TODO: Agregar una grúa a un dock que no existe rompe el server

Agrega una grúa con capacidad de contenedores especificada a un muelle existente.

```bash
./dock.sh -DserverAddress=localhost:50051 -Daction=addCrane -Ddock=<NÚMERO_MUELLE> -DmaxContainers=<CANTIDAD_CONTENEDORES>
```

**Parámetros:</u>
- `serverAddress`: Dirección del servidor y puerto
- `action`: Debe ser `addCrane`
- `dock`: Número del muelle objetivo (obtenido de la respuesta de addDock)
- `maxContainers`: Número máximo de contenedores que la grúa puede manejar

**Ejemplos:</u>
```bash
# Agregar una grúa con capacidad de 10 contenedores al muelle #1
./dock.sh -DserverAddress=localhost:50051 -Daction=addCrane -Ddock=1 -DmaxContainers=10

# Agregar una grúa con capacidad de 25 contenedores al muelle #2
./dock.sh -DserverAddress=localhost:50051 -Daction=addCrane -Ddock=2 -DmaxContainers=25
```

**Salida Esperada:</u>
```
Added 🏗️ Crane #1 (0📦) [10] on 🛟 Dock #1 ()
```

#### Alternar Estado del Muelle
Cambia el estado operacional de un muelle (abierto/cerrado).

```bash
./dock.sh -DserverAddress=localhost:50051 -Daction=openDock -Ddock=<NÚMERO_MUELLE>

./dock.sh -DserverAddress=localhost:50051 -Daction=closeDock -Ddock=<NÚMERO_MUELLE>
```

**Parámetros:</u>
- `serverAddress`: Dirección del servidor y puerto
- `action`: Debe ser `openDock` para abrir o `closeDock` para cerrar
- `dock`: Número del muelle objetivo

**Ejemplos:</u>
```bash
# Alternar estado del muelle #1
./dock.sh -DserverAddress=localhost:50051 -Daction=openDock -Ddock=1

./dock.sh -DserverAddress=localhost:50051 -Daction=closeDock -Ddock=1
```

**Salida Esperada:</u>
```
OPEN 🛟 Dock #1 (S)
```
o bien:
```
CLOSED 🛟 Dock #1 (S)
```

## Ejecutar el Cliente de Barcos

El Cliente de Barcos gestiona las operaciones de barcos incluyendo solicitud de muelle, verificación de estado de muelle y abandono de muelle.

### Configuración

1. **Navegar al directorio de distribución del cliente de barcos y descomprimir el archivo tpe1-g10-ship-client-2025.2Q-bin.tar.gz:**
   ```bash
   cd ship-client/target/ && tar -xzf tpe1-g10-ship-client-2025.2Q-bin.tar.gz
   ```

2. **Hacer el script ejecutable:**
   ```bash
   cd tpe1-g10-ship-client-2025.2Q/ && chmod +x ship.sh
   ```

### Operaciones Disponibles

#### Solicitar un Muelle
Solicita un muelle para un barco, esperando en cola si es necesario, con notificaciones en tiempo real.

```bash
./ship.sh -DserverAddress=localhost:50051 -Daction=requestDock -Did=<ID_BARCO> -Dsize=<TAMAÑO> -Dcontainers=<CONTENEDORES>
```

<u>Parámetros:</u>
- `serverAddress`: Dirección del servidor y puerto (ej., `localhost:50051`)
- `action`: Debe ser `requestDock`
- `id`: ID del barco (e.g., `SHIP1`)
- `size`: Tamaño del barco - `S` (Pequeño), `M` (Mediano), o `L` (Grande)
- `containers`: Lista de contenedores separados por `#` (e.g., `A1#A2#A3`)

<u>Ejemplo:</u>
```bash
./ship.sh -DserverAddress=localhost:50051 -Daction=requestDock -Did=SHIP1 -Dsize=S -Dcontainers=A1#A2#A3
```

<u>Salida Esperada:</u>
```
⚓ SHIP1 (S) (3📦) is waiting for a dock with 0 ships ahead
⚓ SHIP1 (S) (3📦) in 🛟 Dock #1 (S)
```

#### Verificar Estado de un Muelle
Verifica el estado de un muelle (cerrado/abierto/libre/ocupado, con info de barco si ocupado).

```bash
./ship.sh -DserverAddress=localhost:50051 -Daction=checkDock -Ddock=<NÚMERO_MUELLE>
```

<u>Parámetros:</u>
- `serverAddress`: Dirección del servidor y puerto (ej., `localhost:50051`)
- `action`: Debe ser `checkDock`
- `dock`: Número del muelle a verificar

<u>Ejemplo:</u>
```bash
./ship.sh -DserverAddress=localhost:50051 -Daction=checkDock -Ddock=1
```

<u>Salida Esperada:</u>
```
IDLE 🛟 Dock #1 (S)
```
o si ocupado:
```
BUSY ⚓ SHIP1 (S) (3📦) in 🛟 Dock #1 (S)
```

#### Abandonar un Muelle
Abandona un muelle después de completar la descarga.

```bash
./ship.sh -DserverAddress=localhost:50051 -Daction=leaveDock -Did=<ID_BARCO> -Ddock=<NÚMERO_MUELLE>
```

<u>Parámetros:</u>
- `serverAddress`: Dirección del servidor y puerto (ej., `localhost:50051`)
- `action`: Debe ser `leaveDock`
- `id`: ID del barco
- `dock`: Número del muelle a abandonar

<u>Ejemplo:</u>
```bash
./ship.sh -DserverAddress=localhost:50051 -Daction=leaveDock -Did=SHIP1 -Ddock=1
```

<u>Salida Esperada:</u>
```
⚓ SHIP1 (S) (0📦) left 🛟 Dock #1 (S)
```



## Problemas TODO
TODO: Crane está tirando el server cuando corre
```bash
sh crane.sh -DserverAddress=localhost:50051 -Daction=unload -Dcrane=1 -Dcontainer=X1
```
```
2025-09-20 14:38:27 INFO  CraneClient:47 - tpe1-g10 Crane Client Starting ...
Enter Container ID: Error: UNKNOWN
```

and in server:
```
java.lang.NullPointerException: Cannot invoke "ar.edu.itba.pod.g10.server.models.Crane.getDock()" because "crane" is null
        at ar.edu.itba.pod.g10.server.servants.CraneServant$1.onNext(CraneServant.java:35)

Crane crane = craneRepository.getCrane(unloadContainersRequest.getCraneNumber()); is returning null
```

## Ejecutar el Cliente de Grúas

El Cliente de Grúas gestiona las operaciones de grúas incluyendo descarga de contenedores (interactiva), verificación de estado y mantenimiento.

### Configuración

1. **Navegar al directorio de distribución del cliente de grúas y descomprimir el archivo tpe1-g10-crane-client-2025.2Q-bin.tar.gz:**
   ```bash
   cd crane-client/target/ && tar -xzf tpe1-g10-crane-client-2025.2Q-bin.tar.gz
   ```

2. **Hacer el script ejecutable:**
   ```bash
   cd tpe1-g10-crane-client-2025.2Q/ && chmod +x crane.sh
   ```

### Operaciones Disponibles

#### Descargar Contenedores
Inicia la descarga interactiva de contenedores desde una grúa.

```bash
./crane.sh -DserverAddress=localhost:50051 -Daction=unload -Dcrane=<NÚMERO_GRÚA> -Dcontainer=<ID_CONTENEDOR_INICIAL>
```

<u>Parámetros:</u>
- `serverAddress`: Dirección del servidor y puerto (ej., `localhost:50051`)
- `action`: Debe ser `unload`
- `crane`: Número de la grúa
- `container`: ID del primer contenedor

<u>Ejemplo:</u>
```bash
./crane.sh -DserverAddress=localhost:50051 -Daction=unload -Dcrane=1 -Dcontainer=B1
```

<u>Salida Esperada:</u>
```
📦 Unloaded B1 from 🏗️ Crane #1 (1📦) [15] on 🛟 Dock #1 (S)
Enter Container ID: B2
📦 Unloaded B2 from 🏗️ Crane #1 (2📦) [15] on 🛟 Dock #1 (S)
...
Unloading Ended on 🛟 Dock #1 (S)
```

#### Verificar Estado de una Grúa
Verifica el estado de una grúa (UNLOADING/MAINTENANCE/CLOSED/OPEN).

```bash
./crane.sh -DserverAddress=localhost:50051 -Daction=checkCrane -Dcrane=<NÚMERO_GRÚA>
```

<u>Parámetros:</u>
- `serverAddress`: Dirección del servidor y puerto (ej., `localhost:50051`)
- `action`: Debe ser `checkCrane`
- `crane`: Número de la grúa

<u>Ejemplo:</u>
```bash
./crane.sh -DserverAddress=localhost:50051 -Daction=checkCrane -Dcrane=1
```

<u>Salida Esperada:</u>
```
OPEN 🏗️ Crane #1 (0📦) [15] on 🛟 Dock #1 (S)
```

#### Realizar Mantenimiento en una Grúa
Realiza mantenimiento en una grúa, reiniciando contador.

```bash
./crane.sh -DserverAddress=localhost:50051 -Daction=fixCrane -Dcrane=<NÚMERO_GRÚA>
```

<u>Parámetros:</u>
- `serverAddress`: Dirección del servidor y puerto (ej., `localhost:50051`)
- `action`: Debe ser `fixCrane`
- `crane`: Número de la grúa

<u>Ejemplo:</u>
```bash
./crane.sh -DserverAddress=localhost:50051 -Daction=fixCrane -Dcrane=1
```

<u>Salida Esperada:</u>
```
OPEN 🏗️ Crane #1 (0📦) [15] on 🛟 Dock #1 (S)
```

## Ejecutar el Cliente de Observación

El Cliente de Observación gestiona notificaciones en tiempo real para una grúa.

### Configuración

1. **Navegar al directorio de distribución del cliente de observación y descomprimir el archivo tpe1-g10-watch-client-2025.2Q-bin.tar.gz:**
   ```bash
   cd watch-client/target/ && tar -xzf tpe1-g10-watch-client-2025.2Q-bin.tar.gz
   ```

2. **Hacer el script ejecutable:**
   ```bash
   cd tpe1-g10-watch-client-2025.2Q/ && chmod +x watch.sh
   ```

### Operaciones Disponibles

#### Observar Notificaciones de una Grúa
Registra para recibir eventos en tiempo real de una grúa.

```bash
./watch.sh -DserverAddress=localhost:50051 -Daction=watchCrane -Dcrane=<NÚMERO_GRÚA>
```

<u>Parámetros:</u>
- `serverAddress`: Dirección del servidor y puerto (ej., `localhost:50051`)
- `action`: Debe ser `watchCrane`
- `crane`: Número de la grúa

<u>Ejemplo:</u>
```bash
./watch.sh -DserverAddress=localhost:50051 -Daction=watchCrane -Dcrane=1
```

<u>Salida Esperada:</u>
```
OPEN 🏗️ Crane #1 (0📦) [15] on 🛟 Dock #1 (S)
📦 Unloaded B1 from 🏗️ Crane #1 (1📦) [15] on 🛟 Dock #1 (S)
...
```

#### Detener Notificaciones de una Grúa
Anula el registro de notificaciones para una grúa.

```bash
./watch.sh -DserverAddress=localhost:50051 -Daction=stopWatch -Dcrane=<NÚMERO_GRÚA>
```

<u>Parámetros:</u>
- `serverAddress`: Dirección del servidor y puerto (ej., `localhost:50051`)
- `action`: Debe ser `stopWatch`
- `crane`: Número de la grúa

<u>Ejemplo:</u>
```bash
./watch.sh -DserverAddress=localhost:50051 -Daction=stopWatch -Dcrane=1
```

<u>Salida Esperada:</u>
```
Notificaciones detenidas para grúa #1
```

## Ejecutar el Cliente de Manifiestos

El Cliente de Manifiestos genera reportes de barcos pendientes y contenedores descargados en archivos TXT.

### Configuración

1. **Navegar al directorio de distribución del cliente de manifiestos y descomprimir el archivo tpe1-g10-manifest-client-2025.2Q-bin.tar.gz:**
   ```bash
   cd manifest-client/target/ && tar -xzf tpe1-g10-manifest-client-2025.2Q-bin.tar.gz
   ```

2. **Hacer el script ejecutable:**
   ```bash
   cd tpe1-g10-manifest-client-2025.2Q/ && chmod +x manifest.sh
   ```

### Operaciones Disponibles

#### Reporte de Barcos Pendientes
Lista barcos pendientes de muelle en un archivo TXT.

```bash
./manifest.sh -DserverAddress=localhost:50051 -Daction=pending -DoutPath=<RUTA_ARCHIVO>
```

<u>Parámetros:</u>
- `serverAddress`: Dirección del servidor y puerto (ej., `localhost:50051`)
- `action`: Debe ser `pending`
- `outPath`: Ruta al archivo TXT de salida

<u>Ejemplo:</u>
```bash
./manifest.sh -DserverAddress=localhost:50051 -Daction=pending -DoutPath=pending.txt
```

<u>Contenido Esperado en pending.txt:</u>
```
⚓ SHIP1 (S) (3📦)
⚓ SHIP2 (M) (5📦)
```

#### Reporte de Contenedores Descargados
Lista contenedores descargados (histórica, con filtros opcionales) en un archivo TXT.

```bash
./manifest.sh -DserverAddress=localhost:50051 -Daction=containers -DoutPath=<RUTA_ARCHIVO> -Dcrane=<NÚMERO_GRÚA> -Ddock=<NÚMERO_MUELLE>
```

<u>Parámetros:</u>
- `serverAddress`: Dirección del servidor y puerto (ej., `localhost:50051`)
- `action`: Debe ser `containers`
- `outPath`: Ruta al archivo TXT de salida
- `crane`: Número de grúa (opcional filtro)
- `dock`: Número de muelle (opcional filtro)

<u>Ejemplo:</u>
```bash
./manifest.sh -DserverAddress=localhost:50051 -Daction=containers -DoutPath=containers.txt -Dcrane=1 -Ddock=1
```

<u>Contenido Esperado en containers.txt:</u>
```
📦 A1 unloaded from 🏗️ Crane #1 (1📦) [15] on 🛟 Dock #1 (S)
📦 A2 unloaded from 🏗️ Crane #1 (2📦) [15] on 🛟 Dock #1 (S)
```


### Problemas Comunes y Solución de Problemas

#### ClassNotFoundException
Si obtienes `Error: Could not find or load main class ar.edu.itba.pod.g10.client.DockClient`:

**Solución:** Asegúrate de ejecutar el script desde el directorio correcto:
```bash
# Incorrecto - ejecutando desde directorio fuente
dock-client/src/main/assembly/overlay/dock.sh  # ❌

# Correcto - ejecutando desde distribución extraída
cd dock-client/target/tpe1-g10-dock-client-2025.2Q/
./dock.sh  # ✅
```

#### Problemas de Conexión
Si obtienes `UNAVAILABLE: io exception`:

1. **Verificar que el servidor esté ejecutándose:** Asegúrate de que el servidor esté iniciado y escuchando en el puerto especificado
2. **Verificar dirección del servidor:** Usa `localhost:50051` si se ejecuta localmente
3. **Conectividad de red:** Asegúrate de que la dirección del servidor sea accesible desde tu cliente

### Flujo de Trabajo de Ejemplo

Aquí tienes un ejemplo completo de configuración y uso del sistema de muelles:

```bash
# 1. Iniciar el servidor (en una terminal)
cd server/target/tpe1-g10-server-2025.2Q/
./run-server.sh

# 2. En otra terminal, navegar al cliente de muelles
cd dock-client/target/tpe1-g10-dock-client-2025.2Q/

# 3. Agregar un muelle grande
./dock.sh -DserverAddress=localhost:50051 -Daction=addDock -Dsize=L
# Salida: Added 🛟 Dock #1 (L)

# 4. Agregar una grúa al muelle
./dock.sh -DserverAddress=localhost:50051 -Daction=addCrane -Ddock=1 -DmaxContainers=15
# Salida: Added 🏗️ Crane #1 (0📦) [15] on 🛟 Dock #1 ()

# 5. Alternar estado del muelle
./dock.sh -DserverAddress=localhost:50051 -Daction=toggleDock -Ddock=1
# Salida: OPEN 🛟 Dock #1 (L)
```

## Pruebas Realizadas
**Stress Tests:**
Para correr los stress test individuales, navegar a cada cliente y ejecutar:
```bash
./src/main/assembly/overlay/compile.sh
./src/main/assembly/overlay/<client>StressTest.sh
```

## Correcciones y Mejoras Implementadas

### 🐛 Bugs Corregidos

#### 1. Error de Compilación en DockClient
**Problema:** `ClassNotFoundException` y errores de tipos incompatibles
- **Causa:** Conflictos entre enums duplicados en `dock` y `utils` packages
- **Solución:**
   - Eliminados enums duplicados del archivo `dock_service.proto`
   - Corregidos imports para usar `ar.edu.itba.pod.grpc.utils.Size` y `ar.edu.itba.pod.grpc.utils.DockStatus`
   - Actualizados nombres de constantes enum (`SIZE_SMALL`, `SIZE_MEDIUM`, `SIZE_LARGE`)

#### 2. Problema de Ejecución de Scripts
**Problema:** `Error: Could not find or load main class`
- **Causa:** Scripts ejecutados desde directorio incorrecto
- **Solución:** Documentación clara sobre ejecutar desde directorios de distribución extraídos

### ✅ Estado Actual del Sistema
- **Servidor:** Funcionando correctamente
- **Cliente de Muelles:** Todas las operaciones funcionando (`addDock`, `addCrane`, `toggleDock`)
- **Distribuciones:** Generándose correctamente con Maven
- **Scripts:** Ejecutándose sin errores desde directorios correctos

## Notas de Desarrollo

- **Protocol Buffers:** Las definiciones de servicios están en `api/src/main/proto/`
- **Implementación del Servidor:** Las implementaciones de servicios están en `server/src/main/java/`
- **Código de Clientes:** El código fuente de cada cliente está en sus respectivos directorios `src/main/java/`
- **Logging:** Todos los clientes usan Log4j para configuración de logging

## Requisitos

- Java 24
- Maven 3.6
- Compilador de Protocol Buffers (manejado por el plugin de Maven)