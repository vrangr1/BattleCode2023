package ABuildABot;

import battlecode.common.*;

public class BotAmplifier extends Explore{

    private enum Status{
        BORN,
        SPREADING,
        BATTLE_COMM,
        FLEEING,
        ANCHOR_FOLLOWER,
    }

    private static Status amplifierState = Status.BORN;
    private static RobotInfo[] visibleEnemies;
    private static RobotInfo[] visibleAllies;
    private static int commAllyRobots = 0;
    private static int vNonHQEnemies = 0;
    private static int vNonHQCombatEnemies = 0;
    private static int vNonHQCombatAllies = 0;  
    private static MapLocation closestEnemyLocation;

    public static void initAmplifier() throws GameActionException{
        updateVision();
    }

    public static void runAmplifier() throws GameActionException{
        closestEnemyLocation = null;
        updateVision();
        sendCombatLocation(visibleEnemies);
        if (rc.isMovementReady() && vNonHQCombatEnemies > vNonHQCombatAllies){
            tryToBackUpToMaintainMaxRangeAmplifier();
            if (amplifierState == Status.FLEEING){
            }
        }
        else if (rc.getRoundNum() < 25){
            pathing.setAndMoveToDestination(CENTER_OF_THE_MAP);
        } 
        else if (vNonHQEnemies == 0 && commAllyRobots > 0){
            Direction away = directionAwayFromAmplifierAndHQ(visibleAllies);
            if (away!=null && Movement.tryMoveInDirection(explore(away))){
                amplifierState = Status.SPREADING;
            }
        }
        updateVision();
        if (Clock.getBytecodesLeft() > 700) findAndWriteWellLocationsToComms();
        rc.setIndicatorString(amplifierState.toString() + " " + closestEnemyLocation);
    }

    public static boolean tryToBackUpToMaintainMaxRangeAmplifier() throws GameActionException {
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
			rc.move(bestRetreatDir);
            currentLocation = rc.getLocation();
			explore(bestRetreatDir);
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

    private static void getExploreDir(Direction away) throws GameActionException{
        if (away != null){
            assignExplore3Dir(away);
            return;
        }
        assignExplore3Dir(directions[Globals.rng.nextInt(8)]);
    }

    private static MapLocation explore(Direction away) throws GameActionException{
        if (exploreDir != away)
            getExploreDir(away);
        return getExplore3Target();
    }

    private static void updateVision() throws GameActionException {
        commAllyRobots = 0;
        visibleEnemies = rc.senseNearbyRobots(UNIT_TYPE.visionRadiusSquared, ENEMY_TEAM);
        visibleAllies = rc.senseNearbyRobots(UNIT_TYPE.visionRadiusSquared, MY_TEAM);
        vNonHQEnemies = 0;
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

    private static boolean sendCombatLocation(RobotInfo[] visibleHostiles) throws GameActionException{
        if (vNonHQEnemies > 0){
			RobotInfo closestHostile = CombatUtils.getClosestUnitWithCombatPriority(visibleHostiles);
            if (closestHostile != null){
                closestEnemyLocation = closestHostile.getLocation();
                Comms.writeAndOverwriteLesserPriorityMessage(Comms.COMM_TYPE.COMBAT, closestHostile.getLocation(), Comms.SHAFlag.COMBAT_LOCATION);
                amplifierState = Status.BATTLE_COMM;
                return true;
            }
        }
        return false;
    }

}
