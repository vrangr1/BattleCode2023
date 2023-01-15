package ABuildABot;

import battlecode.common.*;

public class BotHeadquarters extends Utils{

    public static void initHeadquarters() throws GameActionException{
        Builder.initBuilder();
        if (TRACKING_LAUNCHER_COUNT) Comms.resetRobotCount(RobotType.LAUNCHER);
    }

    public static void updateEveryTurn() throws GameActionException{
        Comms.wipeCountChannels();
    }

    public static void runHeadquarters() throws GameActionException{
        if (rc.getRoundNum() == 2)
            Comms.initCommunicationsArray();
        updateEveryTurn();
        Builder.buildUnits();
    }
}
