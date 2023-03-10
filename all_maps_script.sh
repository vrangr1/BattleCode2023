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
if test -f "logs/log1.log"; then
  mv logs/log1.log logs/log1_old.log
fi
if test -f "logs/log2.log"; then
  mv logs/log2.log logs/log2_old.log
fi

count=0
for i in AllElements ArtistRendition BatSignal BowAndArrow Cat CloudyOctDoors Clown DefaultMap Diagonal Eyelands Frog \
    GrandRing Grievance Hah Jail KingdomRush maptestsmall Minefield Movepls OctDoors Orbit Pathfind Pit Pizza Quiet Rectangle \
    Scatter SmallElements Sun Tacocat
    # CloudyOctDoors OctDoors #GrandRing
do
  count=$[count+1]
  if test $count -eq 1; then
    echo "Running map $count: $i"
  else
    echo -e "Running map $count: $i"
  fi
  (trap 'kill 0' SIGINT;
#   ./gradlew -PteamA=$team1 -PteamB=$team2 -Pmaps=$i -PenableProfiler=false run >> logs/log1.log &
  ./gradlew -PteamA=$team1 -PteamB=$team2 -Pmaps=$i -PenableProfiler=false run >> logs/log1.log &
  ./gradlew -PteamA=$team2 -PteamB=$team1 -Pmaps=$i -PenableProfiler=false run >> logs/log2.log
  )
  wait
done
echo "========Grepping results========="

grep -E "Warning,|vs. " logs/log1.log >> logs/warnings.log
echo -e "===========\n" >> logs/warnings.log
grep -E "Warning,|vs. " logs/log2.log >> logs/warnings.log

grep -E "Birth |vs. " logs/log1.log >> logs/freezing.log
echo -e "===========\n" >> logs/freezing.log
grep -E --line-buffered "Birth |vs. " logs/log2.log >> logs/freezing.log 

grep -E "vs. |wins |won" logs/log1.log >> logs/detailed_results.log
echo -e "===========\n" >> logs/detailed_results.log
grep -E "vs. |wins |won" logs/log2.log >> logs/detailed_results.log

sleep 1

echo -e "===========\n" >> logs/results.log
grep -F "wins" logs/log1.log >> logs/results.log
echo -e "===========\n" >> logs/results.log
grep -F "wins" logs/log2.log >> logs/results.log

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
