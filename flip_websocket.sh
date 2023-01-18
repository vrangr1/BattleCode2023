#!/bin/bash


if grep -Fq "// '-Dbc.server.websocket=false'," build.gradle
then
    echo "uncommenting websocket line"
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i "" "s/\/\/ '-Dbc.server.websocket=false',/'-Dbc.server.websocket=false',/g" build.gradle
    else
        sed -i "s/\/\/ '-Dbc.server.websocket=false',/'-Dbc.server.websocket=false',/g" build.gradle
    fi
else
    echo "commenting websocket line"
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i "" "s/'-Dbc.server.websocket=false',/\/\/ '-Dbc.server.websocket=false',/g" build.gradle
    else
        sed -i "s/'-Dbc.server.websocket=false',/\/\/ '-Dbc.server.websocket=false',/g" build.gradle
    fi
fi

./gradlew build