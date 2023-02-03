package ORefinedElixirBot;

import battlecode.common.*;
import ORefinedElixirBot.path.Nav;

public class BotDestabilizer extends BotLauncher{

    private static int[] actionEdges_x =  new int[] {-3, -3, -3, -3, -3, -2, -2, -1, -1, 0, 0, 1, 1, 2, 2, 3, 3, 3, 3, 3};
    private static int[] actionEdges_y =  new int[] {-2, -1, 0, 1, 2, -3, 3, -3, 3, -3, 3, -3, 3, -3, 3, -2, -1, 0, 1, 2};

    public static void initDestabilizer() throws GameActionException{
        initLauncher();
    }

    public static void runDestabilizer() throws GameActionException{
        updateVision();
        destinationFlag = "";
        bytecodeCheck();
        if (vNonHQEnemies == 0){
            manageHealingState();
            if (inHealingState){
                tryToHealAtIsland();
            }
            else{
                closerCombatDestination(); // [CUR_STATE] -> [CUR_STATE|MARCHING|EXPLORE]
            }
        }
        bytecodeCheck();
        tryToMicro();
        if (!rc.isActionReady() || !rc.isMovementReady()) // No need to update vision if you haven't moved or attacked
            updateVision();

        if (sendCombatLocation());
        else {
            findNewCombatLocation();
        }
        moveAfterNonMovingCombat();
        if (rc.isActionReady()) {
            updateInRangeEnemiesVision();
            bytecodeCheck(); //6
            chooseTargetAndAttack(visibleEnemies);
        }
        if (rc.isActionReady()) {
            updateInRangeEnemiesVision();
            bytecodeCheck(); //6
            chooseTargetAndAttack(visibleEnemies);
        }
        rc.setIndicatorString(launcherState + "|Dest " + currentDestination + " " + destinationFlag + "|Move " + rc.isMovementReady());
    }

    public static boolean tryToMicro() throws GameActionException {
        if (vNonHQEnemies == 0) {
            return false;
        }

        if (rc.isMovementReady() && retreatIfOutnumbered()){
            destinationFlag += "-ret";
            return true;
        }

        if (rc.isActionReady()){
            if (vNonHQEnemies > 0) {
                chooseTargetAndAttack(inRangeEnemies);
            }
        }
        if (rc.isMovementReady()){
            RobotInfo closestHostile = getClosestUnitWithCombatPriority(visibleEnemies);
            // Most important function
            if (vNonHQEnemies > 0 && tryToBackUpToMaintainMaxRangeLauncher(visibleEnemies)) {
                destinationFlag += "max1";
                launcherState = Status.FLANKING;
                return true;
            }
            tryMoveToEngageOutnumberedEnemy(closestHostile);
            if (!inHealingState && tryMoveToAttackProductionUnit(closestHostile)) {
                destinationFlag += " prod2";
                return true;
            }
        }
        return false;
    }

    public static boolean findNewCombatLocation() throws GameActionException{
        destinationFlag+= "|dfnc0";
        if (vNonHQEnemies == 0){
            if (currentDestination != null){
                if (launcherState == Status.ISLAND_WORK || launcherState == Status.HEALING) return false;
                if (!rc.canSenseLocation(currentDestination)) return false;
                int dist = rc.getLocation().distanceSquaredTo(currentDestination);
                if (MAP_SIZE <= 900 && dist > UNIT_TYPE.actionRadiusSquared) return false;
                Comms.wipeThisLocationFromChannels(Comms.COMM_TYPE.COMBAT, Comms.SHAFlag.COMBAT_LOCATION, currentDestination);
            }
            MapLocation combatLocation = Comms.findNearestLocationOfThisTypeOutOfVision(rc.getLocation(), Comms.COMM_TYPE.COMBAT, Comms.SHAFlag.COMBAT_LOCATION);
            if (combatLocation != null){
                currentDestination = combatLocation;
                destinationFlag += "|dfnc1";
                pathing.setNewDestination(currentDestination);
                launcherState = Status.MARCHING;
                return true;
            }
            else if (currentDestination == null || (currentDestination.distanceSquaredTo(rc.getLocation()) <= UNIT_TYPE.visionRadiusSquared 
                && enemyHQ == null && rc.canSenseLocation(currentDestination))){
                destinationFlag += "|dfnc2";
                setBaseDestination();
                return true;
            }
        }
        return false;
    }

    public static void moveAfterNonMovingCombat() throws GameActionException {
        destinationFlag+= "|dmove0";
        if (vNonHQEnemies != 0) return;
        if (rc.isMovementReady()) {
            destinationFlag+= "|dmove0.5";
            if (currentDestination != null) {
                pathing.setNewDestination(currentDestination);
            }
            if (launcherState == Status.MARCHING || launcherState == Status.ISLAND_WORK) {
                pathing.moveToDestination();
                if (rc.isMovementReady()){
                    destinationFlag += "| dmove1";
                    Nav.goTo(currentDestination);
                }
                else{
                    destinationFlag += "| dmove2";
                }
            }
        }
    }

    public static void chooseTargetAndAttack(RobotInfo[] targets) throws GameActionException {
		MapLocation bestTargetLoc = null;
		double bestValue = -1;
		for (int i = actionEdges_x.length; --i >= 0;) {
            double value = -1;
            MapLocation targetLoc = rc.getLocation().translate(actionEdges_x[i], actionEdges_y[i]);
            if (rc.canDestabilize(targetLoc)) {
                for (RobotInfo target : targets) {
                    if (target.location.distanceSquaredTo(targetLoc) <= GameConstants.DESTABILIZER_RADIUS_SQUARED) {
                        value += target.health;
                    }
                }
                if (currentDestination != null && currentDestination.distanceSquaredTo(targetLoc) < 
                    rc.getLocation().distanceSquaredTo(targetLoc)){
                    value += 3;    
                }
                if (currentDestination!=null && rc.getLocation().distanceSquaredTo(currentDestination) > 
                    targetLoc.distanceSquaredTo(currentDestination))
                    value += 5;
                if (value > bestValue) {
                    bestValue = value;
                    bestTargetLoc = targetLoc;
                }
            }
        }
		if (bestTargetLoc != null) {
            rc.destabilize(bestTargetLoc);
		}
	}

}
