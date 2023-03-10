package OCrowdBot;

import battlecode.common.*;
import OCrowdBot.Comms.COMM_TYPE;
import OCrowdBot.path.Nav;

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
    private static RobotInfo shepherdUnit;

    public static void initAmplifier() throws GameActionException{
        amplifierState = Status.BORN;
        shepherdUnit = null;
    }

    public static void runAmplifier() throws GameActionException{
        if (TRACKING_AMPLIFIER_COUNT) Comms.incrementRobotCount(RobotType.AMPLIFIER);
        updateVision();
        findAndWriteWellLocationsToComms();
        // commsCleaner();
        CombatUtils.sendGenericCombatLocation(visibleEnemies);
        if (rc.isMovementReady() && vNonHQCombatEnemies > vNonHQCombatAllies){
            tryToBackUpToMaintainMaxRangeAmplifier();
        }
        followCombatUnit();
        if (rc.getRoundNum() < 25){
            pathing.setAndMoveToDestination(CENTER_OF_THE_MAP);
            currentDestination = CENTER_OF_THE_MAP;
        } 
        if (vNonHQCombatEnemies == 0){
            amplifierMove();
        }
        rc.setIndicatorString(amplifierState.toString() + " " + currentDestination);
    }

    private static void commsCleaner() throws GameActionException{
        MapLocation combatLoc = Comms.findNearestLocationOfThisType(rc.getLocation(), Comms.COMM_TYPE.COMBAT, Comms.SHAFlag.COMBAT_LOCATION);
        if (combatLoc == null)  return;
        if (vNonHQEnemies > 0)  return;
        if (rc.getLocation().distanceSquaredTo(combatLoc) * 1.25 < RobotType.AMPLIFIER.visionRadiusSquared){
            Comms.wipeThisLocationFromChannels(Comms.COMM_TYPE.COMBAT, Comms.SHAFlag.COMBAT_LOCATION, combatLoc);
        }
    }

    private static void followCombatUnit() throws GameActionException{
        MapLocation destination = checkShepherdUnitLocation();
        if (destination != null){
            if (rc.isMovementReady()){
                currentDestination = destination;
                Nav.goTo(destination);
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
        shepherdUnit = null;
        setShepherdUnit();
        return null;
    }

    private static void setShepherdUnit() throws GameActionException{
        RobotInfo nonCombatShepherdUnit = null;
        if (shepherdUnit == null && visibleAllies.length - commAllyRobots > 0){
            for (int i = visibleAllies.length; --i >= 0;) {
                if (visibleAllies[i].type == RobotType.DESTABILIZER || visibleAllies[i].type == RobotType.LAUNCHER){
                    shepherdUnit = visibleAllies[i];
                    amplifierState = Status.BATTLE_FOLLOWER;
                    break;
                }
                else if (visibleAllies[i].type == RobotType.CARRIER){
                    nonCombatShepherdUnit = visibleAllies[i];
                }
            }
        }
    }

    private static void amplifierMove() throws GameActionException{
        if(rc.isMovementReady()){
            currentDestination = explore(true);
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

			int smallestDistSq = Integer.MAX_VALUE;
			for (int i = visibleEnemies.length; --i >= 0;) {
                RobotInfo hostile = visibleEnemies[i];
				if (hostile.type != RobotType.LAUNCHER) continue;
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
            else if (rc.canWriteSharedArray(0, 0)){
                Comms.writeEnemyHeadquarterLocation(visibleEnemies[i].location);
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
