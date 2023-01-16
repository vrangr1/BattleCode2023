#!/bin/bash

cd src
old_bot="$(basename A*)"
new_bot=$1
old_bot_new_name="O${old_bot:1}"
cp -r $old_bot $new_bot
cd $old_bot

if [[ "$OSTYPE" == "darwin"* ]]; then
    grep -irl "$old_bot" . | xargs sed -i "" "s/$old_bot/$old_bot_new_name/g"
else
    grep -irl "$old_bot" . | xargs sed -i "s/$old_bot/$old_bot_new_name/g"
fi

cd ..
mv $old_bot $old_bot_new_name
cd $new_bot

if [[ "$OSTYPE" == "darwin"* ]]; then
    grep -irl "$old_bot" . | xargs sed -i "" "s/$old_bot/$new_bot/g"
else
    grep -irl "$old_bot" . | xargs sed -i "s/$old_bot/$new_bot/g"
fi

# sed -i '' 's/abc/XYZ/g' /tmp/file.txt