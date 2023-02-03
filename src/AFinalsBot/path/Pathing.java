package AFinalsBot.path;

import AFinalsBot.Utils;
import battlecode.common.*;

public class Pathing extends Utils {
    UnitPathing up;
    MapLocation destination = null;
    static final int BYTECODE_REMAINING = 4500;
    int[] tracker = new int[113];

    int fuzzyMovesLeft = 0;
    int maxFuzzyMoves = MAX_FUZZY_MOVES;

    public Pathing() {
        switch (rc.getType()) {
            case AMPLIFIER:
                up = new BotAmplifierPathing(rc);
                break;
            case BOOSTER:
                up = new BotBoosterPathing(rc);
                break;
            case CARRIER:
                up = new BotCarrierPathing(rc);
                break;
            case DESTABILIZER:
                up = new BotDestabilizerPathing(rc);
                break;
            case LAUNCHER:
                up = new BotLauncherPathing(rc);
                break;
            case HEADQUARTERS:
                break;
        }
    }

    // Set path to new destination
    public void setNewDestination(MapLocation newDest) {
        if (destination == null || destination.distanceSquaredTo(newDest) != 0) {
            destination = newDest;
            resetTracker();
            addVisited(rc.getLocation());
        }
    }

    // Get current destination
    public MapLocation getCurrentDestination(){
        return destination;
    }

    // Turn by turn move to destination
    public void moveToDestination() throws GameActionException {
        if (destination != null) {
            pathTo(destination);
        }
    }

    public void setAndMoveToDestination(MapLocation dest) throws GameActionException{
        setNewDestination(dest);
        moveToDestination();
    }

    // Computer next direction to move to
    public void pathTo(MapLocation target) throws GameActionException {
        if (!rc.isMovementReady()) return;

        if (fuzzyMovesLeft > 0) {
            fuzzyMove(target);
            return;
        }

        if (rc.getLocation().distanceSquaredTo(target) <= 2) {
            if (rc.canMove(rc.getLocation().directionTo(target))) {
                moveTo(rc.getLocation().directionTo(target));
            }
            return;
        }
        Utils.bytecodeCheck("PreBugNavCheck");
        int nearbyRobotcount = rc.senseNearbyRobots().length;
        if (rc.getRoundNum() - BIRTH_ROUND == 3){
            Nav.goTo(rc.getLocation());
        }
        if (rc.getRoundNum() - BIRTH_ROUND < 3 || nearbyRobotcount > 15 || Clock.getBytecodesLeft() < BYTECODE_REMAINING || (Nav.bugState == Nav.BugState.BUG && target.equals(Nav.dest))) {
            Nav.goTo(target);
            destinationFlag+="$Bu1$";
            Utils.bytecodeCheck("PBN1 T:" + target+ "|Side "+ Nav.bugWallSide + "|StartDir " + Nav.bugLookStartDir);
            return;
        }
        Utils.bytecodeCheck("PreBFS");
        Direction dir = up.bestDir(target);
        Utils.bytecodeCheck("PostBFS T:" + target+ " " + dir);
        if (dir == null || !rc.canMove(dir) || isVisited(rc.getLocation().add(dir)) || !rc.sensePassability(rc.getLocation().add(dir)) || 
            rc.isLocationOccupied(rc.getLocation().add(dir))) {
            Nav.goTo(target);
            destinationFlag+="$Bu2$";
            Utils.bytecodeCheck("PBN2 T:" + target + "|Side "+ Nav.bugWallSide + "|StartDir " + Nav.bugLookStartDir);
            addVisited(rc.getLocation());
        } else {
            moveTo(dir);
            destinationFlag+="$BF1$";
        }
    }
    
    // Try to move to next cell in computed path
    public void moveTo(Direction dir) throws GameActionException {
        if (rc.canMove(dir)){
            rc.move(dir);
        }
        if (fuzzyMovesLeft > 0) {
            fuzzyMovesLeft--;
        }
        addVisited(rc.getLocation());
    }

    // If in a loop, do fuzzy move
    public void fuzzyMove(MapLocation target) throws GameActionException {
        if (!rc.isMovementReady()) return;

        // Don't move if adjacent to destination and something is blocking it
        if (rc.getLocation().distanceSquaredTo(target) <= 2 && !rc.canMove(rc.getLocation().directionTo(target))) {
            return;
        }

        Direction toDest = rc.getLocation().directionTo(target);
        Direction[] dirs = {toDest, toDest.rotateLeft(), toDest.rotateRight(), toDest.rotateLeft().rotateLeft(), toDest.rotateRight().rotateRight()};
        int cost = 99999;
        Direction optimalDir = null;
        for (int i = 0; i < dirs.length; i++) {
            if (i > 2 && cost > 0) {
                break;
            }
            Direction dir = dirs[i];
            if (rc.canMove(dir)) {
                // TODO: Have some kind of cost function
                int newCost = 10;
                if (dir == toDest) {
                    newCost -= 1;
                }
                if (newCost < cost) {
                    cost = newCost;
                    optimalDir = dir;
                }
            }
        }
        if (optimalDir != null) {
            moveTo(optimalDir);
        }
    }

    private void addVisited(MapLocation loc) {
        int bit = loc.x + 60*loc.y;
        tracker[bit >>> 5] |= 1 << (31 - bit & 31);
    }

    private boolean isVisited(MapLocation loc) {
        int bit = loc.x + 60*loc.y;
        return (tracker[bit >>> 5] & (1 << (31 - bit & 31))) != 0;
    }

    private void resetTracker() {
        tracker = new int[113];
    }

}
