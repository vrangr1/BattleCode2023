package ACrowdBot;

import battlecode.common.*;

public class BotHeadquarters extends Utils{
    public static boolean CAN_SEE_HEADQUARTER;

    public static void initHeadquarters() throws GameActionException{
        BuilderWrapper.initBuilder();
        if (TRACKING_LAUNCHER_COUNT) Comms.resetRobotCount(RobotType.LAUNCHER);
        if (TRACKING_AMPLIFIER_COUNT) Comms.resetRobotCount(RobotType.AMPLIFIER);
    }

    public static void updateEveryTurn() throws GameActionException{
        Comms.wipeCountChannels();
        // if (rc.getRoundNum() % 2 == 0){
        //     // Comms.wipeChannelsCOMMTYPE();
        // }
    }

    private static void printMapLocation(MapLocation[] locations) throws GameActionException{
        for (int i = 0; i < locations.length; i++){
            System.out.println("location " + i + " is " + locations[i]);
        }
    }

    public static void runHeadquarters() throws GameActionException{
        if (rc.getRoundNum() == 2)
            Comms.initCommunicationsArray();
        BuilderWrapper.buildUnits();
        updateEveryTurn();
    }
}
