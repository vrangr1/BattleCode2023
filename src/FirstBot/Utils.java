package FirstBot;

import battlecode.common.*;
import FirstBot.path.*;

public class Utils extends Constants{
    public static RobotController rc;
    public static Pathing pathing;

    public static void initUtils(RobotController rc1) throws GameActionException{
        rc = rc1;
        pathing = new Pathing();
    }
}
