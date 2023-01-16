#!/bin/bash

rm -vf logs/results.log

echo -e "===========\n" >> logs/results.log
grep -F "wins" logs/log1.log >> logs/results.log
echo -e "===========\n" >> logs/results.log
grep -F "wins" logs/log2.log >> logs/results.log