package OBetterSprintBot;

import battlecode.common.*;
import java.util.Random;

import OBetterSprintBot.path.Nav;

public class BotCarrier extends Utils{

    private enum Status {
        BORN, // State at start of first turn
        EXPLORE_FOR_ISLANDS,
        EXPLORE_FOR_WELLS,
        TRANSIT_TO_WELL,
        TRANSIT_RES_DEP,
        TRANSIT_ANCHOR_COLLECTION,
        TRANSIT_TO_ISLAND,
        COLLECTING_RESOURCES,
        COLLECTING_ANCHOR,
        DEPOSITING_RESOURCES,
        ATTACKING,
        PLACING_ANCHOR,
        ANCHOR_NOT_FOUND,
        DESPERATE,
        OCCUPIED__FINDING_ANOTHER_ISLAND,
        FLEEING
    }

    private static boolean movingToIsland = false;
    private static MapLocation movementDestination = null;
    private static int targetedIslandId = -1;
    private static int currentInventoryWeight = -1;
    private static RobotInfo[] visibleEnemies;
    private static boolean returnToHQ = false;
    private static boolean collectedResourcesThisTurn = false;
    private static boolean inPlaceForCollection = false;
    private static int collectedAdamantium = 0, collectedMana = 0, collectedElixir = 0;
    private static int desperationIndex = 0;
    private static boolean goingToCollectAnchor = false;
    public static int collectAnchorHQidx = -1;
    private static final boolean randomExploration = true;
    private static boolean[] islandViable;
    private static boolean[] ignoreLocations;
    private static boolean returnEarly = false;
    private static Status carrierStatus = null;
    private static ResourceType prioritizedResource;
    private static final boolean TRY_TO_FLEE = true;
    private static boolean isFleeing = false;
    private static int fleeCount = 0;
    private static int[] ignoredIslandLocations;
    private static final int MAX_IGNORED_ISLAND_COUNT = 100;
    private static int ignoredIslandLocationsCount = 0;
    private static int hqIndex;

    private static void initSpawningHeadquarterIndex() throws GameActionException{
        MapLocation loc = Comms.findNearestHeadquarter();
        hqIndex = Comms.getHeadquarterIndex(loc);
        hqIndex = Comms.START_CHANNEL_BANDS + hqIndex * Comms.CHANNELS_COUNT_PER_HEADQUARTER + 2;
    }

    public static void initCarrier() throws GameActionException{
        carrierStatus = Status.BORN;
        rc.setIndicatorString(carrierStatus.toString());
        movingToIsland = false;
        movementDestination = null;
        targetedIslandId = -1;
        currentInventoryWeight = rc.getWeight();
        collectedResourcesThisTurn = false;
        collectedAdamantium = 0;
        collectedMana = 0;
        collectedElixir = 0;
        inPlaceForCollection = false;
        desperationIndex = 0;
        goingToCollectAnchor = false;
        collectAnchorHQidx = -1;
        returnEarly = false;
        rng = new Random(rc.getRoundNum() + 6147);
        // prioritizedResource = (rc.getID() % 2 == 0) ? ResourceType.ADAMANTIUM : ResourceType.MANA; // TODO: Change this
        initSpawningHeadquarterIndex();
        prioritizedResource = Comms.getPrioritizedResource(hqIndex);
        isFleeing = false;
        fleeCount = 0;
        islandViable = new boolean[ISLAND_COUNT];
        ignoredIslandLocations = new int[MAX_IGNORED_ISLAND_COUNT];
        ignoredIslandLocationsCount = 0;
        for (int i = 0; i < ISLAND_COUNT; i++)
            islandViable[i] = true;
        ignoreLocations = new boolean[MAP_HEIGHT * MAP_WIDTH];
        if (Clock.getBytecodesLeft() < 6000){
            returnEarly = true;
            return;
        }
    }

    private static void unFlagAllIslands(){
        for (int i = 0; i < ISLAND_COUNT; i++)
            islandViable[i] = true;
        for (int i = ignoredIslandLocationsCount; --i>=0;)
            ignoreLocations[ignoredIslandLocations[i]] = false;
        ignoredIslandLocationsCount = 0;
    }

    private static void movementWrapper(MapLocation dest) throws GameActionException{
        if (rc.isMovementReady()){
            pathing.setAndMoveToDestination(dest);
        }
        if (rc.isMovementReady()){
            Nav.goTo(dest);
        }
    }

    private static void movementWrapper() throws GameActionException{
        if (rc.isMovementReady()){
            pathing.setAndMoveToDestination(Explore.explore(randomExploration));
        }
        if (rc.isMovementReady()){
            Nav.goTo(Explore.explore(randomExploration));
        }
    }

    private static void movementWrapper(boolean fleeing) throws GameActionException{
        if (rc.isMovementReady())
            pathing.setAndMoveToDestination(Explore.explore());
        if (rc.isMovementReady())
            Nav.goTo(Explore.explore());
    }

    /**
     * Updates list of visible enemies found in vision
     * @throws GameActionException
     * @BytecodeCost : 100
     */
    private static void updateVision() throws GameActionException {
        visibleEnemies = rc.senseNearbyRobots(UNIT_TYPE.visionRadiusSquared, ENEMY_TEAM);
    }

    private static void assertNotHeadquarterLocation(MapLocation givenLoc) throws GameActionException{
        assert rc.getRoundNum() > 2 : "rc.getRoundNum() > 2";
        MapLocation[] locations = Comms.getAlliedHeadquartersLocationsList();
        MapLocation loc;
        for (int i = locations.length; i-->0;){
            loc = locations[i];
            assert loc != null : "locations[i] != null";
            assert loc.x != -1 : "locations[i].x != -1";
            assert !loc.equals(givenLoc) : "locations[i].equals(givenLoc); round num: " + rc.getRoundNum() + "; id: " + rc.getID() + "; currentLocation: " + rc.getLocation() + "; targetLoc: " + givenLoc;
        }
    }

    private static void assertHeadquarterLocation(MapLocation givenLoc) throws GameActionException{
        assert rc.getRoundNum() > 2 : "rc.getRoundNum() > 2";
        MapLocation[] locations = Comms.getAlliedHeadquartersLocationsList();
        MapLocation loc;
        boolean matched = false;
        for (int i = locations.length; i-->0;){
            loc = locations[i];
            if (loc.equals(givenLoc)){
                matched = true;
                break;
            }
        }
        assert matched : "matched; round num: " + rc.getRoundNum() + "; id: " + rc.getID() + "; currentLocation: " + rc.getLocation() + "; targetLoc: " + givenLoc;
    }

    private static void updateOverall() throws GameActionException{
        if (returnEarly) return;
        returnEarly = false;
        currentLocation = rc.getLocation();
        updateVision();
        // isFleeing = false;

        if (TRY_TO_FLEE){
            if (!isFleeing && CombatUtils.hasMilitaryUnit(visibleEnemies)){
                isFleeing = true;
                returnEarly = true;
                fleeCount = 5;
                if (tryToFlee(visibleEnemies)) tryToFlee(visibleEnemies);
                return;
            }
            if (isFleeing && CombatUtils.militaryCount(visibleEnemies) == 0)
                fleeCount = Math.max(0, fleeCount - 1);
            
            if (fleeCount == 0)
                isFleeing = false;
        }
        if (rc.getRoundNum() % 200 == 0){
            unFlagAllIslands();
            if (Clock.getBytecodesLeft() < 6000){
                returnEarly = true;
                return;
            }
        }
    }

    public static int countOfCarriersInVicinity() throws GameActionException{
        RobotInfo[] visibleAllies = rc.senseNearbyRobots(UNIT_TYPE.visionRadiusSquared, MY_TEAM);
        int count = 0;
        for (int i = visibleAllies.length; i-->0;){
            if (visibleAllies[i].type == RobotType.CARRIER) count++;
        }
        return count;
    }

    private static void resetCollectAnchorVariables(){
        collectAnchorHQidx = -1;
        goingToCollectAnchor = false;
        returnToHQ = false;
        movementDestination = null;
    }

    private static boolean isAnchorThereInHQ() throws GameActionException{
        assert rc.getAnchor() == null;
        assert movementDestination != null : "movementDestination isAnchorThereInHQ != null";
        assertHeadquarterLocation(movementDestination);
        boolean anchorProduced = Comms.findIfAnchorProduced(movementDestination);
        if (!anchorProduced) return false;
        returnToHQ = true;
        goingToCollectAnchor = true;
        System.out.println("Heeding anchor call from HQ");
        carrierStatus = Status.TRANSIT_ANCHOR_COLLECTION;
        return true;
    }

    private static boolean collectAnchorFromHQ() throws GameActionException{
        // TODO: Add anchor stuff for anchor.accelerating too...
        if (!isAnchorThereInHQ()) return false;
        int count = Comms.readMessageWithoutSHAFlag(collectAnchorHQidx);
        if (count == 0){
            assert collectAnchorHQidx != -1 : "collectanchorhqidx != -1   2";
            if (rc.readSharedArray(collectAnchorHQidx) != 0){
                Comms.wipeChannel(collectAnchorHQidx);
                System.out.println("False alarm, no anchor to collect. Wiping channel...");
            }
            carrierStatus = Status.ANCHOR_NOT_FOUND;
            resetCollectAnchorVariables();
            return false;
        }
        if (rc.canTakeAnchor(movementDestination, Anchor.STANDARD)){
            carrierStatus = Status.COLLECTING_ANCHOR;
            rc.takeAnchor(movementDestination, Anchor.STANDARD);
            assert rc.canWriteSharedArray(0, 0) : "canWriteSharedArray";
            Comms.writeSHAFlagMessage(count - 1, Comms.SHAFlag.COLLECT_ANCHOR, collectAnchorHQidx);
            System.out.println("Collected anchor from HQ!");
            resetCollectAnchorVariables();
            return false;
        }
        return true;
    }

    private static void transferResourcesToHQ() throws GameActionException{
        if (!returnToHQ) return;
        assert movementDestination != null;
        if (currentLocation.distanceSquaredTo(movementDestination) > 2) return;
        if (collectedElixir > 0 && rc.canTransferResource(movementDestination, ResourceType.ELIXIR, collectedElixir)){
            rc.transferResource(movementDestination, ResourceType.ELIXIR, collectedElixir);
            carrierStatus = Status.DEPOSITING_RESOURCES;
            collectedElixir = 0;
        }
        if (collectedMana > 0 && rc.canTransferResource(movementDestination, ResourceType.MANA, collectedMana)){
            rc.transferResource(movementDestination, ResourceType.MANA, collectedMana);
            carrierStatus = Status.DEPOSITING_RESOURCES;
            collectedMana = 0;
        }
        if (collectedAdamantium > 0 && rc.canTransferResource(movementDestination, ResourceType.ADAMANTIUM, collectedAdamantium)){
            rc.transferResource(movementDestination, ResourceType.ADAMANTIUM, collectedAdamantium);
            carrierStatus = Status.DEPOSITING_RESOURCES;
            collectedAdamantium = 0;
        }
        currentInventoryWeight = rc.getWeight();
        if (collectAnchorFromHQ()) return;
        if (currentInventoryWeight == 0){
            returnToHQ = false;
            movementDestination = null;
        }
    }

    private static void updateCarrier() throws GameActionException{
        if (desperationIndex > 5){
            // pathing.setAndMoveToDestination(explore());
            carrierStatus = Status.EXPLORE_FOR_WELLS;
            movementWrapper();
            desperationIndex--;
            return;
        }
        currentInventoryWeight = rc.getWeight();
        if (!returnToHQ && currentInventoryWeight == GameConstants.CARRIER_CAPACITY){
            returnToHQ = true;
            movementDestination = Comms.findNearestHeadquarter();
        }
        if (returnToHQ && movementDestination == null)
            movementDestination = Comms.findNearestHeadquarter();
        if (returnToHQ && !goingToCollectAnchor)
            carrierStatus = Status.TRANSIT_RES_DEP;
        transferResourcesToHQ();
        collectedResourcesThisTurn = false;
    }

    private static void resetIslandVariables(){
        movingToIsland = false;
        movementDestination = null;
        targetedIslandId = -1;
    }

    private static void setIslandDestination(MapLocation islandLoc, int islandId){
        movingToIsland = true;
        movementDestination = islandLoc;
        targetedIslandId = islandId;
    }

    private static void setIslandDestination(MapLocation islandLoc){
        movingToIsland = true;
        movementDestination = islandLoc;
        targetedIslandId = -1;
    }

    /**
     * Finds nearest square on nearest neutral island in vision. Very bytecode heavy
     * @return nearest location. null if none found.
     * @BytecodeCost : ~ 200 + 100 * (count of islands in vision)
     */
    public static MapLocation findNearestIslandInVision() throws GameActionException{
        int[] nearbyIslands = rc.senseNearbyIslands();
        MapLocation nearestLoc = null;
        int nearestDist = -1, curDist;
        for (int islandId : nearbyIslands){
            if (rc.senseTeamOccupyingIsland(islandId) != Team.NEUTRAL){
                islandViable[islandId - 1] = false;
                continue;
            }
            if (!islandViable[islandId - 1])
                islandViable[islandId - 1] = true;
            MapLocation[] locations = rc.senseNearbyIslandLocations(islandId);
            for (MapLocation loc : locations){
                curDist = currentLocation.distanceSquaredTo(loc);
                if (nearestLoc == null || curDist < nearestDist){
                    nearestLoc = loc;
                    nearestDist = curDist;
                }
            }
        }
        return nearestLoc;
    }

    private static MapLocation findNearestIslandInComms() throws GameActionException{
        MapLocation nearestLoc = null;
        int nearestDist = -1, curDist;
        for (int i = Comms.COMM_TYPE.ISLAND.channelStart; i < Comms.COMM_TYPE.ISLAND.channelStop; i++){
            int message = rc.readSharedArray(i);
            if (Comms.readSHAFlagFromMessage(message) != Comms.SHAFlag.UNOCCUPIED_ISLAND) continue;
            MapLocation loc = Comms.readLocationFromMessage(message);
            if (ignoreLocations[hashLocation(loc)]) continue;
            curDist = currentLocation.distanceSquaredTo(loc);
            if (nearestLoc == null || curDist < nearestDist){
                nearestLoc = loc;
                nearestDist = curDist;
            }
        }
        return nearestLoc;
    }

    /**
     * Finds an unoccupied island location for carrier to place its anchor.
     * @return MapLocation. If none found, returns null
     * @throws GameActionException
     * @BytecodeCost : Can't compute but very heavy
     */
    private static MapLocation getMeAnIslandLocation() throws GameActionException{
        MapLocation commsLoc = findNearestIslandInComms();
        if (commsLoc != null && rc.canSenseLocation(commsLoc)) return commsLoc;
        MapLocation senseLoc = findNearestIslandInVision();
        if (senseLoc != null && rc.canWriteSharedArray(0, 0))
            Comms.writeAndOverwriteLesserPriorityMessage(Comms.COMM_TYPE.ISLAND, senseLoc, Comms.SHAFlag.UNOCCUPIED_ISLAND);
        if (senseLoc != null && commsLoc != null){
            if (currentLocation.distanceSquaredTo(commsLoc) <= currentLocation.distanceSquaredTo(senseLoc))
                return commsLoc;
            return senseLoc;
        }
        else if (senseLoc != null) return senseLoc;
        else if (commsLoc != null) return commsLoc;
        return null;
    }

    private static boolean opportunisticAnchorPlacement() throws GameActionException{
        MapLocation loc = findNearestIslandInVision();
        if (loc == null) return false;
        setIslandDestination(loc, rc.senseIsland(loc));
        moveToIslandAndPlaceAnchor();
        return true;
    }

    private static int hashLocation(MapLocation loc){
        return loc.x * MAP_HEIGHT + loc.y;
    }

    private static void flagThisIsland(int islandId, MapLocation loc) throws GameActionException{
        islandViable[islandId - 1] = false;
        if (rc.canWriteSharedArray(0, 0))
            Comms.wipeThisLocationFromChannels(Comms.COMM_TYPE.ISLAND, Comms.SHAFlag.UNOCCUPIED_ISLAND, loc);
        else{
            int hash = hashLocation(loc);
            if (!ignoreLocations[hash]) ignoredIslandLocations[ignoredIslandLocationsCount++] = hash;
            
            assert ignoredIslandLocationsCount <= MAX_IGNORED_ISLAND_COUNT : "ignoredIslandLocationsCount <= MAX_IGNORED_ISLAND_COUNT";
            ignoreLocations[hashLocation(loc)] = true;
        }
    }

    private static boolean viableIslandCheck(boolean canSenseDest, MapLocation loc, int islandID) throws GameActionException{
        assert canSenseDest : "canSenseDest == true";
        if (rc.getID() == 10094 && rc.getRoundNum() == 1986 || rc.getRoundNum() == 1987)
            System.out.println("Island ID: " + islandID + " loc: " + loc);
        return rc.senseTeamOccupyingIsland(islandID) == Team.NEUTRAL;
    }

    private static void moveToIslandAndPlaceAnchor() throws GameActionException{
        assert movementDestination != null : "movementDestination != null";
        // assert targetedIslandId != -1 : "targetedIslandId != -1";
        assertNotHeadquarterLocation(movementDestination);
        int isCurLocIsland = rc.senseIsland(currentLocation);
        if (isCurLocIsland != -1 && rc.senseTeamOccupyingIsland(isCurLocIsland) == Team.NEUTRAL){
            if (rc.canPlaceAnchor()){
                // rc.setIndicatorString("A small dump for carrier, a huge jump for AnchorIslands! Placed Anchor!");
                carrierStatus = Status.PLACING_ANCHOR;
                System.out.println("Placed Anchor!");
                rc.placeAnchor();
                resetIslandVariables();
            }
            return;
        }
        boolean canSenseDest = rc.canSenseLocation(movementDestination);
        
        if (targetedIslandId == -1 && canSenseDest)
            targetedIslandId = rc.senseIsland(movementDestination);
        if (canSenseDest) assert targetedIslandId == rc.senseIsland(movementDestination) : "targetedIslandId == rc.senseIsland(movementDestination)";

        if (!canSenseDest || viableIslandCheck(canSenseDest, movementDestination, targetedIslandId)){
            // TODO: Can be removed if exceeding overall bytecode limits.
            if (!canSenseDest && opportunisticAnchorPlacement()) return;
            // pathing.setAndMoveToDestination(movementDestination);
            carrierStatus = Status.TRANSIT_TO_ISLAND;
            movementWrapper(movementDestination);
            return;
        }
        // rc.setIndicatorString("Targeted island already occupied. Finding new island...");
        carrierStatus = Status.OCCUPIED__FINDING_ANOTHER_ISLAND;
        assert targetedIslandId != -1 : "targetedIslandId != -1. targeted island occupied";
        // islandViable[targetedIslandId - 1] = false;
        assert movementDestination != null : "movementDestination != null";
        flagThisIsland(targetedIslandId, movementDestination);
        resetIslandVariables();
        // TODO: Might wanna add find and move command here later on.
    }

    private static void carrierAnchorMode() throws GameActionException{
        // I have an anchor. So singularly focusing on getting it to the first island I can find.
        // TODO: Add conditions and actions for behavior under attack.
        if (returnEarly) return;
        if (rc.canPlaceAnchor() && rc.senseAnchor(rc.senseIsland(rc.getLocation())) == null){
            carrierStatus = Status.PLACING_ANCHOR;
            rc.placeAnchor();
            resetIslandVariables();
            getAndSetWellLocation();
            goToWell();
            return;
        }
        if (movingToIsland){
            carrierStatus = Status.TRANSIT_TO_ISLAND;
            moveToIslandAndPlaceAnchor();
            return;
        }
        MapLocation islandLoc = getMeAnIslandLocation();
        if (islandLoc == null){
            rc.setIndicatorString("can't find an island in vision or in comms. Exploring...");
            // pathing.setAndMoveToDestination(explore());
            carrierStatus = Status.EXPLORE_FOR_ISLANDS;
            movementWrapper();
            return;
        }
        assertNotHeadquarterLocation(islandLoc);
        setIslandDestination(islandLoc);
        moveToIslandAndPlaceAnchor();
    }

    private static void collectResources() throws GameActionException{
        if (!rc.isActionReady() || returnToHQ) return;
        WellInfo[] adjacentWells = rc.senseNearbyWells(2);
        if (adjacentWells.length == 0) return;
        int i = rng.nextInt(adjacentWells.length), amount;
        
        WellInfo curWell = adjacentWells[i];
        amount = Math.min(curWell.getRate(), GameConstants.CARRIER_CAPACITY - rc.getWeight());
        rc.collectResource(curWell.getMapLocation(), amount);
        currentInventoryWeight += amount;
        switch(curWell.getResourceType()){
            case ADAMANTIUM: collectedAdamantium += amount; break;
            case MANA: collectedMana += amount; break;
            case ELIXIR: collectedElixir += amount; break;
            default: assert false;
        }
        carrierStatus = Status.COLLECTING_RESOURCES;
        collectedResourcesThisTurn = true;
        desperationIndex = 0;
        
        if (currentInventoryWeight >= GameConstants.CARRIER_CAPACITY){
            returnToHQ = true;
            movementDestination = Comms.findNearestHeadquarter();
        }
    }

    private static void attackIfAboutToDie() throws GameActionException{
        // if (returnEarly) return;
        if (rc.getWeight() < 5) // Can't even attack...
            return;
        else if (returnToHQ && movementDestination != null && currentLocation.distanceSquaredTo(movementDestination) <= 2)
            return;
        else if (visibleEnemies.length == 0 || !rc.isActionReady())
            return;
        else{
            double possibleDamage = Math.round(rc.getWeight()/5.0);
            double bestScore = -1.0;
            MapLocation attackLocation = null;
            MapLocation outOfRangeEnemyLoc = null;
            double outOfRangeEnemyScore = -1.0;
            for (int i = visibleEnemies.length; --i >= 0;){
                RobotInfo curEnemy = visibleEnemies[i];
                if (curEnemy.getType() != RobotType.HEADQUARTERS){
                    if (rc.canAttack(curEnemy.location)){
                        double curScore = curEnemy.health - possibleDamage;
                        if (curScore <=0)   
                            curScore += 1000;
                        if (curScore > bestScore){
                            bestScore = curScore;
                            attackLocation = curEnemy.location;
                        }
                    }
                    else{
                        double curScore = curEnemy.health - possibleDamage;
                        if (curScore <=0)   
                            curScore += 1000;
                        if (curScore > outOfRangeEnemyScore){
                            outOfRangeEnemyScore = curScore;
                            outOfRangeEnemyLoc = curEnemy.location;
                        }
                    }
                }
            }
            if (attackLocation != null){
                rc.attack(attackLocation);
                carrierStatus = Status.ATTACKING;
                return;
            }
            // else if (rc.getWeight() > 20 && outOfRangeEnemyLoc != null && rc.isMovementReady()){
            //     movementWrapper(outOfRangeEnemyLoc);
            //     if (rc.canAttack(outOfRangeEnemyLoc)){
            //         rc.attack(outOfRangeEnemyLoc);
            //         carrierStatus = Status.ATTACKING;
            //         return;
            //     }
            // }
        }
    }

    private static void setWellDestination(MapLocation loc){
        movementDestination = loc;
        inPlaceForCollection = currentLocation.distanceSquaredTo(loc) <= 2;
        desperationIndex = 0;
    }

    /**
     * Finds nearest well in vision.
     * @return nearest well in vision if one exists. Returns null otherwise
     * @throws GameActionException
     * @BytecodeCost : ~ 100 + 10 * [well count in vision]
     */
    private static MapLocation findNearestWellInVision() throws GameActionException{
        WellInfo[] nearbyWells = rc.senseNearbyWells();
        MapLocation nearestLoc = null;
        int nearestDist = -1, curDist;
        for (WellInfo well : nearbyWells){
            MapLocation loc = well.getMapLocation();
            curDist = currentLocation.distanceSquaredTo(loc);
            if (nearestLoc == null || curDist < nearestDist){
                nearestLoc = loc;
                nearestDist = curDist;
            }
        }
        return nearestLoc;
    }

    /**
     * Finds nearest well of the given type in vision.
     * @return nearest well in vision if one exists. Returns null otherwise
     * @throws GameActionException
     * @BytecodeCost : ~ 100 + 10 * [well count in vision]
     */
    public static MapLocation findNearestWellInVision(ResourceType resourceType) throws GameActionException{
        WellInfo[] nearbyWells = rc.senseNearbyWells(resourceType);
        MapLocation nearestLoc = null;
        int nearestDist = -1, curDist;
        for (WellInfo well : nearbyWells){
            MapLocation loc = well.getMapLocation();
            curDist = currentLocation.distanceSquaredTo(loc);
            if (nearestLoc == null || curDist < nearestDist){
                nearestLoc = loc;
                nearestDist = curDist;
            }
        }
        return nearestLoc;
    }

    /**
     * Find a Well location from which resources are to be collected. First try to find in comms. If that location is null or not in vision, try to find out if there's a location in vision that is better
     * @throws GameActionException
     * @BytecodeCost : ~ 350
     */
    private static void getAndSetWellLocation() throws GameActionException{
        MapLocation commsLoc = Comms.findNearestLocationOfThisType(currentLocation, Comms.COMM_TYPE.WELLS, Comms.resourceFlag(prioritizedResource));
        if (commsLoc != null && rc.canSenseLocation(commsLoc)){
            setWellDestination(commsLoc);
            return;
        }
        MapLocation senseLoc = findNearestWellInVision(prioritizedResource);
        if (senseLoc != null && rc.canWriteSharedArray(0, 0))
            Comms.writeAndOverwriteLesserPriorityMessage(Comms.COMM_TYPE.WELLS, senseLoc, Comms.resourceFlag(prioritizedResource));
        if (senseLoc != null && commsLoc != null){
            if (currentLocation.distanceSquaredTo(commsLoc) <= currentLocation.distanceSquaredTo(senseLoc))
                setWellDestination(commsLoc);
            else setWellDestination(senseLoc);
        }
        else if (senseLoc != null) setWellDestination(senseLoc);
        else if (commsLoc != null) setWellDestination(commsLoc);
        else{
            movementDestination = null;
            inPlaceForCollection = false;
        }
    }

    private static void goToWell() throws GameActionException{
        if (inPlaceForCollection){
            collectResources();
            return;
        }
        if (!rc.isMovementReady() || movementDestination == null) return;
        int curDist = currentLocation.distanceSquaredTo(movementDestination);
        if (curDist <= 2) { // Reached location
            if (!rc.isLocationOccupied(movementDestination)){
                Direction dir = currentLocation.directionTo(movementDestination);
                if (rc.canMove(dir)){
                    rc.move(dir);
                    inPlaceForCollection = true;
                }
            }
            else inPlaceForCollection = true;
            collectResources();
            return;
        }
        if (desperationIndex == 5)
            desperationIndex = 12;
        // If outside of action radius
        if (!rc.canActLocation(movementDestination)){
            rc.setIndicatorString("moving to well: " + movementDestination + "; despId: " + desperationIndex);
            // pathing.setAndMoveToDestination(movementDestination);
            carrierStatus = Status.TRANSIT_TO_WELL;
            movementWrapper(movementDestination);
            return;
        }
        // pathing.setAndMoveToDestination(movementDestination);
        movementWrapper(movementDestination);
        if (desperationIndex < 5) return;
        if (rc.canWriteSharedArray(0, 0))
            Comms.wipeThisLocationFromChannels(Comms.COMM_TYPE.WELLS, Comms.resourceFlag(prioritizedResource), movementDestination);
        desperationIndex = 12;
        movementDestination = null;
        carrierStatus = Status.DESPERATE;
        inPlaceForCollection = false;
    }

    private static void gatherResources() throws GameActionException{
        // TODO: Add conditions and actions for behavior under attack here too.
        if (returnEarly) return;
        if (movingToIsland) resetIslandVariables();
        updateCarrier();
        if (rc.getAnchor() != null){
            carrierAnchorMode();
            return;
        }
        if (desperationIndex > 5) return;
        if (returnToHQ){
            assert movementDestination != null : "movementDestination != null in gather resources";
            if (goingToCollectAnchor)
                carrierStatus = Status.TRANSIT_ANCHOR_COLLECTION;
            else carrierStatus = Status.TRANSIT_RES_DEP;
            movementWrapper(movementDestination);
            return;
        }
        collectResources();
        if (collectedResourcesThisTurn) return;
        if (movementDestination != null){
            goToWell();
            collectResources();
        }
        else{
            getAndSetWellLocation();
            if (Clock.getBytecodesLeft() < 2000) return;
            goToWell();
            collectResources();
        }
        if (!collectedResourcesThisTurn && rc.isMovementReady())
            desperationIndex++;
        else desperationIndex = 0;
    }

    private static void endOfTurnUpdate() throws GameActionException{
        returnEarly = false;
        if (carrierStatus == Status.TRANSIT_TO_WELL || carrierStatus == Status.TRANSIT_TO_ISLAND)
            rc.setIndicatorString(carrierStatus.toString() + " " + movementDestination.toString());
        else
            rc.setIndicatorString(carrierStatus.toString());
        if (Clock.getBytecodesLeft() > 700){
            RobotInfo[] visibleEnemies = rc.senseNearbyRobots(-1, ENEMY_TEAM);
            CombatUtils.sendGenericCombatLocation(visibleEnemies);
        }
    }

    private static Direction getRetreatDirection(RobotInfo[] visibleHostiles) throws GameActionException{
        int closestHostileDistSq = Integer.MAX_VALUE;
        MapLocation lCR = rc.getLocation();
        for (RobotInfo hostile : visibleHostiles) {
            if (!hostile.type.canAttack() && hostile.type != RobotType.HEADQUARTERS) continue;
            int distSq = lCR.distanceSquaredTo(hostile.location);
            if (distSq < closestHostileDistSq) {
                closestHostileDistSq = distSq;
            }
        }
        Direction bestRetreatDir = null;
        int bestDistSq = closestHostileDistSq;
        // int bestRubble = rc.senseRubble(rc.getLocation());

        for (Direction dir : directions) {
            if (!rc.canMove(dir)) continue;
            MapLocation dirLoc = lCR.add(dir);
            // int dirLocRubble = rc.senseRubble(dirLoc);
            // if (dirLocRubble > bestRubble) continue; // Don't move to even more rubble

            int smallestDistSq = Integer.MAX_VALUE;
            for (RobotInfo hostile : visibleHostiles) {
                if (!hostile.type.canAttack()) continue;
                int distSq = hostile.location.distanceSquaredTo(dirLoc);
                if (distSq < smallestDistSq) {
                    smallestDistSq = distSq;
                }
            }
            if (smallestDistSq > bestDistSq) {
                bestDistSq = smallestDistSq;
                bestRetreatDir = dir;
                // bestRubble = dirLocRubble;
            }
        }
        return bestRetreatDir;
    }

    private static boolean tryToFlee(RobotInfo[] visibleHostiles) throws GameActionException {
        int closestHostileDistSq = Integer.MAX_VALUE;
        MapLocation lCR = rc.getLocation();
        for (RobotInfo hostile : visibleHostiles) {
            if (!hostile.type.canAttack() && hostile.type != RobotType.HEADQUARTERS) continue;
            int distSq = lCR.distanceSquaredTo(hostile.location);
            if (distSq < closestHostileDistSq) {
                closestHostileDistSq = distSq;
            }
        }
        
        // We don't want to get out of our max range
        // if (closestHostileDistSq > rc.getType().actionRadiusSquared) return false;
        
        Direction bestRetreatDir = null;
        int bestDistSq = closestHostileDistSq;
        // int bestRubble = rc.senseRubble(rc.getLocation());

        for (Direction dir : directions) {
            if (!rc.canMove(dir)) continue;
            MapLocation dirLoc = lCR.add(dir);
            // int dirLocRubble = rc.senseRubble(dirLoc);
            // if (dirLocRubble > bestRubble) continue; // Don't move to even more rubble

            int smallestDistSq = Integer.MAX_VALUE;
            for (RobotInfo hostile : visibleHostiles) {
                if (!hostile.type.canAttack()) continue;
                int distSq = hostile.location.distanceSquaredTo(dirLoc);
                if (distSq < smallestDistSq) {
                    smallestDistSq = distSq;
                }
            }
            if (smallestDistSq > bestDistSq) {
                bestDistSq = smallestDistSq;
                bestRetreatDir = dir;
                // bestRubble = dirLocRubble;
            }
        }
        if (bestRetreatDir != null) {
            // rc.setIndicatorString("Backing: " + bestRetreatDir);
            carrierStatus = Status.FLEEING;
            rc.move(bestRetreatDir);
            return true;
        }
        return false;
    }

    private static void fleeIfNeedTo() throws GameActionException{
        if (!TRY_TO_FLEE || !isFleeing) return;
        if (!rc.isMovementReady()) // Can't do anything...
            return;
        Direction dir = getRetreatDirection(visibleEnemies);
        if (dir != null) Explore.assignExplore3Dir(dir);
        movementWrapper(true);
    }

    public static void runCarrier() throws GameActionException{
        updateOverall();
        attackIfAboutToDie();
        if (rc.getAnchor() != null)
            carrierAnchorMode();
        else
            gatherResources();
        fleeIfNeedTo();
        endOfTurnUpdate();
    }
}
