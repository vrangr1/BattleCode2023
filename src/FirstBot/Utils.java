package FirstBot;

import battlecode.common.*;
import FirstBot.path.*;

public class Utils extends Constants{
    public static RobotController rc;
    public static Pathing pathing;
    public static int MAX_WELLS_COUNT; // 144 in the worst possible case (60 x 60 map size and full 4% of map is wells)

    public static void initUtils(RobotController rc1) throws GameActionException{
        rc = rc1;
        pathing = new Pathing();
        MAX_WELLS_COUNT = (int)(GameConstants.MAX_MAP_PERCENT_WELLS * ((float)(rc.getMapHeight() * rc.getMapWidth())));
    }

    public static int intFromMapLocation(MapLocation loc){
        return (loc.x << 6) | loc.y; 
    }

    // TODO: new MapLocation might be a wrong thing to do soon when cloud and current apis are created.
    public static MapLocation mapLocationFromInt(int loc){
        return new MapLocation(loc >> 6, loc & 0x3F);
    }
}
