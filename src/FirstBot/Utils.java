package FirstBot;

import battlecode.common.*;

public class Utils extends Globals{

    public static int intFromMapLocation(MapLocation loc){
        return (loc.x << 6) | loc.y; 
    }

    // TODO: new MapLocation might be a wrong thing to do soon when cloud and current apis are created.
    public static MapLocation mapLocationFromInt(int loc){
        return new MapLocation(loc >> 6, loc & 0x3F);
    }

    // Bytecode Cost: 20-25
    public static boolean isValidMapLocation(MapLocation loc){ 
        return loc.x < rc.getMapWidth() && loc.x >= 0 && loc.y < rc.getMapHeight() && loc.y >= 0;
    }

    public static Direction directionAwayFromAllRobots(){
        RobotInfo[] senseRobots = rc.senseNearbyRobots();
        MapLocation currentTarget = rc.getLocation();
        for (int i = senseRobots.length; --i >= 0;) {
            RobotInfo aRobot = senseRobots[i];			
        	currentTarget = currentTarget.add(aRobot.location.directionTo(rc.getLocation()));
        }
        if (!rc.getLocation().equals(currentTarget)) {
        	return rc.getLocation().directionTo(currentTarget);
        }
        return null;
    }

    public static MapLocation[] createNullMapLocations(int count){
        MapLocation[] create = new MapLocation[count];
        for (int i = 0; i < count; ++i)
            create[i] = new MapLocation(-1,-1);
        return create;
    }
}
