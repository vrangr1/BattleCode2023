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
# Sorted the maps by size
for i in Checkmate2 Clown AllElements PairedProgramming Rewind DefaultMap KingdomRush HideAndSeek BatSignal Tacocat Hah Eyelands\
    Rectangle Scatter Frog Diagonal Lines Crossword Cat Cornucopia Forest
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

# All Maps (sorted by size (width x height : (x, y)); double # represents our maps):
#  Checkmate2           : 20 x 20 : 400  sprint 2 (chess board type with clouds, currents, and islands as squares of size 3x3); use
#  Lantern              : 20 x 20 : 400  sprint 2
#  maptestsmall         : 20 x 20 : 400
#  Quiet                : 20 x 20 : 400
#  SmallElements        : 20 x 20 : 400
#  Clown                : 21 x 20 : 420
#  Jail                 : 20 x 30 : 600
#  Orbit                : 25 x 25 : 625
#  Sun                  : 25 x 25 : 625
#  BowAndArrow          : 35 x 25 : 875
#  AllElements          : 30 x 30 : 900
#  Cube                 : 30 x 30 : 900  sprint 2
#  Dreamy               : 30 x 30 : 900  clouds++
#  Pakbot               : 30 x 30 : 900  sprint 2
#  PairedProgramming    : 45 x 20 : 900  clouds++ (hqs in clouds)
#  Pizza                : 30 x 30 : 900
#  Rewind               : 30 x 30 : 900  clouds++ (hqs in corners surrounded by clouds)
#  DefaultMap           : 32 x 32 : 1024
#  KingdomRush          : 45 x 24 : 1080
#  HideAndSeek          : 37 x 31 : 1147 sprint 2
#  BatSignal            : 60 x 20 : 1200
#  Minefield            : 60 x 20 : 1200
#  Tacocat              : 60 x 20 : 1200
#  Hah                  : 50 x 25 : 1250
#  Movepls              : 45 x 30 : 1350
#  Eyelands             : 50 x 30 : 1500
#  Rectangle            : 50 x 30 : 1500
#  Scatter              : 50 x 30 : 1500
#  Frog                 : 39 x 39 : 1521
#  ArtistRendition      : 40 x 40 : 1600
#  Diagonal             : 40 x 40 : 1600
#  Divergence           : 40 x 40 : 1600 sprint 2
#  Lines                : 40 x 40 : 1600 sprint 2 (pathfind test?)
#  Turtle               : 40 x 40 : 1600 clouds++
#  Pit                  : 60 x 30 : 1800
## CloudyOctDoors       : 45 x 45 : 2025
#  Crossword            : 45 x 45 : 2025 sprint 2
#  FourNations          : 45 x 45 : 2025 sprint 2
#  Pathfind             : 45 x 45 : 2025
## OctDoors             : 45 x 45 : 2025
#  BattleSuns           : 50 x 50 : 2500 sprint 2
#  Cat                  : 50 x 50 : 2500
#  Maze                 : 50 x 50 : 2500 sprint 2
#  Cornucopia           : 59 x 59 : 3481 sprint 2 (wells in center)
#  Forest               : 60 x 60 : 3600 clouds++
## GrandRing            : 60 x 60 : 3600
#  Grievance            : 60 x 60 : 3600
