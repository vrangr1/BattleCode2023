package ASprintBot;

import battlecode.common.*;

public class BotHeadquarters extends Utils{

    public static void initHeadquarters() throws GameActionException{
        BuilderWrapper.initBuilder();
        if (TRACKING_LAUNCHER_COUNT) Comms.resetRobotCount(RobotType.LAUNCHER);
    }

    public static void updateEveryTurn() throws GameActionException{
        Comms.wipeCountChannels();
        if (rc.getRoundNum() % 2 == 0){
            // Comms.wipeChannelsCOMMTYPE();
        }
    }

    public static void runHeadquarters() throws GameActionException{
        if (rc.getRoundNum() == 2)
            Comms.initCommunicationsArray();
        updateEveryTurn();
        BuilderWrapper.buildUnits();
    }
}
