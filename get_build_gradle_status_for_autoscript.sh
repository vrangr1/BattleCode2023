#!/bin/bash


if grep -Fq "// '-Dbc.server.websocket=false'," build.gradle
then
    echo "shouldn't run autoscript"
else
    echo "can run autoscript"
fi