#!/bin/bash
start_time=$SECONDS
team1="$(basename src/A*)"
team2=OTCBot

mkdir -p logs
if test -f "logs/results.log"; then
  mv logs/results.log logs/results_old.log
fi
if test -f "logs/warnings.log"; then
  mv logs/warnings.log logs/warnings_old.log
fi
if test -f "logs/freezing.log"; then
  mv logs/freezing.log logs/freezing_old.log
fi
if test -f "logs/detailed_results.log"; then
  mv logs/detailed_results.log logs/detailed_results_old.log
fi

for ((i=1; i<=8; i++));
do
  if test -f "logs/log$i.log"; then
    mv logs/log$i.log logs/log$i_old.log
  fi
done

count=0
maps=(SmallElements AllElements DefaultMap Cat Tacocat Pizza Diagonal BatSignal)
length=${#maps[@]}
for ((i=0; i<${length}; i+=4));
do
  (trap 'kill 0' SIGINT;
    echo "Running map $[i+1]: $[i+4]"
  ./gradlew -PteamA=$team1 -PteamB=$team2 -Pmaps=${maps[$i]} -PenableProfiler=false run >> logs/log1.log &
  ./gradlew -PteamA=$team2 -PteamB=$team1 -Pmaps=${maps[$i]} -PenableProfiler=false run >> logs/log2.log &
  ./gradlew -PteamA=$team1 -PteamB=$team2 -Pmaps=${maps[$[i+1]]} -PenableProfiler=false run >> logs/log3.log &
  ./gradlew -PteamA=$team2 -PteamB=$team1 -Pmaps=${maps[$[i+1]]} -PenableProfiler=false run >> logs/log4.log &
  ./gradlew -PteamA=$team1 -PteamB=$team2 -Pmaps=${maps[$[i+2]]} -PenableProfiler=false run >> logs/log5.log &
  ./gradlew -PteamA=$team2 -PteamB=$team1 -Pmaps=${maps[$[i+2]]} -PenableProfiler=false run >> logs/log6.log &
  ./gradlew -PteamA=$team1 -PteamB=$team2 -Pmaps=${maps[$[i+3]]} -PenableProfiler=false run >> logs/log7.log &
  ./gradlew -PteamA=$team2 -PteamB=$team1 -Pmaps=${maps[$[i+3]]} -PenableProfiler=false run >> logs/log8.log
  )
  wait < <(jobs -p)
done
wait < <(jobs -p)
echo "========Grepping results========="

for ((i=1; i<=8; i++));
do
    grep -E "Warning,|vs. " logs/log$i.log >> logs/warnings.log
    echo -e "===========\n" >> logs/warnings.log

    grep -E "Birth |vs. " logs/log$i.log >> logs/freezing.log
    echo -e "===========\n" >> logs/freezing.log

    grep -E "vs. |wins" logs/log$i.log >> logs/detailed_results.log
    echo -e "===========\n" >> logs/detailed_results.log

    grep -F "wins" logs/log$i.log >> logs/results.log
    echo -e "===========\n" >> logs/results.log

done

elapsed=$(( SECONDS - start_time ))
echo "Elapsed time: $elapsed seconds"

# eval "echo Elapsed time: $(date -ud "@$elapsed" +'$((%s/3600/24)) days %H hr %M min %S sec')"
##0: maptestsmall  : 20 x 20 : 400
#1: SmallElements : 20 x 20 : 400
#2: AllElements   : 30 x 30 : 900
#3: DefaultMap    : 32 x 32 : 1024
#4: OctDoors      : 45 x 45 : 2025
#5: CloudyOctDoors: 45 x 45 : 2025
##6: GrandRing     : 60 x 60 : 3600
