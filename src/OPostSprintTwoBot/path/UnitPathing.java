package OPostSprintTwoBot.path;

import battlecode.common.*;

public interface UnitPathing {
    public Direction bestDir(MapLocation target) throws GameActionException;
}
