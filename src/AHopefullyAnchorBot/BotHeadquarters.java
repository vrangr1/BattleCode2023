package AHopefullyAnchorBot;

import battlecode.common.*;

public class BotHeadquarters extends Utils{

    public static void initHeadquarters() throws GameActionException{
    }

    public static void runHeadquarters() throws GameActionException{
        if (rc.getRoundNum() == 2)
            Comms.initCommunicationsArray();
        Builder.buildUnits();
    }
}
