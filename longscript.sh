#!/bin/bash

team1="$(basename src/A*)"
count1=0

AllElements
DefaultMap
maptestsmall
SmallElements
ArtistRendition
BatSignal
BowAndArrow
Cat
Clown
Diagonal
Eyelands
Frog
Grievance
Hah
Jail
KingdomRush
Minefield
Movepls
Orbit
Pathfind
Pit
Pizza
Quiet
Rectangle
Scatter
Sun
Tacocat
Turtle
Dreamy
Forest
PairedProgramming
Rewind
BattleSuns
Checkmate2
Cornucopia
Crossword
Cube
Divergence
FourNations
HideAndSeek
Lantern
Lines
Maze
Pakbot
Piglets
Risk
Sine
Snowflake
SomethingFishy
Spin
Spiral
Squares
Star
Sus
SweetDreams
TicTacToe
USA
Barcode
Contraction
Flower
Grapes
IslandHopping
Marsh
RaceToTheTop
Repetition
River
RockWall
Sakura
SoundWave
ThirtyFive
TimesUp
TreasureMap

for team2 in OQualsBot OSymmetryBot OnElixirBot OHealingBot OMysteriousBot OPreSprintTwoBot OSavantMinerBot
do
    rm -rf logs/${team1}_vs_${team2}/
    mkdir -p logs/${team1}_vs_${team2}
    # if test -f "logs/${team2}_results.log"; then
    #   mv logs/results.log logs/results_old.log
    # fi
    # if test -f "logs/warnings.log"; then
    #   mv logs/warnings.log logs/warnings_old.log
    # fi
    # if test -f "logs/freezing.log"; then
    #   mv logs/freezing.log logs/freezing_old.log
    # fi
    # if test -f "logs/detailed_results.log"; then
    #   mv logs/detailed_results.log logs/detailed_results_old.log
    # fi
    # if test -f "logs/log.log"; then
    #   mv logs/log.log logs/log_old.log
    # fi
    count1=$[count1+1]
    count2=0
    echo "Team number $count1: $team2, is processing:"
    for i in SmallElements Clown AllElements PairedProgramming Pizza Rewind DefaultMap KingdomRush BatSignal Tacocat Hah Eyelands\
        Rectangle Scatter Frog Diagonal Cat Forest
    do
    count2=$[count2+1]
    echo "$team2: Running map $count2: $i"
    ./gradlew -PteamA=$team1 -PteamB=$team2 -Pmaps=$i -PenableProfiler=false run >> logs/${team1}_vs_${team2}/log.log
    ./gradlew -PteamA=$team2 -PteamB=$team1 -Pmaps=$i -PenableProfiler=false run >> logs/${team1}_vs_${team2}/log.log
    done
    echo "Grepping results"
    grep -F "wins" logs/${team1}_vs_${team2}/log.log >> logs/${team1}_vs_${team2}/results.log
    grep -E "vs. |wins" logs/${team1}_vs_${team2}/log.log >> logs/${team1}_vs_${team2}/detailed_results.log
    grep -E "Warning,|vs. " logs/${team1}_vs_${team2}/log.log >> logs/${team1}_vs_${team2}/warnings.log
    grep -E "Birth |vs. " logs/${team1}_vs_${team2}/log.log >> logs/${team1}_vs_${team2}/freezing.log
done