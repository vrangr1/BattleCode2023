package AFirstSubmissionBot.path;

import AFirstSubmissionBot.Utils;
import battlecode.common.*;

public class BugNav extends Utils {

    // private static final int ACCEPTABLE_RUBBLE = 50;

    private static Direction bugDirection = null;

    public static Direction walkTowards(MapLocation target) throws GameActionException {
        bugDirection = null;
        if (!rc.isMovementReady()) {
            return null;
        }

        if (rc.getLocation().equals(target)) {
            return null;
        }

        Direction d = rc.getLocation().directionTo(target);
        Direction result = null;
        if (rc.canMove(d) && !isObstacle(rc, d)) {
            rc.move(d);
            bugDirection = null;
        } else {
            if (bugDirection == null) {
                bugDirection = d;
            }
            for (int i = 0; i < 8; i++) {
                if (rc.canMove(bugDirection) && !isObstacle(rc, bugDirection)) {
                    result = bugDirection;
                    bugDirection = bugDirection.rotateLeft();
                    break;
                } else {
                    bugDirection = bugDirection.rotateRight();
                }
            }
        }
        return result;
    }

    /**
     * Checks if the square we reach by moving in direction d is an obstacle.
     */
    private static boolean isObstacle(RobotController rc, Direction d) throws GameActionException {
        MapLocation adjacentLocation = rc.getLocation().add(d);
        return !rc.sensePassability(adjacentLocation);
        // return rubbleOnLocation > ACCEPTABLE_RUBBLE;
    }
}