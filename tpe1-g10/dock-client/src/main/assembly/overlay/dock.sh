#!/bin/bash
PATH_TO_CODE_BASE=`pwd`
#JAVA_OPTS="-Djava.rmi.server.codebase=file://$PATH_TO_CODE_BASE/lib/jars/rmi-params-client-1.0-SNAPSHOT.jar"
#JAVA_OPTS="-Dio.netty.tryReflectionSetAccessible=false --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED"
JAVA_OPTS="-Dio.netty.noUnsafe=true"
for arg in "$@"
do
  if [[ $arg == -D* ]]; then
    JAVA_OPTS="$JAVA_OPTS -D${arg:2}"
    shift
  fi
done
MAIN_CLASS="ar.edu.itba.pod.g10.client.DockClient"
java $JAVA_OPTS -cp 'lib/jars/*' $MAIN_CLASS $*