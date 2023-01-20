package AManaFocusedBot;

import battlecode.common.*;

// Designed for circular exploration from either hq or a well location.
// Call initExplore() by the unit that will be exploring to setup some constants.
public class CircularExplore extends Utils{
    private static boolean startedExplore = false, reachedPerimeter = false;
    private static MapLocation centerLocation = null, revolutionStartLocation;
    private static int MIN_DISTANCE_FROM_HQ_TO_EXPLORE;
    private static int currentDistanceFromHQ = -1;
    private static final int PERIMETER_BUFFER = 5;
    private static int revolutionStartRound = -1;
    private static Direction lastDir, centerLocationDir;
    private static final int EXTRAPOLATION_DISTANCE_UNSQUARED = 5;
    private static final int REVOLUTION_COMPLETE_SQ_DISTANCE = 6, REVOLUTION_BEGUN_ROUND_BUFFER = 6;
    private static boolean isCenterHQ = false;
    
    ///////////////////////// Public Methods /////////////////////////

    public static void updateCenterLocation() throws GameActionException{ // Used to set center location to nearest hq
        startExploration();
    }

    public static void updateCenterLocation(MapLocation loc){ // Used to set center location to given well location
        // MIN_DISTANCE_FROM_HQ_TO_EXPLORE = GameConstants.MAX_DISTANCE_BETWEEN_WELLS - UNIT_TYPE.visionRadiusSquared; // 80 for carriers
        isCenterHQ = false;
        MIN_DISTANCE_FROM_HQ_TO_EXPLORE = 4;
        startedExplore = true;
        
        reachedPerimeter = false;
        currentDistanceFromHQ = -1;
        revolutionStartLocation = null;
        revolutionStartRound = -1;
        lastDir = null;
        centerLocationDir = null;
        
        centerLocation = loc;
    }

    public static MapLocation explore() throws GameActionException{
        if (!startedExplore)
            startExploration();
        updateDetails();
        // if (rc.getID() == 13837)
        //     System.out.println("explore");
        if (!reachedPerimeter) return reachPerimeter();
        if (checkRevolutionComplete() && !isCenterHQ){
            updateRevolution();
            return explore();
        }
        return computeNextTangentialLocation();
    }

    // public static void printStatus(){
    //     rc.setIndicatorString("center: " + centerLocation + "centerDir: " + centerLocationDir + "lastDir: " + lastDir + "currentDist: " + currentDistanceFromHQ + "reachedPerimeter: " + reachedPerimeter);
    // }

    // public static void printStatus(MapLocation loc){
    //     rc.setIndicatorString("loc: " + loc + ";center: " + centerLocation + ";centerDir: " + centerLocationDir + ";lastDir: " + lastDir + ";currentDist: " + currentDistanceFromHQ + ";reachedPerimeter: " + reachedPerimeter);
    //     if (rc.getID() == 13837)
    //         System.out.println("loc: " + loc + ";center: " + centerLocation + ";centerDir: " + centerLocationDir + ";lastDir: " + lastDir + ";currentDist: " + currentDistanceFromHQ + ";reachedPerimeter: " + reachedPerimeter + ";revStartLoc: " + revolutionStartLocation + ";revStartRound: " + revolutionStartRound + "\n");
    // }


    ///////////////////////// Private Methods /////////////////////////

    private static void initExplore(){
        // MIN_DISTANCE_FROM_HQ_TO_EXPLORE = GameConstants.MIN_NEAREST_AD_DISTANCE - UNIT_TYPE.visionRadiusSquared; // 80 for carriers
        MIN_DISTANCE_FROM_HQ_TO_EXPLORE = 35;
        isCenterHQ = true;
        resetExplore();
    }

    private static void updateRevolution(){
        // if (rc.getID() == 13837)
        //     System.out.println("Revolution complete");
        MIN_DISTANCE_FROM_HQ_TO_EXPLORE += 15;
        isCenterHQ = false;
        reachedPerimeter = false;
        currentDistanceFromHQ = -1;
        revolutionStartLocation = null;
        revolutionStartRound = -1;
        lastDir = null;
        centerLocationDir = null;
    }

    private static void startExploration() throws GameActionException{
        initExplore();
        startedExplore = true;
        isCenterHQ = true;
        centerLocation = Comms.findNearestHeadquarter();
    }

    private static void updateDetails(){
        currentLocation = rc.getLocation();
        currentDistanceFromHQ = currentLocation.distanceSquaredTo(centerLocation);
        centerLocationDir = currentLocation.directionTo(centerLocation);
    }


    private static void resetExplore(){
        startedExplore = false;
        centerLocation = null;
        reachedPerimeter = false;
        currentDistanceFromHQ = -1;
        revolutionStartLocation = null;
        revolutionStartRound = -1;
        lastDir = null;
        centerLocationDir = null;
    }

    private static MapLocation extrapolateAndReturn(Direction dir){
        return extrapolateLocation(currentLocation, dir, EXTRAPOLATION_DISTANCE_UNSQUARED);
    }

    private static MapLocation goToPerimeter(){
        if (checkIfDistanceInBuffer(currentDistanceFromHQ))
            return null;
        if (currentDistanceFromHQ > MIN_DISTANCE_FROM_HQ_TO_EXPLORE + PERIMETER_BUFFER)
            return centerLocation;
        Direction dir;
        dir = centerLocationDir.opposite();
        MapLocation temp = currentLocation.add(dir);
        if (rc.onTheMap(temp)){
            // if (rc.getID() == 13837)
            //     System.out.println("Going to perimeter; curLoc: " + currentLocation + "; center: " + centerLocation + "; dir: " + dir);
            return extrapolateAndReturn(dir);
        }
        if (lastDir != null){
            temp = currentLocation.add(lastDir);
            if (rc.onTheMap(temp)) return extrapolateAndReturn(lastDir);
        }
        int diff;
        switch(dir){
            case NORTH:
                diff = MAP_WIDTH - currentLocation.x - 2;
                if (lastDir != null)
                    dir = Direction.SOUTH;
                else if (currentLocation.x > diff)
                    dir = Direction.EAST;
                else
                    dir = Direction.WEST;
                break;
            case NORTHWEST:
                if (lastDir != null){
                    switch(lastDir){
                        case NORTH:
                            dir = Direction.EAST;
                            break;
                        case WEST:
                            dir = Direction.SOUTH;
                            break;
                        default: assert false;
                    }
                    break;
                }
                if (currentLocation.x == 0 && currentLocation.y + 1 == MAP_HEIGHT)
                    dir = Direction.SOUTH;
                else if (currentLocation.x == 0)
                    dir = Direction.NORTH;
                else if (currentLocation.y + 1 == MAP_HEIGHT)
                    dir = Direction.WEST;
                else
                    assert false;
                break;
            case WEST:
                diff = MAP_HEIGHT - currentLocation.y - 2;
                if (lastDir != null)
                    dir = Direction.EAST;
                else if (currentLocation.y > diff)
                    dir = Direction.NORTH;
                else
                    dir = Direction.SOUTH;
                break;
            case SOUTHWEST:
                if (lastDir != null){
                    switch(lastDir){
                        case SOUTH:
                            dir = Direction.EAST;
                            break;
                        case WEST:
                            dir = Direction.NORTH;
                            break;
                        default: assert false;
                    }
                    break;
                }
                if (currentLocation.x == 0 && currentLocation.y == 0)
                    dir = Direction.EAST;
                else if (currentLocation.x == 0)
                    dir = Direction.SOUTH;
                else if (currentLocation.y == 0)
                    dir = Direction.WEST;
                else
                    assert false;
                break;
            case SOUTH:
                diff = MAP_WIDTH - currentLocation.x - 2;
                if (lastDir != null)
                    dir = Direction.NORTH;
                else if (currentLocation.x > diff)
                    dir = Direction.EAST;
                else
                    dir = Direction.WEST;
                break;
            case SOUTHEAST:
                if (lastDir != null){
                    switch(lastDir){
                        case SOUTH:
                            dir = Direction.WEST;
                            break;
                        case EAST:
                            dir = Direction.NORTH;
                            break;
                        default: assert false;
                    }
                    break;
                }
                if (currentLocation.x + 1 == MAP_WIDTH && currentLocation.y == 0)
                    dir = Direction.NORTH;
                else if (currentLocation.x + 1 == MAP_WIDTH)
                    dir = Direction.SOUTH;
                else if (currentLocation.y == 0)
                    dir = Direction.EAST;
                else
                    assert false;
                break;
            case EAST:
                diff = MAP_HEIGHT - currentLocation.y - 2;
                if (lastDir != null)
                    dir = Direction.WEST;
                else if (currentLocation.y >= diff)
                    dir = Direction.NORTH;
                else
                    dir = Direction.SOUTH;
                break;
            case NORTHEAST:
                if (lastDir != null){
                    switch(lastDir){
                        case NORTH:
                            dir = Direction.WEST;
                            break;
                        case EAST:
                            dir = Direction.SOUTH;
                            break;
                        default: assert false;
                    }
                    break;
                }
                if (currentLocation.x + 1 == MAP_WIDTH && currentLocation.y + 1 == MAP_HEIGHT)
                    dir = Direction.WEST;
                else if (currentLocation.x + 1 == MAP_WIDTH)
                    dir = Direction.NORTH;
                else if (currentLocation.y + 1 == MAP_HEIGHT)
                    dir = Direction.EAST;
                else
                    assert false;
                break;
            default:
                assert false;
        }
        lastDir = dir;
        temp = currentLocation.add(dir);
        assert rc.onTheMap(temp);
        return extrapolateAndReturn(dir);
    }

    private static void setupRevolution(){
        reachedPerimeter = true;
        revolutionStartLocation = currentLocation;
        revolutionStartRound = rc.getRoundNum();
        lastDir = null;
        lastDir = centerLocationDir.rotateRight().rotateRight(); // TODO: Works?
    }

    private static boolean checkIfLocationInBuffer(MapLocation loc){
        if (!rc.onTheMap(loc)) return false;
        int dist = loc.distanceSquaredTo(centerLocation);
        return checkIfDistanceInBuffer(dist);
    }

    private static boolean checkIfDistanceInBuffer(int dist){
        return dist <= MIN_DISTANCE_FROM_HQ_TO_EXPLORE + PERIMETER_BUFFER && dist >= MIN_DISTANCE_FROM_HQ_TO_EXPLORE;
    }

    private static MapLocation computeNextTangentialLocation() throws GameActionException{
        Direction dir;
        MapLocation temp;
        if (currentDistanceFromHQ > MIN_DISTANCE_FROM_HQ_TO_EXPLORE + PERIMETER_BUFFER){
            dir = centerLocationDir.rotateRight();
            temp = currentLocation.add(dir);
            if (rc.onTheMap(temp)){
                lastDir = dir;
                return extrapolateAndReturn(dir);
            }
            else{
                lastDir = centerLocationDir;
                return centerLocation;
            }
        }
        else if (currentDistanceFromHQ < MIN_DISTANCE_FROM_HQ_TO_EXPLORE){
            dir = centerLocationDir.opposite().rotateLeft();
            temp = currentLocation.add(dir);
            if (rc.onTheMap(temp)){
                lastDir = dir;
                return extrapolateAndReturn(dir);
            }
            else if (lastDir != null) return extrapolateAndReturn(lastDir); // TODO: Check this
            else {
                lastDir = centerLocationDir.opposite();
                return extrapolateAndReturn(lastDir);
            }
        }

        dir = centerLocationDir.rotateRight();
        temp = currentLocation.add(dir);
        lastDir = dir;
        if (checkIfLocationInBuffer(temp))
            return extrapolateAndReturn(dir);
        dir = dir.rotateRight();
        lastDir = dir;
        temp = currentLocation.add(dir);
        if (checkIfLocationInBuffer(temp))
            return extrapolateAndReturn(dir);
        dir = dir.rotateRight();
        lastDir = dir;
        temp = currentLocation.add(dir);
        if (checkIfLocationInBuffer(temp))
            return extrapolateAndReturn(dir);
        assert false : "rc.getID(): " + rc.getID() + "rn: " + rc.getRoundNum() + " currentLocation: " + currentLocation + " centerLocation: " + centerLocation + " centerLocationDir: " + centerLocationDir + " currentDistanceFromHQ: " + currentDistanceFromHQ;
        return null;
    }

    private static MapLocation reachPerimeter() throws GameActionException{
        MapLocation dest = goToPerimeter();
        if (dest != null) return dest;
        setupRevolution();
        return computeNextTangentialLocation();
    }

    private static boolean checkRevolutionComplete() throws GameActionException{
        if (rc.getRoundNum() - revolutionStartRound <= REVOLUTION_BEGUN_ROUND_BUFFER)
            return false;
        return (currentLocation.distanceSquaredTo(revolutionStartLocation) <= REVOLUTION_COMPLETE_SQ_DISTANCE);
    }
}