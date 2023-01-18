#!/bin/bash
cd scripts
rm -f ../logs/warnings.log
rm -f ../logs/freezing.log
rm -f ../logs/detailed_results.log
rm -f ../logs/results.log

touch ../logs/warnings.log
touch ../logs/freezing.log
touch ../logs/detailed_results.log
touch ../logs/results.log

for ((i=1; i<=4; i++));
do
    grep -E "Warning,|vs. " ../logs/log$i.log >> ../logs/warnings.log
    echo -e "===========\n" >> ../logs/warnings.log

    grep -E "Birth |vs. " ../logs/log$i.log >> ../logs/freezing.log
    echo -e "===========\n" >> ../logs/freezing.log

    grep -E "vs. |wins" ../logs/log$i.log >> ../logs/detailed_results.log
    echo -e "===========\n" >> ../logs/detailed_results.log

    grep -F "wins" ../logs/log$i.log >> ../logs/results.log
    echo -e "===========\n" >> ../logs/results.log
done