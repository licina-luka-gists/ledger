#!/usr/bin/env bash

if [ -z "$GRAALVM_HOME" ]; then
    echo 'Please set $GRAALVM_HOME'
    exit 1
fi

#  Clojure steps
mkdir -p bin/ classes/

clj -M -e "(compile 'ledger.core)" \
    && java -cp "$(clj -Spath)":classes ledger.core

# GraalVM steps
"$GRAALVM_HOME/bin/gu" install native-image
"$GRAALVM_HOME/bin/native-image" \
    -cp $(clj -Spath):classes \
    -H:Name=bin/ledger \
    -H:+ReportExceptionStackTraces \
    --initialize-at-build-time \
    --verbose \
    --no-fallback \
    --no-server \
    "-J-Xmx3g" \
    ledger.core
