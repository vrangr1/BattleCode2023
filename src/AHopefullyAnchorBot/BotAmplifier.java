package AHopefullyAnchorBot;

import battlecode.common.*;

public class BotAmplifier extends Explore{
    private static RobotInfo[] visibleEnemies;
    private static RobotInfo[] visibleAllies;
    private static int commAllyRobots = 0;
    private static int vNonHQEnemies = 0;

    public static void initAmplifier() throws GameActionException{
        updateVision();
        if (vNonHQEnemies == 0 && commAllyRobots > 0){
            Direction away = directionAwayFromAmplifierAndHQ(visibleAllies);
            if (away!=null)
                Movement.tryMoveInDirection(explore(away));
        }
    }

    public static void runAmplifier() throws GameActionException{
        updateVision();
        sendCombatLocation(visibleEnemies);
        if (vNonHQEnemies == 0 && commAllyRobots > 0){
            Direction away = directionAwayFromAmplifierAndHQ(visibleAllies);
            if (away!=null)
                Movement.tryMoveInDirection(explore(away));
        }
        if (Clock.getBytecodesLeft() > 700) findAndWriteWellLocationsToComms();
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
        // inRangeEnemies = rc.senseNearbyRobots(UNIT_TYPE.actionRadiusSquared, ENEMY_TEAM);
        visibleAllies = rc.senseNearbyRobots(UNIT_TYPE.visionRadiusSquared, MY_TEAM);
        // inRangeAllies = rc.senseNearbyRobots(UNIT_TYPE.actionRadiusSquared, MY_TEAM);
        vNonHQEnemies = 0;
        for (int i = visibleEnemies.length; --i >= 0;) {
            if (visibleEnemies[i].type != RobotType.HEADQUARTERS) {
                vNonHQEnemies++;
            }
        }
        for (int i = visibleAllies.length; --i >= 0;) {
            if (visibleAllies[i].type == RobotType.HEADQUARTERS || visibleAllies[i].type == RobotType.AMPLIFIER) {
                commAllyRobots++;
            }
        }
    }

    private static boolean sendCombatLocation(RobotInfo[] visibleHostiles) throws GameActionException{
        if (vNonHQEnemies > 0){
			RobotInfo closestHostile = CombatUtils.getClosestUnitWithCombatPriority(visibleHostiles);
            if (closestHostile != null)
				Comms.writeAndOverwriteLesserPriorityMessage(Comms.COMM_TYPE.COMBAT, closestHostile.getLocation(), Comms.SHAFlag.COMBAT_LOCATION);
            return true;
        }
        return false;
    }

}
