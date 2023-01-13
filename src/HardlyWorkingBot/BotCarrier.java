package HardlyWorkingBot;

import HardlyWorkingBot.Comms.SHAFlag;
import battlecode.common.*;

public class BotCarrier extends Explore{

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

    // public static boolean isMinedThisTurn;
    // public static int numOfMiners;
    // private static final int maxCapacity = GameConstants.CARRIER_CAPACITY;
    // private static MapLocation miningLocation;
    // private static boolean inPlaceForMining;
    // public static boolean commitSuicide;
    // private static MapLocation suicideLocation;
    // public static int desperationIndex;
    // private static final int MIN_SUICIDE_DIST = 4;
    // public static boolean prolificMiningLocationsAtBirth;
    // private static boolean moveOut;
    // private static boolean isFleeing;
    // private static final boolean searchByDistance = false;
    // private static final int randomPersistance = 20;
    // private static boolean tooCrowded;
    // private static final int CROWD_LIMIT = 5;
    // private static final int LOW_HEALTH_STRAT_TRIGGER = (int)((MAX_HEALTH*3.0d)/10.0d);
    // private static final int LOW_HEALTH_STRAT_RELEASER = (int)((MAX_HEALTH*8.0d)/10.0d);
    // public static boolean lowHealthStratInPlay = false;
    // public static MapLocation lowHealthStratArchon;
    // public static int fleeCount;
    // private static MapLocation finalDestination = null; 
    // private static boolean DEBUG_MODE = false;

    // private static WellInfo[] nearbyWells = null;

    public static void initCarrier() throws GameActionException{
        movingToIsland = false;
        movementDestination = null;
        targetedIslandId = -1;
        currentInventoryWeight = rc.getWeight();
        collectedResourcesThisTurn = false;
        collectedAdamantium = 0;
        collectedMana = 0;
        collectedElixir = 0;
        inPlaceForCollection = false;
        // moveOut = false;
        desperationIndex = 0;
        goingToCollectAnchor = false;
        collectAnchorHQidx = -1;

        // nearbyWells = rc.senseNearbyWells();
        // resetVariables();
        // lowHealthStratInPlay = false;
        // lowHealthStratArchon = null;
        // fleeCount = 0;
    }

    // public static boolean areMiningLocationsAbundant(){
    //     try{
    //         return (nearbyWells.length > 30);
    //     } catch (Exception e){
    //         e.printStackTrace();
    //     }
    //     return false;
    // }

    // TODO: Update this with average mining per turn of the location
    public static boolean isLocationBeingMined(MapLocation loc) throws GameActionException{
        MapLocation[] locAdjacentLocations = rc.getAllLocationsWithinRadiusSquared(loc, UNIT_TYPE.actionRadiusSquared);
        for (int i = locAdjacentLocations.length; i-->0;){
            MapLocation curLoc = locAdjacentLocations[i];
            if (!rc.canSenseLocation(curLoc)) continue;
            RobotInfo bot = rc.senseRobotAtLocation(curLoc);
            if (bot != null && bot.team == MY_TEAM && bot.type == RobotType.CARRIER) return true;
        }
        return false;
    }

    public static void getExploreDir() throws GameActionException{
        Direction away = directionAwayFromAllRobots();
        if (away != null){
            assignExplore3Dir(away);
            return;
        }
        MapLocation closestHQ = Comms.findNearestHeadquarter();
        if (rc.canSenseLocation(closestHQ)) 
            assignExplore3Dir(closestHQ.directionTo(currentLocation));
        else
            assignExplore3Dir(directions[Globals.rng.nextInt(8)]);
    }

    public static MapLocation explore() throws GameActionException{
        if (exploreDir == CENTER)
            getExploreDir();
        return getExplore3Target();
    }

    /**
     * Updates list of visible enemies found in vision
     * @throws GameActionException
     * @BytecodeCost : 100
     */
    private static void updateVision() throws GameActionException {
        visibleEnemies = rc.senseNearbyRobots(UNIT_TYPE.visionRadiusSquared, ENEMY_TEAM);
    }

    private static void updateOverall() throws GameActionException{
        updateVision();
        MapLocation loc = null;
        if (!goingToCollectAnchor)
            loc = Comms.findIfAnchorProduced();
        if (loc != null){
            returnToHQ = true;
            movementDestination = loc;
            goingToCollectAnchor = true;
            System.out.println("going to collect anchor at " + movementDestination);
            System.out.println("collect anchor hq id: " + collectAnchorHQidx);
            assert collectAnchorHQidx != -1 : "collectAnchorHQidx != -1";
        }
    }

    // TODO: Shift to CombatUtils
    // private static boolean checkIfEnemyHQInVision() throws GameActionException{
    //     for (int i = visibleEnemies.length; i-->0;){
    //         RobotInfo bot = visibleEnemies[i];
    //         if (bot.type == RobotType.HEADQUARTERS){
    //             if (rc.canWriteSharedArray(0, 0))
    //                 Comms.writeAndOverwriteLesserPriorityMessage(Comms.COMM_TYPE.COMBAT, bot.getLocation(), Comms.SHAFlag.ENEMY_HEADQUARTER_LOCATION);
    //             return true;
    //         }
    //     }
    //     return false;
    // }

    // private static void resetVariables(){
    //     miningLocation = null;
    //     inPlaceForMining = false;
    //     commitSuicide = false;
    //     suicideLocation = null;
    //     desperationIndex = 0;
    //     moveOut = true; 
    //     isFleeing = false;
    // }


    public static int countOfCarriersInVicinity() throws GameActionException{
        RobotInfo[] visibleAllies = rc.senseNearbyRobots(UNIT_TYPE.visionRadiusSquared, MY_TEAM);
        int count = 0;
        for (int i = visibleAllies.length; i-->0;){
            if (visibleAllies[i].type == RobotType.CARRIER) count++;
        }
        return count;
    }

    public static void opportunisticGathering() throws GameActionException{
        // if (movementDestination == null || rc.canSenseLocation(movementDestination)) return;
        // MapLocation[] nearbyLocations = rc.senseNearbyLocationsWithGold();
        // if (nearbyLocations.length > 0){ 
        //     miningLocation = findOptimalLocationForMiningGold(nearbyLocations);
        //     inPlaceForMining = (rc.getLocation().distanceSquaredTo(miningLocation) <= 2);
        //     return;
        // }
        // if (!depleteMine) nearbyLocations = rc.senseNearbyLocationsWithLead(MINER_VISION_RADIUS, 20);
        // else nearbyLocations = rc.senseNearbyLocationsWithLead();
        // if (nearbyLocations.length > 0){ 
        //     miningLocation = findOptimalLocationForMiningLead(nearbyLocations);
        //     if (miningLocation != null) inPlaceForMining = (rc.getLocation().distanceSquaredTo(miningLocation) <= 2);
        //     else inPlaceForMining = false;
        //     return;
        // }
    }

    private static void resetCollectAnchorVariables(){
        collectAnchorHQidx = -1;
        goingToCollectAnchor = false;
        returnToHQ = false;
        movementDestination = null;
    }

    private static void collectAnchorFromHQ() throws GameActionException{
        // TODO: Add anchor stuff for anchor.accelerating too...
        if (rc.getNumAnchors(Anchor.STANDARD) == 0){
            assert collectAnchorHQidx != -1 : "collectanchorhqidx != -1   2";
            Comms.wipeChannel(collectAnchorHQidx);
            resetCollectAnchorVariables();
            return;
        }
        if (rc.canTakeAnchor(movementDestination, Anchor.STANDARD)){
            rc.takeAnchor(movementDestination, Anchor.STANDARD);
            resetCollectAnchorVariables();
        }
    }

    private static void transferResourcesToHQ() throws GameActionException{
        if (!returnToHQ) return;
        assert movementDestination != null;
        if (currentLocation.distanceSquaredTo(movementDestination) > 2) return;
        if (goingToCollectAnchor){
            collectAnchorFromHQ();
            return;
        }
        if (collectedElixir > 0 && rc.canTransferResource(movementDestination, ResourceType.ELIXIR, collectedElixir)){
            rc.transferResource(movementDestination, ResourceType.ELIXIR, collectedElixir);
            collectedElixir = 0;
        }
        if (collectedMana > 0 && rc.canTransferResource(movementDestination, ResourceType.MANA, collectedMana)){
            rc.transferResource(movementDestination, ResourceType.MANA, collectedMana);
            collectedMana = 0;
        }
        if (collectedAdamantium > 0 && rc.canTransferResource(movementDestination, ResourceType.ADAMANTIUM, collectedAdamantium)){
            rc.transferResource(movementDestination, ResourceType.ADAMANTIUM, collectedAdamantium);
            collectedAdamantium = 0;
        }
        currentInventoryWeight = rc.getWeight();
        if (currentInventoryWeight == 0){
            returnToHQ = false;
            movementDestination = null;
        }
    }

    private static void updateCarrier() throws GameActionException{
        if (desperationIndex > 5){
            pathing.setAndMoveToDestination(explore());
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
        transferResourcesToHQ();
        collectedResourcesThisTurn = false;

        // moveOut = true;
        // tooCrowded = false;
        // if (CombatUtils.militaryCount(visibleEnemies) == 0)
        //     fleeCount = Math.max(0, fleeCount - 1);
        // if (fleeCount == 0) isFleeing = false;
        // else isFleeing = true;
        // checkIfEnemyHQInVision();
        // if (!isSafeToMine(currentLocation)){
        //     isFleeing = true;
        //     miningLocation = null;
        //     inPlaceForMining = false;
        //     isFleeing = CombatUtils.tryToBackUpToMaintainMaxRangeMiner(visibleEnemies);
        //     fleeCount = 5;
        // }
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
            if (rc.senseTeamOccupyingIsland(islandId) != Team.NEUTRAL) continue;
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

    /**
     * Finds an unoccupied island location for carrier to place its anchor.
     * @return MapLocation. If none found, returns null
     * @throws GameActionException
     * @BytecodeCost : Can't compute but very heavy
     */
    private static MapLocation getMeAnIslandLocation() throws GameActionException{
        MapLocation commsLoc = Comms.findNearestLocationOfThisType(currentLocation, Comms.COMM_TYPE.ISLAND, SHAFlag.UNOCCUPIED_ISLAND);
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

    private static void moveToIslandAndPlaceAnchor() throws GameActionException{
        assert movementDestination != null : "movementDestination != null";
        // assert targetedIslandId != -1 : "targetedIslandId != -1";
        int isCurLocIsland = rc.senseIsland(currentLocation);
        if (isCurLocIsland != -1 && rc.senseTeamOccupyingIsland(isCurLocIsland) == Team.NEUTRAL){
            if (rc.canPlaceAnchor()){
                rc.setIndicatorString("A small dump for carrier, a huge jump for AnchorIslands! Placed Anchor!");
                rc.placeAnchor();
                resetIslandVariables();
            }
            return;
        }
        boolean canSenseDest = rc.canSenseLocation(movementDestination);
        
        if (targetedIslandId == -1 && canSenseDest)
            targetedIslandId = rc.senseIsland(movementDestination);

        if (!canSenseDest || rc.senseTeamOccupyingIsland(targetedIslandId) == Team.NEUTRAL){
            // TODO: Can be removed if exceeding overall bytecode limits.
            if (!canSenseDest && opportunisticAnchorPlacement()) return;
            pathing.setAndMoveToDestination(movementDestination);
            return;
        }
        rc.setIndicatorString("Targeted island already occupied. Finding new island...");
        resetIslandVariables();
        carrierAnchorMode();
    }

    private static void carrierAnchorMode() throws GameActionException{
        // I have an anchor. So singularly focusing on getting it to the first island I can find.
        // TODO: Add conditions and actions for behavior under attack.
        if (movingToIsland){
            moveToIslandAndPlaceAnchor();
            return;
        }
        MapLocation islandLoc = getMeAnIslandLocation();
        if (islandLoc == null){
            rc.setIndicatorString("can't find an island in vision or in comms. Exploring...");
            pathing.setAndMoveToDestination(explore());
            return;
        }
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
        collectedResourcesThisTurn = true;
        
        if (currentInventoryWeight == GameConstants.CARRIER_CAPACITY){
            returnToHQ = true;
            movementDestination = Comms.findNearestHeadquarter();
        }
    }

    private static void attackIfAboutToDie() throws GameActionException{
        // TODO: Figure out what to do here
        if (rc.getWeight() == 0) // Can't even attack...
            return;
        if (returnToHQ && movementDestination != null && currentLocation.distanceSquaredTo(movementDestination) <= 2)
            return;
    }

    private static void setWellDestination(MapLocation loc){
        movementDestination = loc;
        inPlaceForCollection = currentLocation.distanceSquaredTo(loc) <= 2;
    }

    private static MapLocation findNearestWellInVision() throws GameActionException{
        WellInfo[] nearbyWells = rc.senseNearbyWells();
        MapLocation nearestLoc = null;
        int nearestDist = -1, curDist;
        for (WellInfo well : nearbyWells){
            // if (rc.senseTeamOccupyingIsland(islandId) != Team.NEUTRAL) continue;
            MapLocation loc = well.getMapLocation();
            curDist = currentLocation.distanceSquaredTo(loc);
            if (nearestLoc == null || curDist < nearestDist){
                nearestLoc = loc;
                nearestDist = curDist;
            }
        }
        return nearestLoc;
    }

    private static void getAndSetWellLocation() throws GameActionException{
        MapLocation commsLoc = Comms.findNearestLocationOfThisType(currentLocation, Comms.COMM_TYPE.WELLS, SHAFlag.WELL_LOCATION);
        if (commsLoc != null && rc.canSenseLocation(commsLoc)){
            setWellDestination(commsLoc);
            return;
        }
        MapLocation senseLoc = findNearestWellInVision();
        if (senseLoc != null && rc.canWriteSharedArray(0, 0))
            Comms.writeAndOverwriteLesserPriorityMessage(Comms.COMM_TYPE.WELLS, senseLoc, Comms.SHAFlag.WELL_LOCATION);
        if (senseLoc != null && commsLoc != null){
            if (currentLocation.distanceSquaredTo(commsLoc) <= currentLocation.distanceSquaredTo(senseLoc))
                setWellDestination(commsLoc);
            else setWellDestination(senseLoc);
        }
        else if (senseLoc != null) setWellDestination(senseLoc);
        else if (commsLoc != null) setWellDestination(commsLoc);
    }

    private static void goToWell() throws GameActionException{
        // moveOut = false;
        if (inPlaceForCollection){
            collectResources();
            return;
        }
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
        // If outside of vision or Location is not occupied:
        if (!rc.canActLocation(movementDestination)){
            rc.setIndicatorString("moving to well: " + movementDestination);
            pathing.setAndMoveToDestination(movementDestination);
            return;
        }
        pathing.setAndMoveToDestination(movementDestination);
        if (desperationIndex < 5) return;
        if (rc.canWriteSharedArray(0, 0))
            Comms.wipeThisLocationFromChannels(Comms.COMM_TYPE.WELLS, Comms.SHAFlag.WELL_LOCATION, movementDestination);
        desperationIndex = 12;
        movementDestination = null;
        inPlaceForCollection = false;
        pathing.setAndMoveToDestination(explore());
        desperationIndex--;
    }

    private static void gatherResources() throws GameActionException{
        // TODO: Add conditions and actions for behavior under attack here too.
        if (movingToIsland) resetIslandVariables();
        updateCarrier();
        collectResources();
        if (desperationIndex > 5) return;
        if (returnToHQ){
            assert movementDestination != null : "movementDestination != null in gather resources";
            pathing.setAndMoveToDestination(movementDestination);
            return;
        }
        if (collectedResourcesThisTurn) return;
        opportunisticGathering();
        if (inPlaceForCollection)
            collectResources();
        else if (movementDestination != null){
            goToWell();
            collectResources();
        }
        else{
            getAndSetWellLocation();
            goToWell();
            collectResources();
        }
        if (!rc.canMove(NORTH) || !rc.canMove(SOUTH) || !rc.canMove(EAST) || !rc.canMove(WEST))
            desperationIndex = 0;
        else desperationIndex++;
    }

    public static void runCarrier() throws GameActionException{
        updateOverall();
        attackIfAboutToDie();
        if (rc.getAnchor() != null)
            carrierAnchorMode();
        else
            gatherResources();
    }
}
