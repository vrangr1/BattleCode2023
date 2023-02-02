package ARefinedElixirBot;

import battlecode.common.*;

public class BotBooster extends Utils{

    public static RobotInfo[] visibleEnemies;
    public static RobotInfo[] inRangeEnemies;
    public static RobotInfo[] visibleAllies;
    public static MapLocation[] cloudLocations;
    public static int vNonHQEnemies = 0;
    public static int inRNonHQEnemies = 0;
    public static int enemyHQInVision = 0;
    public static int vNonHQCombatAllies = 0;
    public static RobotInfo enemyHQ = null;

    public static void initBooster() throws GameActionException{

    }

    public static void runBooster() throws GameActionException{
        updateVision();
        findWell();
        if (currentDestination != null && rc.getLocation().distanceSquaredTo(currentDestination) > 16){
            pathing.setAndMoveToDestination(currentDestination);
        }
        if (rc.canBoost()){
            rc.boost();
        }
    }

    public static void findWell() throws GameActionException{
        if (currentDestination == null){
            currentDestination = findNearestWellInVision(ResourceType.ELIXIR);
        }
        if (currentDestination == null){
            currentDestination = findNearestWellInVision(ResourceType.MANA);
        }
        if (currentDestination == null){
            currentDestination = Comms.findNearestLocationOfThisType(rc.getLocation(), Comms.COMM_TYPE.WELLS, Comms.SHAFlag.MANA_WELL_LOCATION);
        }
        if (currentDestination == null){
            BotLauncher.setBaseDestination();
        }
    }

    public static MapLocation findNearestWellInVision(ResourceType resourceType) throws GameActionException{
        WellInfo[] nearbyWells = rc.senseNearbyWells();
        MapLocation nearestLoc = null;
        int nearestDist = -1, curDist;
        WellInfo well;
        for (int i = nearbyWells.length; --i >= 0;){
            well = nearbyWells[i];
            MapLocation loc = well.getMapLocation();
            // if (well.getResourceType() == ResourceType.ELIXIR && ElixirProducer.rollTheDice(ElixirProducer.ELIXIR_TO_MANA_RATIO)) return loc;
            if (well.getResourceType() != resourceType){
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

    public static void updateVision() throws GameActionException {
        updateVisibleEnemiesVision();
        if (visibleEnemies.length > 0)
            updateInRangeEnemiesVision();
        else{
            inRangeEnemies = null;
            inRNonHQEnemies = 0;
        }
    }

    public static void updateVisibleEnemiesVision() throws GameActionException{
        visibleEnemies = rc.senseNearbyRobots(UNIT_TYPE.visionRadiusSquared, ENEMY_TEAM);
        vNonHQEnemies = 0;
        enemyHQInVision = 0;
        enemyHQ = null;
        for (int i = visibleEnemies.length; --i >= 0;) {
            if (visibleEnemies[i].type != RobotType.HEADQUARTERS) {
                vNonHQEnemies++;
            }
            else{
                enemyHQInVision++;
                enemyHQ = visibleEnemies[i];
            }
        }
    }

    public static void updateInRangeEnemiesVision() throws GameActionException{
        inRangeEnemies = rc.senseNearbyRobots(UNIT_TYPE.actionRadiusSquared, ENEMY_TEAM);
        inRNonHQEnemies = 0;
        for (int i = inRangeEnemies.length; --i >= 0;) {
            if (inRangeEnemies[i].type != RobotType.HEADQUARTERS) {
                inRNonHQEnemies++;
            }
        }
    }
}
