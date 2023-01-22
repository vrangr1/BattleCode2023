# TODO List

## Comms

- [x] Headquarter count: Fix.
- [x] Lesser or equal priority message overwriting. If none found, overwrite oldest message.
~~- [ ] Add dedicated channels for maintaining count of units~~
- [x] Free up 3 bits for symmetry stuff.
- [ ] Flushing the queues

## Carriers
- [x] Carriers need to explore more and avoid overcrowding
~~- [ ] Prioritize resource collection based on lack of a resource.~~
- [x] While returning to HQ to collect anchor, periodically check on the anchor count. If reaches zero, go back to resource gathering.
- [x] Collect anchors when going back to deposit resources..
- [x] Flee from launchers.
- [x] Consume less bytecode in highly crowded areas.
- [x] Opportunistic anchor collection.
- [x] Collection rate too low. Fix issue.
- [x] Update HQ to go to every turn when in transit_TO_DESP
~~- [ ] Don't do opportunistic if currently mining (inplacemining)~~
- [x] Carrier attacking carrier
- [x] Desperation index
- [x] Replace hasMilitaryUnit with a non carrier one
- [x] Mine only mana on smaller and medium size maps until some certain round.
- [x] Store island and wells locations locally and write it to comms later on.
- [x] Write combat locations
- [x] Do the new explore where you use the well near hq to find other wells
~~- [ ] What's happening with saving locations and writing them.~~
- [x] Flee if carrying anchors also
- [x] Overwrite older locations in savedLocations.
- [x] Increase radius if revolution complete in looping around hqs too.
~~- [ ] Possibly transfer resources from a hq that has abundant resources to an hq with less resources.~~
~~- [ ] Attack carriers by carriers~~
- [x] When near hq and about to deposit resources, don't flee.
~~- [ ] Need to slightly increase adamantium collection. Perhaps go back to 1:1 priortization split?~~
- [ ] Increase adamantium collection on larger maps
- [ ] Possible overcrowding fix: Trigger explore when not mining and greater than a threshold number of carriers in vision.
- [ ] Find out how to improve production in BowAndArrow (noBFSplz match)
- [ ] Shift all movementWrappers to the end of the code PROPERLY.
- [ ] Fix flee from enemy hq. Perhaps use circular explore to circumvent?
- [ ] If carrying anchor and encountered enemy occupied island with no enemy to defend, squat and free.
- [ ] Refine toExploreOrNotToExplore heuristic
- [ ] Test mining of resources enroute to another location
- [ ] Check save location's performance.
- [ ] Test flee rounds counts.
- [ ] Perhaps deposit to other hqs if nearest hq is overcrowded
- [ ] Experiment with mining immediately after first move. Remove bugs.
- [ ] Carrier mining loss due to movement wrapper (run auto script)
- [ ] Try removing the need for Explore.explore() in the worst case scenario in Circular Explore.
- [ ] Fix fleeing code
- [ ] Avoid overcrowding
- [ ] Work on generation of elixir

## Explore:
- [x] Add function to allow launchers to circle around a location at a distance.

## Build Order:
- [x] Perhaps use Ivan Geffner's build order
~~- [ ] If using bot counts for build order prioritization, use amplifier count as a confidence factor on how trustworthy each bot count is.~~
- [x] Fix amplifier excess production.
- [x] verify well designation by hq for carrier.
- [x] Anchor production too low now. Fix
- [x] Spawn launchers when our hq can see enemy hq
- [ ] Set optimal build location properly (carriers and launchers done)

## Launchers

- [x] Change to Finite State Machine micro.
- [ ] Try to shift code to microbattle if possible
- [ ] Add cloud attacking
- [ ] Add hiding in clouds and better attacking comms

## Combat

- [x] Verify combat message types' priority order
- [x] Find out if an anchor can be thrown by a carrier -> Yes
- [x] Find out how much damage is done by throwing an anchor by a carrier.

## Amplifiers:
- [ ] Do all comms stuff you can (while ensure bytecode limits).

## General
~~- [ ]  Make maps~~
- [x]  Defense of skyislands
- [x]  Send enemy occupied islands to combat channels
- [ ]  Amplifier built by HQ follow carriers carrying anchors
- [ ]  Scan for unoccupied islands should be done by every bot (given bytecodes left)
- [ ]  Attack of skyislands via any units (launchers first to clear the enemy units and then any units for occupation of the same)
- [ ]  Have amplifiers travel to combat locations