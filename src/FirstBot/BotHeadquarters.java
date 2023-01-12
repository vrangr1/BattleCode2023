package FirstBot;

import battlecode.common.*;

public class BotHeadquarters extends Utils{
    private static int unitNumber = 0;

    public static void initHeadquarters() throws GameActionException{

    }

    public static void runHeadquarters() throws GameActionException{
        if (rc.getRoundNum() == 2){
            Comms.initCommunicationsArray();
        }

        Direction dir = directions[rng.nextInt(directions.length)];
        MapLocation newLoc = rc.getLocation().add(dir);

        if (unitNumber % 2 == 0 && unitNumber % 5 == 0){
            rc.setIndicatorString("Trying to build a amplifier");
            tryToBuild(RobotType.AMPLIFIER, newLoc);
        } else if (unitNumber % 2 == 0){
            rc.setIndicatorString("Trying to build a carrier");
            tryToBuild(RobotType.CARRIER, newLoc);
        } else {
            rc.setIndicatorString("Trying to build a launcher");
            tryToBuild(RobotType.LAUNCHER, newLoc);
        }
        
    }

    private static void tryToBuild(RobotType robotType, MapLocation loc) throws GameActionException{
        if (rc.canBuildRobot(robotType, loc)){
            rc.buildRobot(robotType, loc);
            unitNumber++;
        }
    }
}
