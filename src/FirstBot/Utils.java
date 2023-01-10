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
}
