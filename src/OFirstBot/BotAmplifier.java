package OFirstBot;

import battlecode.common.*;

public class BotAmplifier extends Utils{
    private static RobotInfo[] visibleEnemies;
    private static RobotInfo[] visibleAllies;
    private static RobotInfo[] inRangeEnemies;
    private static RobotInfo[] inRangeAllies;

    public static void initAmplifier() throws GameActionException{
        updateVision();
        if (visibleEnemies.length < visibleAllies.length){
            moveOutOfHQCommsRange();
        }
    }

    public static void runAmplifier() throws GameActionException{
        updateVision();
        if (visibleEnemies.length < visibleAllies.length){
            MapLocation awayLocation = locationAwayFromAmplifierAndHQ(visibleAllies);
            Movement.goToDirect(awayLocation);
        }
    }

    public static MapLocation locationAwayFromAmplifierAndHQ(RobotInfo[] givenRobots){
        MapLocation currentTarget = rc.getLocation();
        for (int i = givenRobots.length; --i >= 0;) {
            if (givenRobots[i].type == RobotType.HEADQUARTERS || givenRobots[i].type == RobotType.AMPLIFIER){
                RobotInfo aRobot = givenRobots[i];			
                currentTarget = currentTarget.add(aRobot.location.directionTo(rc.getLocation()));
            }
        }
        return currentTarget;
    }

    private static void moveOutOfHQCommsRange() throws GameActionException{
        MapLocation nearestHQ = Comms.findNearestHeadquarter();
        if (nearestHQ.distanceSquaredTo(rc.getLocation()) > 9){
            Movement.moveAwayFromLocation(nearestHQ);
        }
    }

    private static void updateVision() throws GameActionException {
        visibleEnemies = rc.senseNearbyRobots(UNIT_TYPE.visionRadiusSquared, ENEMY_TEAM);
        inRangeEnemies = rc.senseNearbyRobots(UNIT_TYPE.actionRadiusSquared, ENEMY_TEAM);
        visibleAllies = rc.senseNearbyRobots(UNIT_TYPE.visionRadiusSquared, MY_TEAM);
        inRangeAllies = rc.senseNearbyRobots(UNIT_TYPE.actionRadiusSquared, MY_TEAM);
    }

}
