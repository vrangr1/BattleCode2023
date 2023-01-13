package HardlyWorkingBot;

import HardlyWorkingBot.Comms.SHAFlag;
import battlecode.common.*;

public class BotHeadquarters extends Utils{
    private static int unitNumber = 0;
    public static int headquarterIndex = -1;

    public static void initHeadquarters() throws GameActionException{
    }

    public static void runHeadquarters() throws GameActionException{
        if (rc.getRoundNum() == 2){
            Comms.initCommunicationsArray();
        }

        Direction dir = directions[rng.nextInt(directions.length)];
        MapLocation newLoc = rc.getLocation().add(dir);

        if (rc.getRoundNum() % 50 == 0){
            rc.setIndicatorString("Trying to build a amplifier");
            tryToBuild(RobotType.AMPLIFIER, newLoc);
        } else if (rc.getRoundNum() % 40 == 0){
            rc.setIndicatorString("Trying to build a carrier");
            tryToBuild(RobotType.CARRIER, newLoc);
        } 
        else if (rc.getRoundNum() % 70 == 0){
            rc.setIndicatorString("Trying to build an anchor");
            tryToBuild(Anchor.STANDARD);
        } 
        else {
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

    private static void tryToBuild(Anchor anchor) throws GameActionException{
        if (rc.canBuildAnchor(anchor)){
            rc.buildAnchor(anchor);
            unitNumber++;
            MapLocation loc = Comms.readLocationFromMessage(rc.readSharedArray(headquarterIndex - 2));
            // assert loc == currentLocation : "has to be; read loc " + loc + "; curLoc: " + currentLocation;
            if (!loc.equals(currentLocation)) assert false;
            Comms.writeSHAFlagMessage(currentLocation, SHAFlag.COLLECT_ANCHOR, headquarterIndex);
        }
    }
}
