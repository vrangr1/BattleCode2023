package APostQualsBot;

import battlecode.common.*;
import APostQualsBot.path.Nav;

public class BotAmplifier extends Explore{

    private enum Status{
        BORN,
        EXPLORING,
        BATTLE_COMM,
        FLEEING,
        BATTLE_FOLLOWER,
        ANCHOR_FOLLOWER,
    }

    private static Status amplifierState;
    private static RobotInfo[] visibleEnemies;
    private static RobotInfo[] visibleAllies;
    private static RobotInfo enemyHQ = null;
    private static int commAllyRobots = 0;
    private static int vNonHQEnemies = 0;
    private static int vNonHQCombatEnemies = 0;
    private static int vNonHQCombatAllies = 0;  
    public static int enemyHQInVision = 0;
    private static MapLocation currentDestination;

    public static void initAmplifier() throws GameActionException{
        amplifierState = Status.BORN;
        shepherdUnit = null;
    }

    public static void runAmplifier() throws GameActionException{
        if (TRACKING_AMPLIFIER_COUNT) Comms.incrementRobotCount(RobotType.AMPLIFIER);
        updateVision();
        findAndWriteWellLocationsToComms();
        Comms.surveyForIslandsAmplifiers();
        combatCommsCleaner(vNonHQEnemies);
        CombatUtils.sendGenericCombatLocation(visibleEnemies, enemyHQ);
        if (rc.isMovementReady()){
            tryToBackUpToMaintainMaxRangeAmplifier();
        }
        // circleEnemyHQ();
        followCombatUnit();
        if (shepherdUnit == null){
            if (rc.getRoundNum() < 25){
                pathing.setAndMoveToDestination(CENTER_OF_THE_MAP);
                currentDestination = CENTER_OF_THE_MAP;
            } 
            else if (vNonHQCombatEnemies == 0){
                amplifierMove();
            }
        }
        if (amplifierState == Status.BATTLE_FOLLOWER){
            rc.setIndicatorString(amplifierState.toString() + " " + shepherdUnit.location);
        }
        rc.setIndicatorString(amplifierState.toString() + "|Dest" + currentDestination + "|ACount" + Comms.getRobotCount(RobotType.AMPLIFIER));
    }

    private static void followCombatUnit() throws GameActionException{
        MapLocation destination = checkShepherdUnitLocation();
        if (destination != null){
            if (rc.isMovementReady() && rc.getLocation().distanceSquaredTo(destination) > 2){
                currentDestination = destination;
                if (willGoInEnemyHQRange(currentDestination)) return;
                Nav.goTo(currentDestination);
                amplifierState = Status.BATTLE_FOLLOWER;
            }
        }
    }

    private static MapLocation checkShepherdUnitLocation() throws GameActionException{
        if (shepherdUnit != null){
            for (int i = visibleAllies.length; --i >= 0;) {
                if (visibleAllies[i].ID == shepherdUnit.ID){
                    return visibleAllies[i].location;
                }
            }
        }
        setShepherdUnit();
        if (shepherdUnit != null){
            return shepherdUnit.location;
        }
        return null;
    }

    private static void setShepherdUnit() throws GameActionException{
        shepherdUnit = null;
        RobotInfo nonCombatShepherdUnit = null;
        int bestDistance = 0;
        int count = 1;
        if (shepherdUnit == null && vNonHQCombatAllies - commAllyRobots >= 0){
            for (int i = visibleAllies.length; --i >= 0;) {
                if (CombatUtils.isMilitaryUnit(visibleAllies[i].type)){
                    int curDistance = rc.getLocation().distanceSquaredTo(visibleAllies[i].location);
                    if (curDistance == bestDistance) count++;
                    else if (curDistance > bestDistance){
                        count = 1;
                        bestDistance = curDistance;
                    }
                    else{
                        continue;
                    }
                    if (rng.nextInt(count) == 0){
                        shepherdUnit = visibleAllies[i];
                    }
                }
                else if (visibleAllies[i].type == RobotType.CARRIER){
                    nonCombatShepherdUnit = visibleAllies[i];
                }
            }
        }
        if (shepherdUnit == null)
            amplifierState = Status.EXPLORING;
        return;
    }

    private static void amplifierMove() throws GameActionException{
        if (rc.isMovementReady()){
            currentDestination = explore(true);
            if (willGoInEnemyHQRange(currentDestination)) return;
            Nav.goTo(currentDestination);
            amplifierState = Status.EXPLORING;
        }
    }

    private static boolean tryToBackUpToMaintainMaxRangeAmplifier() throws GameActionException {
        if (vNonHQCombatEnemies == 0) return false;
		int closestHostileDistSq = Integer.MAX_VALUE;
        for (int i= visibleEnemies.length; --i >= 0;) {
            RobotInfo hostile = visibleEnemies[i];
			if (!CombatUtils.isMilitaryUnit(hostile)) continue;
			int distSq = rc.getLocation().distanceSquaredTo(hostile.location);
			if (distSq < closestHostileDistSq) {
				closestHostileDistSq = distSq;
			}
		}
        rc.setIndicatorString("P Ret|" + closestHostileDistSq);

        if (closestHostileDistSq == Integer.MAX_VALUE) return false;
		
        Direction bestRetreatDir = null;
		double leastAttack = Double.MAX_VALUE;
        // double bestRubble = rc.senseMapInfo(rc.getLocation()).getCooldownMultiplier(ENEMY_TEAM) * 10.0;
		for (Direction dir : carDirections) {
			if (!rc.canMove(dir)) continue;
			MapLocation dirLoc = rc.getLocation().add(dir);
            // MapInfo dirLocMapInfo = rc.senseMapInfo(dirLoc);
            double unitsAttacking = 0;
            if (rc.senseCloud(dirLoc)){
                unitsAttacking+=2;
            }
            for (int i = visibleEnemies.length; --i >= 0;){
                if (CombatUtils.isMilitaryUnit(visibleEnemies[i].type)){
                    int distToDirLoc = visibleEnemies[i].location.distanceSquaredTo(dirLoc);
                    if (distToDirLoc <= visibleEnemies[i].type.actionRadiusSquared){
                        unitsAttacking+=1;
                    }
                    else if (distToDirLoc <= visibleEnemies[i].type.visionRadiusSquared){
                        unitsAttacking+=0.5;
                    }
                    unitsAttacking++;
                }
            }
            if (unitsAttacking < leastAttack){
                leastAttack = unitsAttacking;
                bestRetreatDir = dir;
            }
		}
		if (bestRetreatDir != null) {
			rc.move(bestRetreatDir);
            rc.setIndicatorString("Ret" + bestRetreatDir + "lA " + leastAttack + "Move " + rc.isMovementReady());
			return true;
		}
		return false;
	}

    private static void updateVision() throws GameActionException {
        visibleEnemies = rc.senseNearbyRobots(UNIT_TYPE.visionRadiusSquared, ENEMY_TEAM);
        visibleAllies = rc.senseNearbyRobots(UNIT_TYPE.visionRadiusSquared, MY_TEAM);
        commAllyRobots = 0;
        vNonHQEnemies = 0;
        vNonHQCombatEnemies = 0;
        vNonHQCombatAllies= 0;
        enemyHQInVision = 0;
        enemyHQ = null;
        for (int i = visibleEnemies.length; --i >= 0;) {
            if (visibleEnemies[i].type != RobotType.HEADQUARTERS) {
                vNonHQEnemies++;
                if (CombatUtils.isMilitaryUnit(visibleEnemies[i])){
                    vNonHQCombatEnemies++;
                }
            }
            else {
                enemyHQInVision++;
                enemyHQ = visibleEnemies[i];
                if (rc.canWriteSharedArray(0, 0)){
                    Comms.writeEnemyHeadquarterLocation(enemyHQ.location);
                }
            }
        }
        for (int i = visibleAllies.length; --i >= 0;) {
            if (visibleAllies[i].type == RobotType.AMPLIFIER) {
                commAllyRobots++;
            }
            else if (CombatUtils.isMilitaryUnit(visibleAllies[i].type)){
                vNonHQCombatAllies++;
            }
        }
    }

    private static boolean willGoInEnemyHQRange(MapLocation destination) throws GameActionException{
        if (enemyHQInVision > 0){
            if (destination.distanceSquaredTo(enemyHQ.location) <= enemyHQ.type.actionRadiusSquared * 1.5){
                return true;
            }
        }
        return false;
    }
}
