package OManaFocusedBot;

import battlecode.common.*;

public class BotDestabilizer extends BotLauncher{

    private enum Status {
        BORN, // State at start of first turn
        EXPLORE, // No directions given
        MARCHING, // Going to a location given by comms
        ATTACKING,
        RETREATING,
        BOMBARDING,
    }

    private static Status deStabState;

    public static void initDestabilizer() throws GameActionException{
        deStabState = Status.BORN;
    }

    public static void runDestabilizer() throws GameActionException{
        updateVision();
        rc.setIndicatorString(deStabState.toString());
    }
}
