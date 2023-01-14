# TODO List

## Comms

- [x] Headquarter count: Fix.
- [x] Lesser or equal priority message overwriting. If none found, overwrite oldest message.
- [ ] Flushing the queues
- [ ] Add dedicated channels for maintaining count of units

## Carriers
- [x] Carriers need to explore more and avoid overcrowding
- [ ] Prioritize resource collection based on lack of a resource.

## Build Order:
- [x] Perhaps use Ivan Geffner's build order
- [ ] If using bot counts for build order prioritization, use amplifier count as a confidence factor on how trustworthy each bot count is.

## Launchers

- [ ] Change to Finite State Machine micro.
- [ ] Try to shift code to microbattle if possible
- [ ] Add hiding in clouds and better attacking comms

## Combat

- [x] Verify combat message types' priority order
- [x] Find out if an anchor can be thrown by a carrier -> Yes
- [ ] Find out how much damage is done by throwing an anchor by a carrier.

## General
- [ ] Carrier bytecode consumption reduction
- [ ]  Amplifier built by HQ follow carriers carrying anchors
- [ ]  Scan for unoccupied islands should be done by every bot (given bytecodes left)
- [ ]  Defense of skyislands
- [ ]  Send enemy occupied islands to combat channels
- [ ]  Attack of skyislands via any units (launchers first to clear the enemy units and then any units for occupation of the same)
- [ ]  Have amplifiers travel to combat locations
- [ ]  Make maps