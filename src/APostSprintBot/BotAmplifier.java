package APostSprintBot;

import battlecode.common.*;
import APostSprintBot.path.Nav;

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
    private static int commAllyRobots = 0;
    private static int vNonHQEnemies = 0;
    private static int vNonHQCombatEnemies = 0;
    private static int vNonHQCombatAllies = 0;  
    private static MapLocation currentDestination;

    public static void initAmplifier() throws GameActionException{
        amplifierState = Status.BORN;
    }

    public static void runAmplifier() throws GameActionException{
        if (TRACKING_AMPLIFIER_COUNT) Comms.incrementRobotCount(RobotType.AMPLIFIER);
        updateVision();
        findAndWriteWellLocationsToComms();
        CombatUtils.sendGenericCombatLocation(visibleEnemies);
        if (rc.isMovementReady() && vNonHQCombatEnemies > vNonHQCombatAllies){
            tryToBackUpToMaintainMaxRangeAmplifier();
            amplifierState = Status.FLEEING;
        }
        else if (rc.getRoundNum() < 25){
            pathing.setAndMoveToDestination(CENTER_OF_THE_MAP);
            currentDestination = CENTER_OF_THE_MAP;
        } 
        else if (vNonHQEnemies == 0){
            amplifierMove();
        }
        rc.setIndicatorString(amplifierState.toString() + " " + currentDestination);
    }

    private static void amplifierMove() throws GameActionException{
        if(rc.isMovementReady()){
            if (rc.getRoundNum()%25 == 0){
                Direction away = directionAwayFromAmplifierAndHQ(visibleAllies);
                if (away != null){
                    assignExplore3Dir(away);
                }
            }
            currentDestination = explore();
            Nav.goTo(currentDestination);
            amplifierState = Status.EXPLORING;
        }
    }

    private static boolean tryToBackUpToMaintainMaxRangeAmplifier() throws GameActionException {
		int closestHostileDistSq = Integer.MAX_VALUE;
        MapLocation lCR = rc.getLocation();
        for (int i= visibleEnemies.length; --i >= 0;) {
            RobotInfo hostile = visibleEnemies[i];
			if (hostile.type != RobotType.LAUNCHER) continue;
			int distSq = lCR.distanceSquaredTo(hostile.location);
			if (distSq < closestHostileDistSq) {
				closestHostileDistSq = distSq;
			}
		}
		
		Direction bestRetreatDir = null;
		int bestDistSq = closestHostileDistSq;

		for (Direction dir : directions) {
			if (!rc.canMove(dir)) continue;
			MapLocation dirLoc = lCR.add(dir);
            boolean isDirPassable = rc.sensePassability(dirLoc);
            if (!isDirPassable) continue; // Don't try to move to impassable location

			int smallestDistSq = Integer.MAX_VALUE;
			for (int i= visibleEnemies.length; --i >= 0;) {
                RobotInfo hostile = visibleEnemies[i];
				if (hostile.type != RobotType.LAUNCHER) continue;
				int distSq = hostile.location.distanceSquaredTo(dirLoc);
				if (distSq < smallestDistSq) {
					smallestDistSq = distSq;
				}
			}
			if (smallestDistSq > bestDistSq) {
				bestDistSq = smallestDistSq;
				bestRetreatDir = dir;
			}
		}
		if (bestRetreatDir != null) {
			assignExplore3Dir(bestRetreatDir);
            currentDestination = explore();
            Nav.goTo(currentDestination);
            amplifierState = Status.FLEEING;
			return true;
		}
		return false;
	}

    private static Direction directionAwayFromAmplifierAndHQ(RobotInfo[] givenRobots){
        MapLocation currentTarget = rc.getLocation();
        for (int i = givenRobots.length; --i >= 0;) {
            if (givenRobots[i].type == RobotType.HEADQUARTERS || givenRobots[i].type == RobotType.AMPLIFIER){
                RobotInfo aRobot = givenRobots[i];			
                currentTarget = currentTarget.add(aRobot.location.directionTo(rc.getLocation()));
            }
        }
        return rc.getLocation().directionTo(currentTarget);
    }

    private static void updateVision() throws GameActionException {
        visibleEnemies = rc.senseNearbyRobots(UNIT_TYPE.visionRadiusSquared, ENEMY_TEAM);
        visibleAllies = rc.senseNearbyRobots(UNIT_TYPE.visionRadiusSquared, MY_TEAM);
        commAllyRobots = 0;
        vNonHQEnemies = 0;
        vNonHQCombatEnemies = 0;
        vNonHQCombatAllies= 0;
        for (int i = visibleEnemies.length; --i >= 0;) {
            if (visibleEnemies[i].type != RobotType.HEADQUARTERS) {
                vNonHQEnemies++;
                if (visibleEnemies[i].type == RobotType.LAUNCHER){
                    vNonHQCombatEnemies++;
                }
            }
        }
        for (int i = visibleAllies.length; --i >= 0;) {
            if (visibleAllies[i].type == RobotType.HEADQUARTERS || visibleAllies[i].type == RobotType.AMPLIFIER) {
                commAllyRobots++;
            }
            else if (visibleAllies[i].type == RobotType.LAUNCHER){
                vNonHQCombatAllies++;
            }
        }
    }

}
