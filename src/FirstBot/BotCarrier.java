package FirstBot;

import battlecode.common.*;

// TODO: Remove the following. Potentially bytecode heavy operations.
import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;

public class BotCarrier extends Explore{

    public static boolean isMinedThisTurn;
    public static int numOfMiners;
    private static final int maxCapacity = GameConstants.CARRIER_CAPACITY;
    private static MapLocation miningLocation;
    private static boolean inPlaceForMining;
    public static boolean commitSuicide;
    private static MapLocation suicideLocation;
    public static int desperationIndex;
    private static final int MIN_SUICIDE_DIST = 4;
    public static boolean prolificMiningLocationsAtBirth;
    private static boolean moveOut;
    private static RobotInfo[] visibleEnemies;
    private static boolean isFleeing;
    private static final boolean searchByDistance = false;
    private static final int randomPersistance = 20;
    private static boolean tooCrowded;
    private static final int CROWD_LIMIT = 5;
    private static final int LOW_HEALTH_STRAT_TRIGGER = (int)((MAX_HEALTH*3.0d)/10.0d);
    private static final int LOW_HEALTH_STRAT_RELEASER = (int)((MAX_HEALTH*8.0d)/10.0d);
    public static boolean lowHealthStratInPlay = false;
    public static MapLocation lowHealthStratArchon;
    public static int fleeCount;
    private static MapLocation finalDestination = null; 
    private static boolean DEBUG_MODE = false;

    private static WellInfo[] nearbyWells = null;

    public static void initCarrier() throws GameActionException{
        nearbyWells = rc.senseNearbyWells();
        resetVariables();
        lowHealthStratInPlay = false;
        lowHealthStratArchon = null;
        fleeCount = 0;
    }

    public static boolean areMiningLocationsAbundant(){
        try{
            return (nearbyWells.length > 30);
        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

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
            assignExplore3Dir(closestHQ.directionTo(rc.getLocation()));
        else
            assignExplore3Dir(directions[Globals.rng.nextInt(8)]);
    }

    public static MapLocation explore() throws GameActionException{
        if (exploreDir == CENTER)
            getExploreDir();
        return getExplore3Target();
    }

    public static void updateCarrier() throws GameActionException{
        if (rc.getAnchor() != null) {
            carrierAnchorMode();
            return;
        }
        isMinedThisTurn = false;
        moveOut = true;
        tooCrowded = false;
        updateVision();
        if (CombatUtils.militaryCount(visibleEnemies) == 0)
            fleeCount = Math.max(0, fleeCount - 1);
        if (fleeCount == 0) isFleeing = false;
        else isFleeing = true;
        checkIfEnemyHQInVision();
        if (!isSafeToMine(rc.getLocation())){
            isFleeing = true;
            miningLocation = null;
            inPlaceForMining = false;
            isFleeing = CombatUtils.tryToBackUpToMaintainMaxRangeMiner(visibleEnemies);
            fleeCount = 5;
        }
    }

    private static void carrierAnchorMode() throws GameActionException{
        // If I have an anchor singularly focus on getting it to the first island I see
        int[] islands = rc.senseNearbyIslands();
        Set<MapLocation> islandLocs = new HashSet<>();
        for (int id : islands) {
            MapLocation[] thisIslandLocs = rc.senseNearbyIslandLocations(id);
            islandLocs.addAll(Arrays.asList(thisIslandLocs));
        }
        if (islandLocs.size() > 0) {
            MapLocation islandLocation = islandLocs.iterator().next();
            rc.setIndicatorString("Moving my anchor towards " + islandLocation);
            while (!rc.getLocation().equals(islandLocation)) {
                Direction dir = rc.getLocation().directionTo(islandLocation);
                if (rc.canMove(dir)) {
                    rc.move(dir);
                }
            }
            if (rc.canPlaceAnchor()) {
                rc.setIndicatorString("Huzzah, placed anchor!");
                rc.placeAnchor();
            }
        }
    }

    public static void updateVision() throws GameActionException {
        visibleEnemies = rc.senseNearbyRobots(UNIT_TYPE.visionRadiusSquared, ENEMY_TEAM);
    }

    // TODO: Shift to CombatUtils
    private static boolean checkIfEnemyHQInVision() throws GameActionException{
        for (int i = visibleEnemies.length; i-->0;){
            RobotInfo bot = visibleEnemies[i];
            if (bot.type == RobotType.HEADQUARTERS){
                Comms.writeAndOverwriteLesserPriorityMessage(Comms.COMM_TYPE.COMBAT, bot.getLocation(), Comms.SHAFlag.ENEMY_HEADQUARTER_LOCATION);
                return true;
            }
        }
        return false;
    }

    private static void resetVariables(){
        miningLocation = null;
        inPlaceForMining = false;
        commitSuicide = false;
        suicideLocation = null;
        desperationIndex = 0;
        moveOut = true; 
        isFleeing = false;
    }

    // TODO: Change this so that every carrier gets some of the resource?
    private static void mine() throws GameActionException{
        if (!rc.isActionReady()) return;
        WellInfo[] adjacentLocations = rc.senseNearbyWells(rc.getLocation(), UNIT_TYPE.actionRadiusSquared);
        for (int i = adjacentLocations.length; i-->0;){
            WellInfo wellLoc = adjacentLocations[i];
            MapLocation loc = wellLoc.getMapLocation();
            if (!rc.isActionReady()) return;
            ResourceType wellResourceType = wellLoc.getResourceType();
            int amountInWell = wellLoc.getResource(wellResourceType);
            while(rc.canCollectResource(loc, Math.min(maxCapacity-1-rc.getResourceAmount(wellResourceType), amountInWell))){
                isMinedThisTurn = true;
                rc.collectResource(loc, Math.min(maxCapacity, amountInWell));
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

    // public static MapLocation findOpenMiningLocationNearby() throws GameActionException{
    //     if (prolificMiningLocationsAtBirth){
    //         return Movement.moveToLattice(2, 0);
    //     }
    //     if (rc.senseWell(rc.getLocation()) != null){
    //         return rc.getLocation();
    //     }
    //     if (countOfCarriersInVicinity() > CROWD_LIMIT){ 
    //         tooCrowded = true;
    //         return null;
    //     }
    //     MapLocation[] potentialMiningLocations = rc.senseNearbyLocationsWithGold();
    //     if (potentialMiningLocations.length > 0) return findOptimalLocationForMiningGold(potentialMiningLocations);
    //     if (!depleteMine) 
    //         potentialMiningLocations = rc.senseNearbyLocationsWithLead(UNIT_TYPE.visionRadiusSquared, 8);
    //     else potentialMiningLocations = rc.senseNearbyLocationsWithLead();
    //     if (potentialMiningLocations.length > 0) return findOptimalLocationForMiningLead(potentialMiningLocations);
    //     return null;
    // }

    public static void runCarrier(){

    }
}
