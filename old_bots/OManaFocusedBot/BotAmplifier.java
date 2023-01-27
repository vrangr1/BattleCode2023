package OManaFocusedBot;

import battlecode.common.*;
import OManaFocusedBot.Comms.COMM_TYPE;
import OManaFocusedBot.path.Nav;

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
    private static RobotInfo shepherdUnit;

    public static void initAmplifier() throws GameActionException{
        amplifierState = Status.BORN;
        shepherdUnit = null;
    }

    public static void runAmplifier() throws GameActionException{
        if (TRACKING_AMPLIFIER_COUNT) Comms.incrementRobotCount(RobotType.AMPLIFIER);
        updateVision();
        findAndWriteWellLocationsToComms();
        Comms.surveyForIslandsAmplifiers();
        // commsCleaner();
        CombatUtils.sendGenericCombatLocation(visibleEnemies);
        if (rc.isMovementReady() && vNonHQCombatEnemies > vNonHQCombatAllies){
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
        rc.setIndicatorString(amplifierState.toString() + " " + currentDestination);
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

    private static MapLocation checkShepherdUnitLocation () throws GameActionException{
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
                    if (curDistance > bestDistance){
                        count = 1;
                        bestDistance = curDistance;
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
		int closestHostileDistSq = Integer.MAX_VALUE;
        MapLocation lCR = rc.getLocation();
        for (int i= visibleEnemies.length; --i >= 0;) {
            RobotInfo hostile = visibleEnemies[i];
			if (!CombatUtils.isMilitaryUnit(hostile)) continue;
            if (rc.getLocation().distanceSquaredTo(hostile.location) > hostile.type.visionRadiusSquared) continue;
			int distSq = lCR.distanceSquaredTo(hostile.location);
			if (distSq < closestHostileDistSq) {
				closestHostileDistSq = distSq;
			}
		}

        if (closestHostileDistSq == Integer.MAX_VALUE) return false;
		
		Direction bestRetreatDir = null;
		int bestDistSq = closestHostileDistSq;

		for (Direction dir : directions) {
			if (!rc.canMove(dir)) continue;
			MapLocation dirLoc = lCR.add(dir);

			int smallestDistSq = Integer.MAX_VALUE;
			for (int i = visibleEnemies.length; --i >= 0;) {
                RobotInfo hostile = visibleEnemies[i];
				if (!CombatUtils.isMilitaryUnit(hostile)) continue;
                if (rc.getLocation().distanceSquaredTo(hostile.location) > hostile.type.visionRadiusSquared) continue;
				int distSq = hostile.location.distanceSquaredTo(dirLoc);
				if (distSq < smallestDistSq) {
					smallestDistSq = distSq;
				}
			}
			if (smallestDistSq >= bestDistSq) {
				bestDistSq = smallestDistSq;
				bestRetreatDir = dir;
			}
		}
		if (bestRetreatDir != null) {
            Nav.goTo(rc.getLocation().add(bestRetreatDir));
            amplifierState = Status.FLEEING;
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
