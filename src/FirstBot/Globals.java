package FirstBot;

import battlecode.common.*;
import java.util.Random;
import FirstBot.path.*;

public class Globals {
    
    public static final int MAX_FUZZY_MOVES = 3;
    public static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };
    /**
     * A random number generator.
     * We will use this RNG to make some random moves. The Random class is provided by the java.util.Random
     * import at the top of this file. Here, we *seed* the RNG with a constant number (6147); this makes sure
     * we get the same sequence of numbers every time this code is run. This is very useful for debugging!
     */
    static final Random rng = new Random(6147);

    public static final int COMMS_VARCOUNT = GameConstants.SHARED_ARRAY_LENGTH;

    public static RobotController rc;
    public static Pathing pathing;
    public static int MAX_WELLS_COUNT; // 144 in the worst possible case (60 x 60 map size and full 4% of map is wells)

    public static void initGlobals(RobotController rc1) throws GameActionException{
        rc = rc1;
        pathing = new Pathing();
        MAX_WELLS_COUNT = (int)(GameConstants.MAX_MAP_PERCENT_WELLS * ((float)(rc.getMapHeight() * rc.getMapWidth())));
    }

}
