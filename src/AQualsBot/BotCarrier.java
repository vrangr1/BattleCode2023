package AQualsBot;

import battlecode.common.*;
import java.util.Random;

import AQualsBot.path.Nav;

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
        FLEEING,
        SEARCHING_FOR_WELL,
        TRANSIT_TO_MANA_WELL_FOR_ELIXIR,
        NORMAL,
        TOO_MUCH_BYTECODES_RET_EARLY,
    }

    private static boolean movingToIsland = false;
    private static MapLocation movementDestination = null;
    private static int targetedIslandId = -1;
    private static int currentInventoryWeight = -1;
    private static RobotInfo[] visibleEnemies;
    private static boolean returnToHQ = false;
    private static boolean collectedResourcesThisTurn = false;
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
    private static MapLocation exploreDest1, exploreDest2;
    private static final int EXCESS_RESOURCES = 3;
    private static final int INCREASE_RESOURCE_COLLECTION_ROUND = 0;
    private static boolean INITIAL_MINE_ONLY_MANA_STRAT = true;
    private static int MINE_ONLY_MANA_TILL_ROUND;
    private static boolean exploringForWells = false;
    private static MapLocation fleeTarget = null;
    public static MapLocation otherTypeWell;
    private static Direction lastRetreatDirection = null;
    private static final int FLEE_EXTRAPOLATE_UNSQUARED_DISTANCE = 4;
    private static int FLEE_ROUNDS = 10;
    private static final boolean USING_CIRCULAR_EXPLORATION = true;
    private static final boolean STORING_EXPLORED_LOCATIONS = false;
    private static MapLocation[] exploredLocations;
    private static final int EXPLORED_LOCATIONS_MAX_SIZE = 10;
    private static int exploredLocationsCount = 0;
    private static int movesLeftBeforeDeath = -1;
    public static final boolean DEBUG_PRINT = false;
    private static final boolean DOING_EARLY_MANA_DEPOSITION = true;
    private static final int EARLY_MANA_DEPOSTION_THRESHOLD = 15;
    private static int GEFFNERS_EXPLORE_TRIGGER = 12;
    private static int persistance = 0;
    private static final int MAX_PERSISTANCE = 15;
    private static final int MIN_EXPLORE_ROUND_COUNT = 3;
    private static int exploreRoundCount = 0;

    public static int initSpawningHeadquarterIndex(int index) throws GameActionException{
        MapLocation loc = Comms.findKthNearestHeadquarter(index + 1);
        hqIndex = Comms.getHeadquarterIndex(loc);
        hqIndex = Comms.START_CHANNEL_BANDS + hqIndex * Comms.CHANNELS_COUNT_PER_HEADQUARTER + 2;
        return hqIndex;
    }

    private static void setInitialMineOnlyManaStrat(){
        if (MAP_SIZE < 1000) INITIAL_MINE_ONLY_MANA_STRAT = false;
        else if (MAP_SIZE < 1400) INITIAL_MINE_ONLY_MANA_STRAT = false;
        else if (MAP_SIZE < 1600) INITIAL_MINE_ONLY_MANA_STRAT = true;
        else INITIAL_MINE_ONLY_MANA_STRAT = true;
    }

    private static void setGeffnersExploreTrigger(){
        if (MAP_SIZE < 1000) GEFFNERS_EXPLORE_TRIGGER = 20;
        else if (MAP_SIZE <= 1600) GEFFNERS_EXPLORE_TRIGGER = 15;
        else GEFFNERS_EXPLORE_TRIGGER = 12;
    }

    private static void setMineOnlyManaRoundLimit(){
        if (MAP_SIZE < 1000) MINE_ONLY_MANA_TILL_ROUND = 125;
        else if (MAP_SIZE < 1600) MINE_ONLY_MANA_TILL_ROUND = 70;
        else MINE_ONLY_MANA_TILL_ROUND = 50;
    }

    private static void setFleeRounds(){
        if (MAP_SIZE < 1000) FLEE_ROUNDS = 5;
        else if (MAP_SIZE < 1600) FLEE_ROUNDS = 7;
        else FLEE_ROUNDS = 10;
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
        desperationIndex = 0;
        goingToCollectAnchor = false;
        collectAnchorHQidx = -1;
        returnEarly = false;
        otherTypeWell = null;
        exploreDest1 = null;
        exploreDest2 = null;
        exploringForWells = false;
        fleeTarget = null;
        lastRetreatDirection = null;
        setInitialMineOnlyManaStrat();
        setGeffnersExploreTrigger();
        movesLeftBeforeDeath = 2 * (rc.getHealth() / RobotType.LAUNCHER.damage);
        setMineOnlyManaRoundLimit();
        setFleeRounds();
        rng = new Random(rc.getID());
        // prioritizedResource = (rc.getID() % 2 == 0) ? ResourceType.ADAMANTIUM : ResourceType.MANA; // TODO: Change this
        initSpawningHeadquarterIndex(0);
        prioritizedResource = Comms.getPrioritizedResource(hqIndex, 0);
        isFleeing = false;
        fleeCount = 0;
        islandViable = new boolean[ISLAND_COUNT];
        ignoredIslandLocations = new int[MAX_IGNORED_ISLAND_COUNT];
        ignoredIslandLocationsCount = 0;
        for (int i = 0; i < ISLAND_COUNT; i++)
            islandViable[i] = true;
        ignoreLocations = new boolean[MAP_HEIGHT * MAP_WIDTH];
        if (Clock.getBytecodesLeft() < 3000){
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

    private static boolean limitReachedFindInCommsNow() throws GameActionException{
        Comms.SHAFlag flag = Comms.resourceFlag(getLocalPrioritizedResource());
        movementDestination = Comms.findNearestLocationOfThisType(rc.getLocation(), Comms.COMM_TYPE.WELLS, flag);
        if (movementDestination == null){
            Explore.resetExploreRoundCount();
            return false;
        }
        exploringForWells = false;
        carrierStatus = Status.TRANSIT_TO_WELL;
        setWellDestination(movementDestination);
        Comms.writeOrSaveLocation(movementDestination, flag);
        return true;
    }

    private static MapLocation ifExploreDestNullStuff(MapLocation loc) throws GameActionException{
        if (loc == null && limitReachedFindInCommsNow()){
            movementWrapper(movementDestination);
            return null;
        }
        else if (loc == null){
            loc = Explore.explore(randomExploration);
            assert loc != null;
            return loc;
        }
        return loc;
    }

    private static void movementWrapper(MapLocation dest) throws GameActionException{
        if (rc.isMovementReady() && Clock.getBytecodesLeft() > 3000){
            pathing.setAndMoveToDestination(dest);
        }
        // if (!returnToHQ && rc.getLocation().distanceSquaredTo(dest) <= 2 && rc.onTheMap(dest) && rc.isLocationOccupied(dest))
        //     collectResources();
        // if (!collectedResourcesThisTurn && rc.isMovementReady()){
        if (rc.isMovementReady()){
            Nav.goTo(dest);
        }
    }

    private static void movementWrapper() throws GameActionException{
        if (rc.isMovementReady()){
            // exploreDest1 = ifExploreDestNullStuff(Explore.explore(randomExploration));
            // if (exploreDest1 == null) return;
            exploreDest1 = Explore.explore(randomExploration);
            pathing.setAndMoveToDestination(exploreDest1);
        }
        if (rc.isMovementReady()){
            // exploreDest2 = ifExploreDestNullStuff(Explore.explore(randomExploration));
            // if (exploreDest2 == null) return;
            exploreDest2 = Explore.explore(randomExploration);
            Nav.goTo(exploreDest2);
        }
    }

    private static void movementWrapperForCircularExplore() throws GameActionException{
        if (CircularExplore.getCenterLocation() == null){
            movementWrapper(true);
            return;
        }
        if (rc.isMovementReady()){
            // exploreDest1 = ifExploreDestNullStuff(CircularExplore.explore());
            // if (exploreDest1 == null) return;
            exploreDest1 = CircularExplore.explore();
            pathing.setAndMoveToDestination(exploreDest1);
        }
        if (rc.isMovementReady()){
            // exploreDest2 = ifExploreDestNullStuff(CircularExplore.explore());
            // if (exploreDest2 == null) return;
            exploreDest2 = CircularExplore.explore();
            Nav.goTo(exploreDest2);
        }
    }

    private static void movementWrapper(boolean fleeing) throws GameActionException{
        if (rc.isMovementReady()){
            // exploreDest1 = ifExploreDestNullStuff(Explore.explore());
            // if (exploreDest1 == null) return;
            exploreDest1 = Explore.explore(); 
            pathing.setAndMoveToDestination(exploreDest1);
        }
            
        if (rc.isMovementReady()){
            // exploreDest2 = ifExploreDestNullStuff(Explore.explore());
            // if (exploreDest2 == null) return;
            exploreDest2 = Explore.explore();
            Nav.goTo(exploreDest2);
        }
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

    private static boolean isMilitaryUnit(RobotInfo robotInfo) throws GameActionException{
        switch(robotInfo.type){
            case LAUNCHER: 
            case DESTABILIZER: return true;
            case HEADQUARTERS: return (rc.getAnchor() == null && robotInfo.getLocation().distanceSquaredTo(rc.getLocation()) <= RobotType.HEADQUARTERS.actionRadiusSquared);
            default: return false;
        }
    }

    private static boolean canSeeMilitaryUnit() throws GameActionException{
        for (int i = visibleEnemies.length; --i >= 0;)
            if (isMilitaryUnit(visibleEnemies[i])) return true;
        return false;
    }

    private static int vicinityMilitaryCount(RobotInfo[] rInfos) throws GameActionException{
        int count = 0;
        for (int i = rInfos.length; --i >= 0;)
            if (isMilitaryUnit(rInfos[i])) count++;
        return count;
    }

    private static void fleeUpdate() throws GameActionException{
        if (!TRY_TO_FLEE) return;
        if (!isFleeing && canSeeMilitaryUnit()){
            RobotInfo[] alliedInfos = rc.senseNearbyRobots(UNIT_TYPE.visionRadiusSquared, MY_TEAM);
            int allyCount = vicinityMilitaryCount(alliedInfos);
            int enemyCount = vicinityMilitaryCount(visibleEnemies);
            // if (allyCount <= 3 || allyCount < enemyCount + Math.max(2,(allyCount/3))){ 
            if (rc.getAnchor() != null || allyCount < enemyCount){ 
                isFleeing = true;
                fleeCount = FLEE_ROUNDS;
                // if (tryToFlee(visibleEnemies)) tryToFlee(visibleEnemies);
                return;
            }
        }
        else if (isFleeing && vicinityMilitaryCount(visibleEnemies) == 0)
            fleeCount = Math.max(0, fleeCount - 1);
        
        if (isFleeing && fleeCount == 0){
            isFleeing = false;
            lastRetreatDirection = null;
            if (returnToHQ) movementDestination = Comms.findNearestHeadquarter();
            else if (movingToIsland){
                MapLocation islandLoc = getMeAnIslandLocation();
                if (islandLoc == null){
                    carrierStatus = Status.EXPLORE_FOR_ISLANDS;
                    movementWrapper();
                    return;
                }
                setIslandDestination(islandLoc);
                // moveToIslandAndPlaceAnchor();
            }
            else movementDestination = null;
        }
    }

    private static void updateOverall() throws GameActionException{
        carrierStatus = Status.NORMAL;
        if (returnEarly){
            carrierStatus = Status.TOO_MUCH_BYTECODES_RET_EARLY;
            return;
        }
        returnEarly = false;
        currentLocation = rc.getLocation();
        otherTypeWell = null;
        updateVision();
        exploreDest1 = null;
        exploreDest2 = null;
        movesLeftBeforeDeath = (rc.getHealth() / RobotType.LAUNCHER.damage);
        if (rc.getWeight() <= 2) movesLeftBeforeDeath *= 2;
        // Comms.writeSavedLocations();
        fleeUpdate();
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
        // int count = Comms.readMessageWithoutSHAFlag(collectAnchorHQidx);
        int count = Comms.getAnchorCount(collectAnchorHQidx);
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
        if (rc.getLocation().distanceSquaredTo(movementDestination) > 2) return;
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
            getAndSetWellLocation();
            // goToWell();
            if (movementDestination != null){
                carrierStatus = Status.TRANSIT_TO_WELL;
                movementWrapper(movementDestination);
            }
        }
    }

    private static void exploreForWells(boolean skip) throws GameActionException{
        ResourceType rType = getLocalPrioritizedResource();
        otherTypeWell = null;
        if (!skip && exploreRoundCount == 0) movementDestination = findNearestWellInVision(rType);
        carrierStatus = Status.EXPLORE_FOR_WELLS;
        if (movementDestination != null){
            exploringForWells = false;
            carrierStatus = Status.TRANSIT_TO_WELL;
            setWellDestination(movementDestination);
            Comms.writeOrSaveLocation(movementDestination, Comms.resourceFlag(rType));
        }
        else{
            if (USING_CIRCULAR_EXPLORATION){
                if (otherTypeWell == null) movementWrapperForCircularExplore();
                else{
                    if (!otherTypeWell.equals(CircularExplore.getCenterLocation())){
                        CircularExplore.updateCenterLocation(otherTypeWell);
                        addLocationToExplored(otherTypeWell);
                    }
                    movementWrapperForCircularExplore();
                }
            }
            else movementWrapper();
            exploreRoundCount = Math.max(0, exploreRoundCount - 1);
        }
    }

    private static boolean adamantiumDepositionForElixirStuff() throws GameActionException{
        if (!ElixirProducer.shouldProduceElixir() || ElixirProducer.checkIfElixirWellMade()) return false;
        if (rc.getResourceAmount(ResourceType.ADAMANTIUM) != amountToCollect()) return false;
        movementDestination = ElixirProducer.getManaWellToConvert();
        if (movementDestination == null) return false;
        carrierStatus = Status.TRANSIT_TO_MANA_WELL_FOR_ELIXIR;
        ElixirProducer.goingToElixirWell = true;
        return true;
    }

    private static void setReturnToHQTrue() throws GameActionException{
        if (adamantiumDepositionForElixirStuff()) return;
        returnToHQ = true;
        movementDestination = Comms.findNearestHeadquarter();
    }

    private static void setReturnToHQTrue(MapLocation hqLoc){
        returnToHQ = true;
        movementDestination = hqLoc;
    }

    private static void doIReturnToHQ() throws GameActionException{
        currentInventoryWeight = rc.getWeight();
        if (!returnToHQ && currentInventoryWeight >= amountToCollect())
            setReturnToHQTrue();
        else if (DOING_EARLY_MANA_DEPOSITION && !returnToHQ && isFleeing){
            MapLocation hqLoc = Comms.findNearestHeadquarter();
            if (currentInventoryWeight >= EARLY_MANA_DEPOSTION_THRESHOLD){
                setReturnToHQTrue(hqLoc);
                isFleeing = false;
                fleeCount = 0;
            }
        }
        if (returnToHQ && movementDestination == null)
            movementDestination = Comms.findNearestHeadquarter();
        if (returnToHQ && !goingToCollectAnchor){
            carrierStatus = Status.TRANSIT_RES_DEP;
            movementDestination = Comms.findNearestHeadquarter();
        }
    }

    private static void updateCarrier() throws GameActionException{
        if (exploringForWells){
            exploreForWells(false);
            return;
        }
        doIReturnToHQ();
        collectedElixir = rc.getResourceAmount(ResourceType.ELIXIR);
        collectedMana = rc.getResourceAmount(ResourceType.MANA);
        collectedAdamantium = rc.getResourceAmount(ResourceType.ADAMANTIUM);
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
        MapLocation nearestLoc = null, loc;
        int nearestDist = -1, curDist;
        int islandId;
        for (int i = nearbyIslands.length; i-- > 0; ){
            islandId = nearbyIslands[i];
            if (rc.senseTeamOccupyingIsland(islandId) != Team.NEUTRAL){
                islandViable[islandId - 1] = false;
                continue;
            }
            if (!islandViable[islandId - 1])
                islandViable[islandId - 1] = true;
            MapLocation[] locations = rc.senseNearbyIslandLocations(islandId);
            for (int j = locations.length; j-- > 0; ){
                loc = locations[j];
                curDist = rc.getLocation().distanceSquaredTo(loc);
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
        for (int i = Comms.COMM_TYPE.NEUTRAL_ISLANDS.channelStart; i < Comms.COMM_TYPE.NEUTRAL_ISLANDS.channelStop; i++){
            int message = rc.readSharedArray(i);
            if (Comms.readSHAFlagFromMessage(message) != Comms.SHAFlag.UNOCCUPIED_ISLAND) continue;
            MapLocation loc = Comms.readLocationFromMessage(message);
            if (ignoreLocations[hashLocation(loc)]) continue;
            curDist = rc.getLocation().distanceSquaredTo(loc);
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
        if (senseLoc != null && rc.canWriteSharedArray(0, 0) 
        // && !Comms.findIfLocationAlreadyPresent(senseLoc, Comms.COMM_TYPE.ISLAND, Comms.SHAFlag.UNOCCUPIED_ISLAND)
        )
            Comms.writeAndOverwriteLesserPriorityMessage(Comms.COMM_TYPE.NEUTRAL_ISLANDS, senseLoc, Comms.SHAFlag.UNOCCUPIED_ISLAND);
        if (senseLoc != null && commsLoc != null){
            if (rc.getLocation().distanceSquaredTo(commsLoc) <= rc.getLocation().distanceSquaredTo(senseLoc))
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
            Comms.wipeThisLocationFromChannels(Comms.COMM_TYPE.NEUTRAL_ISLANDS, Comms.SHAFlag.UNOCCUPIED_ISLAND, loc);
        else{
            int hash = hashLocation(loc);
            if (!ignoreLocations[hash]) ignoredIslandLocations[ignoredIslandLocationsCount++] = hash;
            
            assert ignoredIslandLocationsCount <= MAX_IGNORED_ISLAND_COUNT : "ignoredIslandLocationsCount <= MAX_IGNORED_ISLAND_COUNT";
            ignoreLocations[hashLocation(loc)] = true;
        }
    }

    private static boolean viableIslandCheck(boolean canSenseDest, MapLocation loc, int islandID) throws GameActionException{
        assert canSenseDest : "canSenseDest == true";
        return rc.senseTeamOccupyingIsland(islandID) == Team.NEUTRAL;
    }

    private static void moveToIslandAndPlaceAnchor() throws GameActionException{
        assert movementDestination != null : "movementDestination != null";
        // assert targetedIslandId != -1 : "targetedIslandId != -1";
        assertNotHeadquarterLocation(movementDestination);
        int isCurLocIsland = rc.senseIsland(rc.getLocation());
        if (isCurLocIsland != -1 && rc.senseTeamOccupyingIsland(isCurLocIsland) == Team.NEUTRAL){
            if (rc.canPlaceAnchor()){
                // rc.setIndicatorString("A small dump for carrier, a huge jump for AnchorIslands! Placed Anchor!");
                carrierStatus = Status.PLACING_ANCHOR;
                System.out.println("Placed Anchor!");
                rc.placeAnchor();
                Comms.writeOrSaveLocation(rc.getLocation(), Comms.SHAFlag.OUR_ISLAND);
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
        Comms.wipeOrSaveLocation(movementDestination, Comms.SHAFlag.UNOCCUPIED_ISLAND);
        resetIslandVariables();
        // TODO: Might wanna add find and move command here later on.
    }

    private static void carrierAnchorMode() throws GameActionException{
        // I have an anchor. So singularly focusing on getting it to the first island I can find.
        // TODO: Add conditions and actions for behavior under attack.
        if (returnEarly) {
            carrierStatus = Status.TOO_MUCH_BYTECODES_RET_EARLY;
            return;
        }
        if (isFleeing && shouldIFlee()) return;
        if (rc.canPlaceAnchor() && rc.senseAnchor(rc.senseIsland(rc.getLocation())) == null){
            carrierStatus = Status.PLACING_ANCHOR;
            rc.placeAnchor();
            Comms.writeOrSaveLocation(rc.getLocation(), Comms.SHAFlag.OUR_ISLAND);
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

    private static int amountToCollect(){
        if (rc.getRoundNum() > INCREASE_RESOURCE_COLLECTION_ROUND) return GameConstants.CARRIER_CAPACITY;
        switch(prioritizedResource){
            case ADAMANTIUM: return Math.min(RobotType.CARRIER.buildCostAdamantium / 2 + EXCESS_RESOURCES, GameConstants.CARRIER_CAPACITY);
            case MANA: return Math.min(RobotType.LAUNCHER.buildCostMana / 2 + EXCESS_RESOURCES, GameConstants.CARRIER_CAPACITY);
            case ELIXIR: return Math.min(RobotType.DESTABILIZER.buildCostElixir / 2 + EXCESS_RESOURCES, GameConstants.CARRIER_CAPACITY);
            default : break;
        }
        assert false;
        return 0;
    }

    private static void exitCollectionOfResources(MapLocation chosenWell) throws GameActionException{
        if (returnToHQ || isFleeing || chosenWell == null) return;
        Movement.legionMining(chosenWell);
    }

    private static void collectResources() throws GameActionException{
        if (!rc.isActionReady() || returnToHQ) return;
        WellInfo[] adjacentWells = rc.senseNearbyWells(2);
        if (adjacentWells.length == 0) 
            return;
        MapLocation chosenWell = null;
        // First mine for priority resource.
        for (int i = adjacentWells.length; i-- > 0;){
            if (!rc.isActionReady() || returnToHQ){
                exitCollectionOfResources(chosenWell);
                return;
            }
            // if (adjacentWells[i].getResourceType() == ResourceType.ADAMANTIUM && getLocalPrioritizedResource() == ResourceType.MANA)
            //     continue;
            ResourceType rType = adjacentWells[i].getResourceType();
            if (rType != ResourceType.ELIXIR && rType != getLocalPrioritizedResource()) continue;
            WellInfo curWell = adjacentWells[i];
            Comms.writeOrSaveLocation(curWell.getMapLocation(), Comms.resourceFlag(curWell.getResourceType()));    
            collectionWrapper(curWell);
            chosenWell = curWell.getMapLocation();
        }
        if (returnToHQ) return;
        // Then for the other resource
        for (int i = adjacentWells.length; i-- > 0;){
            if (!rc.isActionReady() || returnToHQ){
                exitCollectionOfResources(chosenWell);
                return;
            }
            if (adjacentWells[i].getResourceType() == ResourceType.ADAMANTIUM && getLocalPrioritizedResource() == ResourceType.MANA) 
                continue;
            WellInfo curWell = adjacentWells[i];  
            collectionWrapper(curWell);
            if (chosenWell == null) chosenWell = curWell.getMapLocation();
        }

        if (!returnToHQ && chosenWell != null){
            Movement.legionMining(chosenWell);
        }
    }

    private static void collectionWrapper(WellInfo curWell) throws GameActionException{
        int amount = Math.min(curWell.getRate(), amountToCollect() - rc.getWeight());  
        if (amount <= 0){
            setReturnToHQTrue();
            carrierStatus = Status.TRANSIT_RES_DEP;
            movementWrapper(movementDestination);
            return;
        }      
        if (rc.canCollectResource(curWell.getMapLocation(), amount)){
            rc.collectResource(curWell.getMapLocation(), amount);
            persistance = 0;
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
            if (currentInventoryWeight >= amountToCollect()){
                setReturnToHQTrue();
                return;
            }
        }
    }

    private static void attackIfAboutToDie() throws GameActionException{
        // if (returnEarly) return;
        if (rc.getWeight() < 5) // Can't even attack...
            return;
        else if (returnToHQ && movementDestination != null && rc.getLocation().distanceSquaredTo(movementDestination) <= 2)
            return;
        else if (visibleEnemies.length == 0 || !rc.isActionReady())
            return;
        else if (rc.getHealth() > UNIT_TYPE.health/10) return;
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
        WellInfo well;
        // for (WellInfo well : nearbyWells){
        for (int i = nearbyWells.length; --i >= 0;){
            well = nearbyWells[i];
            MapLocation loc = well.getMapLocation();
            curDist = rc.getLocation().distanceSquaredTo(loc);
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
        if (INITIAL_MINE_ONLY_MANA_STRAT && rc.getRoundNum() <= MINE_ONLY_MANA_TILL_ROUND)
            resourceType = ResourceType.MANA;
        WellInfo[] nearbyWells = rc.senseNearbyWells();
        MapLocation nearestLoc = null;
        int nearestDist = -1, curDist;
        otherTypeWell = null;
        int otherTypeDist = -1;
        WellInfo well;
        for (int i = nearbyWells.length; --i >= 0;){
            well = nearbyWells[i];
            MapLocation loc = well.getMapLocation();
            if (well.getResourceType() == ResourceType.ELIXIR) return loc;
            if (well.getResourceType() != resourceType){
                curDist = rc.getLocation().distanceSquaredTo(loc);
                if (otherTypeWell == null || curDist < otherTypeDist){
                    otherTypeWell = loc;
                    otherTypeDist = curDist;
                }
                continue;
            }
            curDist = rc.getLocation().distanceSquaredTo(loc);
            if (nearestLoc == null || curDist < nearestDist){
                nearestLoc = loc;
                nearestDist = curDist;
            }
        }
        return nearestLoc;
    }

    private static MapLocation findNearestWellInVisionOfOnlyThisType(ResourceType rType) throws GameActionException{
        WellInfo[] nearbyWells = rc.senseNearbyWells(rType);
        if (nearbyWells.length == 0) return null;
        MapLocation nearestLoc = null;
        int nearestDist = -1, curDist;
        WellInfo well;
        for (int i = nearbyWells.length; --i >= 0;){
            well = nearbyWells[i];
            MapLocation loc = well.getMapLocation();
            curDist = rc.getLocation().distanceSquaredTo(loc);
            if (nearestLoc == null || curDist < nearestDist){
                nearestLoc = loc;
                nearestDist = curDist;
            }
        }
        return nearestLoc;
    }

    public static ResourceType getLocalPrioritizedResource(){
        if (INITIAL_MINE_ONLY_MANA_STRAT && rc.getRoundNum() <= MINE_ONLY_MANA_TILL_ROUND)
            return ResourceType.MANA;
        return prioritizedResource;
    }

    private static boolean isLocationExplored(MapLocation loc){
        switch(exploredLocationsCount){
            case 10:
                if (loc.equals(exploredLocations[9])) return true;
            case 9:
                if (loc.equals(exploredLocations[8])) return true;
            case 8:
                if (loc.equals(exploredLocations[7])) return true;
            case 7:
                if (loc.equals(exploredLocations[6])) return true;
            case 6:
                if (loc.equals(exploredLocations[5])) return true;
            case 5:
                if (loc.equals(exploredLocations[4])) return true;
            case 4:
                if (loc.equals(exploredLocations[3])) return true;
            case 3:
                if (loc.equals(exploredLocations[2])) return true;
            case 2:
                if (loc.equals(exploredLocations[1])) return true;
            case 1:
                if (loc.equals(exploredLocations[0])) return true;
            case 0:
                return false;
            default: break;
        }
        assert false;
        return false;
    }

    private static void addLocationToExplored(MapLocation loc){
        if (!STORING_EXPLORED_LOCATIONS) return;
        if (exploredLocationsCount == EXPLORED_LOCATIONS_MAX_SIZE) return;
        exploredLocations[exploredLocationsCount++] = loc;
    }

    private static boolean toExploreOrNotToExplore(MapLocation obtainedLoc){
        if (otherTypeWell == null) return  true;
        if (STORING_EXPLORED_LOCATIONS && isLocationExplored(obtainedLoc) && !Comms.checkIfLocationSaved(obtainedLoc)) return false;
        double wellDist = Math.sqrt(rc.getLocation().distanceSquaredTo(otherTypeWell));
        double potentialDist = Math.min(Math.sqrt(GameConstants.MAX_DISTANCE_BETWEEN_WELLS), Math.min(MAP_HEIGHT, MAP_WIDTH)/4);
        double expectedDist = wellDist + potentialDist;
        return rc.getLocation().distanceSquaredTo(obtainedLoc) < expectedDist * expectedDist;
    }

    private static boolean possibilityOfElixirWell() throws GameActionException{
        MapLocation senseLoc = findNearestWellInVisionOfOnlyThisType(ResourceType.ELIXIR);
        if (senseLoc == null && !ElixirProducer.checkIfElixirWellMade()) return false;
        else if (senseLoc == null && ElixirProducer.rollTheDice()){
            senseLoc = Comms.getElixirWellTarget();
            assert senseLoc != null;
        }
        else if (senseLoc == null) return false;
        setWellDestination(senseLoc);
        Comms.writeOrSaveLocation(senseLoc, Comms.SHAFlag.ELIXIR_WELL_LOCATION);
        return true;
    }

    /**
     * Find a Well location from which resources are to be collected. First try to find in comms. If that location is null or not in vision, try to find out if there's a location in vision that is better
     * @throws GameActionException
     * @BytecodeCost : ~ 350
     */
    private static void getAndSetWellLocation() throws GameActionException{
        otherTypeWell = null;
        if (possibilityOfElixirWell()) return;
        ResourceType rType = getLocalPrioritizedResource();
        Comms.SHAFlag flag = Comms.resourceFlag(rType);
        MapLocation senseLoc = findNearestWellInVision(rType);
        carrierStatus = Status.SEARCHING_FOR_WELL;
        if (senseLoc != null && rc.canWriteSharedArray(0, 0) 
        // && !Comms.findIfLocationAlreadyPresent(senseLoc, Comms.COMM_TYPE.WELLS, flag)
        ){
            Comms.writeAndOverwriteLesserPriorityMessage(Comms.COMM_TYPE.WELLS, senseLoc, flag);
            setWellDestination(senseLoc);
            return;
        }
        MapLocation commsLoc = Comms.findNearestLocationOfThisType(rc.getLocation(), Comms.COMM_TYPE.WELLS, flag);
        if (commsLoc != null && rc.canSenseLocation(commsLoc)){
            setWellDestination(commsLoc);
            return;
        }
        else if (commsLoc != null && toExploreOrNotToExplore(commsLoc)) setWellDestination(commsLoc);
        else if (otherTypeWell != null) {
            if (CircularExplore.getCenterLocation() == null || !otherTypeWell.equals(CircularExplore.getCenterLocation())){
                addLocationToExplored(otherTypeWell);
                CircularExplore.updateCenterLocation(otherTypeWell);
            }
            carrierStatus = Status.EXPLORE_FOR_WELLS;
            exploringForWells = true;
            movementDestination = null;
        }
        else{
            CircularExplore.startExploration();
            carrierStatus = Status.EXPLORE_FOR_WELLS;
            exploringForWells = true;
            movementDestination = null;
        }
    }

    private static boolean morePreciseOvercrowdingCheck(MapLocation wellLoc) throws GameActionException{
        int count = rc.senseNearbyRobots(wellLoc, 2, MY_TEAM).length;
        MapInfo[] mapInfos = rc.senseNearbyMapInfos(wellLoc, 2);
        int blockedCount = 0;
        for (int i = mapInfos.length; --i >= 0;)
            if (!mapInfos[i].isPassable()) blockedCount++;
        if (count + blockedCount >= 8) {
            persistance++;
            return persistance > MAX_PERSISTANCE;
        }
        return false;
    }

    private static boolean doOvercrowdingCheck(MapLocation wellLoc) throws GameActionException{
        int curDist = rc.getLocation().distanceSquaredTo(wellLoc);
        if (curDist <= UNIT_TYPE.visionRadiusSquared && curDist > 8){
            int count = rc.senseNearbyRobots(UNIT_TYPE.visionRadiusSquared, MY_TEAM).length;
            if (count > GEFFNERS_EXPLORE_TRIGGER){
                exploringForWells = true;
                desperationIndex = 0;
                movementDestination = null;
                carrierStatus = Status.EXPLORE_FOR_WELLS;
                movementWrapper(true);
                CircularExplore.resetCenterLocation();
                exploreRoundCount = MIN_EXPLORE_ROUND_COUNT;
                return true;
            }
        }
        else if (curDist <= UNIT_TYPE.visionRadiusSquared && morePreciseOvercrowdingCheck(movementDestination)){
            persistance++;
            if (persistance < MAX_PERSISTANCE) return false;
            exploringForWells = true;
            desperationIndex = 0;
            movementDestination = null;
            carrierStatus = Status.EXPLORE_FOR_WELLS;
            movementWrapper(true);
            CircularExplore.resetCenterLocation();
            exploreRoundCount = MIN_EXPLORE_ROUND_COUNT;
            return true;
        }
        return false;
    }

    private static void goToWell() throws GameActionException{
        if (!rc.isMovementReady()) return;
        if (movementDestination == null){
            assert carrierStatus == Status.EXPLORE_FOR_WELLS : "Movement destination is null but status is not explore for wells";
            exploringForWells = true;
            exploreForWells(true);
            return;
        }
        int curDist = rc.getLocation().distanceSquaredTo(movementDestination);
        if (curDist <= 2) { // Reached location
            if (!rc.isLocationOccupied(movementDestination)){
                Direction dir = rc.getLocation().directionTo(movementDestination);
                if (rc.canMove(dir))
                    rc.move(dir);
            }
            collectResources();
            return;
        }

        if (doOvercrowdingCheck(movementDestination)) return;
        else if (curDist > UNIT_TYPE.actionRadiusSquared){
            // If outside of action radius
            otherTypeWell = null;
            MapLocation senseLoc = null;
            senseLoc = findNearestWellInVision(getLocalPrioritizedResource());
            if (senseLoc == null && otherTypeWell == null){
                rc.setIndicatorString("moving to well: " + movementDestination + "; despId: " + desperationIndex);
                carrierStatus = Status.TRANSIT_TO_WELL;
            }
            else if (senseLoc == null && !toExploreOrNotToExplore(movementDestination) && Clock.getBytecodesLeft() > 1500){
                exploringForWells = true;
                desperationIndex = 0;
                movementDestination = null;
                carrierStatus = Status.EXPLORE_FOR_WELLS;
                if (!otherTypeWell.equals(CircularExplore.getCenterLocation())){
                    CircularExplore.updateCenterLocation(otherTypeWell);
                    addLocationToExplored(otherTypeWell);
                }
                movementWrapperForCircularExplore();
                return;
            }
            else if (senseLoc != null){
                Comms.writeOrSaveLocation(senseLoc, Comms.resourceFlag(getLocalPrioritizedResource()));
                setWellDestination(senseLoc);
            }
            carrierStatus = Status.TRANSIT_TO_WELL;
            movementWrapper(movementDestination);
            return;
        }
        // pathing.setAndMoveToDestination(movementDestination);
        movementWrapper(movementDestination);
        carrierStatus = Status.TRANSIT_TO_WELL;
        if (desperationIndex < 5) return;
        if (rc.canWriteSharedArray(0, 0))
            Comms.wipeThisLocationFromChannels(Comms.COMM_TYPE.WELLS, Comms.resourceFlag(prioritizedResource), movementDestination);
        // desperationIndex = 12;
        exploringForWells = true;
        desperationIndex = 0;
        movementDestination = null;
        carrierStatus = Status.DESPERATE;
    }

    private static void goToManaWellForElixir() throws GameActionException{
        assert movementDestination != null;
        currentLocation = rc.getLocation();
        if (currentLocation.distanceSquaredTo(movementDestination) > 2){
            carrierStatus = Status.TRANSIT_TO_MANA_WELL_FOR_ELIXIR;
            movementWrapper(movementDestination);
            return;
        }
        WellInfo well = rc.senseWell(movementDestination);
        assert well != null : "where did the well go?";
        assert well.getResourceType() == ResourceType.MANA : "I was targeting mana wells right?";
        int depositedAmount = well.getResource(ResourceType.ADAMANTIUM);
        int amountToTransfer = Math.min(depositedAmount, ElixirProducer.ADAMANTIUM_DEPOSITION_FOR_ELIXIR_WELL_CREATION);
        if (rc.canTransferResource(movementDestination, ResourceType.ADAMANTIUM, amountToTransfer)){
            rc.transferResource(movementDestination, ResourceType.ADAMANTIUM, amountToTransfer);
            depositedAmount += amountToTransfer;
            if (depositedAmount == GameConstants.UPGRADE_TO_ELIXIR)
                ElixirProducer.setElixirWellMade();
            collectResources();
            Comms.writeOrSaveLocation(movementDestination, Comms.SHAFlag.ELIXIR_WELL_LOCATION);
            ElixirProducer.goingToElixirWell = false;
        }
    }

    private static void gatherResources() throws GameActionException{
        // TODO: Add conditions and actions for behavior under attack here too.
        if (returnEarly) {
            carrierStatus = Status.TOO_MUCH_BYTECODES_RET_EARLY;
            return;
        }
        if (movingToIsland) resetIslandVariables();
        updateCarrier();
        if (isFleeing && shouldIFlee()){
            collectResources();
            return;
        }
        if (rc.getAnchor() != null){
            carrierAnchorMode();
            return;
        }
        if (exploringForWells) return;
        if (ElixirProducer.goingToElixirWell){
            goToManaWellForElixir();
            return;
        }
        if (returnToHQ){
            assert movementDestination != null : "movementDestination != null in gather resources";
            if (goingToCollectAnchor)
                carrierStatus = Status.TRANSIT_ANCHOR_COLLECTION;
            else carrierStatus = Status.TRANSIT_RES_DEP;
            movementWrapper(movementDestination);
            collectedElixir = rc.getResourceAmount(ResourceType.ELIXIR);
            collectedMana = rc.getResourceAmount(ResourceType.MANA);
            collectedAdamantium = rc.getResourceAmount(ResourceType.ADAMANTIUM);
            transferResourcesToHQ();
            return;
        }
        collectResources();
        if (collectedResourcesThisTurn) {
            if (rc.getWeight() < amountToCollect()) return;
            setReturnToHQTrue();
            carrierStatus = Status.TRANSIT_RES_DEP;
            movementWrapper(movementDestination);
            return;
        }
        if (movementDestination != null){
            goToWell();
            collectResources();
        }
        else{
            getAndSetWellLocation();
            if (Clock.getBytecodesLeft() < 2000) {
                movementWrapper(movementDestination);
                return;
            }
            goToWell();
            collectResources();
        }
        if (rc.getWeight() < amountToCollect() && !collectedResourcesThisTurn && rc.isMovementReady())
            desperationIndex++;
        else desperationIndex = 0;
    }

    private static void endOfTurnUpdate() throws GameActionException{
        returnEarly = false;
        if (carrierStatus == Status.TRANSIT_TO_WELL || carrierStatus == Status.TRANSIT_TO_ISLAND)
            rc.setIndicatorString(carrierStatus.toString() + " " + movementDestination);
        else if (carrierStatus == Status.EXPLORE_FOR_WELLS || carrierStatus == Status.EXPLORE_FOR_ISLANDS){
            if (CircularExplore.DEBUG_PRINT && CircularExplore.DEBUG_ID == rc.getID())
            CircularExplore.printStatus(exploreDest1, exploreDest2);
            else rc.setIndicatorString(carrierStatus.toString() + " " + exploreDest1 + " " + exploreDest2);
        }
        else
            rc.setIndicatorString(carrierStatus.toString() + " " + movementDestination);
        if (Clock.getBytecodesLeft() > 700){
            RobotInfo[] visibleEnemies = rc.senseNearbyRobots(-1, ENEMY_TEAM);
            CombatUtils.sendGenericCombatLocation(visibleEnemies);
        }
        if (Clock.getBytecodesLeft() > 700)
            Comms.surveyForIslands();
        Comms.writeSavedLocations();
        Comms.wipeSavedLocations();
    }

    private static Direction getRetreatDirection(RobotInfo[] visibleHostiles) throws GameActionException{
        int closestHostileDistSq = Integer.MAX_VALUE;
        MapLocation lCR = rc.getLocation();
        RobotInfo hostile;
        // for (RobotInfo hostile : visibleHostiles) {
        for (int i = visibleHostiles.length; --i >= 0;) {
            hostile = visibleHostiles[i];
            if (!hostile.type.canAttack() && hostile.type != RobotType.HEADQUARTERS) continue;
            int distSq = lCR.distanceSquaredTo(hostile.location);
            if (distSq < closestHostileDistSq) {
                closestHostileDistSq = distSq;
            }
        }
        Direction bestRetreatDir = null, dir;
        int bestDistSq = closestHostileDistSq;

        for (int i = directions.length; --i >= 0;) {
            dir = directions[i];
            if (!rc.canMove(dir)) continue;
            MapLocation dirLoc = lCR.add(dir);
            // int dirLocRubble = rc.senseRubble(dirLoc);
            // if (dirLocRubble > bestRubble) continue; // Don't move to even more rubble

            int smallestDistSq = Integer.MAX_VALUE;
            for (int j = visibleHostiles.length; --j >= 0;) {
                hostile = visibleHostiles[j];
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

    private static boolean shouldIFlee() throws GameActionException{
        if (!returnToHQ && !movingToIsland) return true;
        // assert movementDestination != null: "rn: " + rc.getRoundNum() + "; curLoc: " + rc.getLocation() + "; id: " + rc.getID() + "";
        if (movementDestination == null) return true;
        int dist = rc.getLocation().distanceSquaredTo(movementDestination);
        return dist > (movesLeftBeforeDeath*movesLeftBeforeDeath);
    }

    private static boolean tryToFlee(RobotInfo[] visibleHostiles) throws GameActionException {
        
        Direction bestRetreatDir = getRetreatDirection(visibleHostiles);
        if (bestRetreatDir != null) {
            // rc.setIndicatorString("Backing: " + bestRetreatDir);
            lastRetreatDirection = bestRetreatDir;
            if (!shouldIFlee()) return false;
            carrierStatus = Status.FLEEING;
            Explore.lastCallRound = rc.getRoundNum();
            rc.move(bestRetreatDir);
            return true;
        }
        return false;
    }

    private static void fleeIfNeedTo() throws GameActionException{
        if (!TRY_TO_FLEE || !isFleeing) return;
        if (!rc.isMovementReady() || !shouldIFlee()) // Can't do anything...
            return;
        carrierStatus = Status.FLEEING;
        Direction dir = getRetreatDirection(visibleEnemies);
        // if (dir != null) Explore.assignExplore3Dir(dir);
        Explore.lastCallRound = rc.getRoundNum();
        if (dir != null)
            fleeTarget = extrapolateLocation(rc.getLocation(), dir, FLEE_EXTRAPOLATE_UNSQUARED_DISTANCE);
        else if (lastRetreatDirection != null)
            fleeTarget = extrapolateLocation(rc.getLocation(), lastRetreatDirection, FLEE_EXTRAPOLATE_UNSQUARED_DISTANCE);
        else{
            movementWrapper();
            return;
        }
        movementWrapper(fleeTarget);
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
