#!/bin/bash

cd src
rm -rvf OTCBot/
old_bot="$(basename A*)"
new_bot="OTCBot"
cp -r $old_bot $new_bot

cd $new_bot

if [[ "$OSTYPE" == "darwin"* ]]; then
    grep -irl "$old_bot" . | xargs sed -i "" "s/$old_bot/$new_bot/g"
else
    grep -irl "$old_bot" . | xargs sed -i "s/$old_bot/$new_bot/g"
fi