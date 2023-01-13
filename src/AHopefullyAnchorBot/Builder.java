package AHopefullyAnchorBot;

import battlecode.common.*;

public class Builder extends Utils {
    private static int headquarterIndex = -1;

    private static void tryToBuild(RobotType robotType, MapLocation loc) throws GameActionException{
        if (rc.canBuildRobot(robotType, loc))
            rc.buildRobot(robotType, loc);
    }

    public static void setHeadquarterIndex(int index){
        headquarterIndex = index;
    }

    private static void tryToBuild(Anchor anchor) throws GameActionException{
        if (rc.canBuildAnchor(anchor)){
            rc.buildAnchor(anchor);
            MapLocation loc = Comms.readLocationFromMessage(rc.readSharedArray(headquarterIndex - 2));
            assert loc.equals(currentLocation) : "has to be";
            Comms.writeSHAFlagMessage(rc.getNumAnchors(Anchor.STANDARD), Comms.SHAFlag.COLLECT_ANCHOR, headquarterIndex);
        }
    }

    public static void buildUnits() throws GameActionException{
        Direction dir = directions[rng.nextInt(directions.length)];
        MapLocation newLoc = rc.getLocation().add(dir);

        if (rc.getRoundNum() % 30 == 0){
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
        else{
            rc.setIndicatorString("Trying to build a launcher");
            tryToBuild(RobotType.LAUNCHER, newLoc);
        }
    }
}