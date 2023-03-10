package OHealingBot;

import battlecode.common.*;

// Designed for circular exploration from either hq or a well location.
// Call initExplore() by the unit that will be exploring to setup some constants.
public class CircularExplore extends Utils{
    private static boolean startedExplore = false, reachedPerimeter = false;
    private static MapLocation centerLocation = null, revolutionStartLocation;
    private static int MIN_DISTANCE_FROM_HQ_TO_EXPLORE;
    private static int currentDistanceFromHQ = -1;
    private static int PERIMETER_BUFFER = 7;
    private static int revolutionStartRound = -1;
    private static Direction lastDir, centerLocationDir;
    private static final int EXTRAPOLATION_DISTANCE_UNSQUARED = 5;
    private static final int REVOLUTION_COMPLETE_SQ_DISTANCE = 6, REVOLUTION_BEGUN_ROUND_BUFFER = 6;
    private static boolean isCenterHQ = false;
    private static boolean rotateAntiClockwise = true;
    private static MapLocation lastOnTheMapLocation = null;
    private static final int WELL_INITIAL_EXPLORE_RADIUS = 18;
    public static final boolean DEBUG_PRINT = false;
    public static final int DEBUG_ID = 13757;
    private static final int MAX_EXPLORE_ROUNDS_ALLOWED = 90;
    private static int exploreRoundCount = 0;
    private static MapLocation previousLocation;
    private static final int PREVIOUS_LOCATION_ROUND_DIFF = 5;
    private static int previousLocationRoundNum = BIRTH_ROUND;
    
    ///////////////////////// Public Methods /////////////////////////

    public static void resetCenterLocation(){
        centerLocation = null;
    }

    public static void updateCenterLocation() throws GameActionException{ // Used to set center location to nearest hq
        startExploration();
        exploreRoundCount = 0;
    }

    public static MapLocation getCenterLocation(){
        return centerLocation;
    }

    public static void updateCenterLocation(int k) throws GameActionException{ // Used to set center location to kth (1-indexed) nearest hq
        if (k == 1) updateCenterLocation();
        else{
            centerLocation = Comms.findKthNearestHeadquarter(k);
            initExplore();
            startedExplore = true;
            isCenterHQ = true;
        }
    }

    public static void updateCenterLocation(MapLocation loc){ // Used to set center location to given well location
        // MIN_DISTANCE_FROM_HQ_TO_EXPLORE = GameConstants.MAX_DISTANCE_BETWEEN_WELLS - UNIT_TYPE.visionRadiusSquared; // 80 for carriers
        centerLocation = loc;
        isCenterHQ = true;
        // setCircleRadiusForWells();
        MIN_DISTANCE_FROM_HQ_TO_EXPLORE = WELL_INITIAL_EXPLORE_RADIUS;
        PERIMETER_BUFFER = MIN_DISTANCE_FROM_HQ_TO_EXPLORE;
        startedExplore = true;
        
        reachedPerimeter = false;
        currentDistanceFromHQ = -1;
        revolutionStartLocation = null;
        revolutionStartRound = -1;
        lastDir = null;
        exploreRoundCount = 0;
        currentLocation = rc.getLocation();
        centerLocationDir = currentLocation.directionTo(centerLocation);
        decideRotationDirection();
    }

    public static void updateCenterLocationForLauncher(MapLocation loc){
        isCenterHQ = false;
        MIN_DISTANCE_FROM_HQ_TO_EXPLORE = RobotType.HEADQUARTERS.actionRadiusSquared + 5;
        PERIMETER_BUFFER = MIN_DISTANCE_FROM_HQ_TO_EXPLORE;
        startedExplore = true;
        
        reachedPerimeter = false;
        currentDistanceFromHQ = -1;
        revolutionStartLocation = null;
        revolutionStartRound = -1;
        lastDir = null;
        centerLocation = loc;
        currentLocation = rc.getLocation();
        centerLocationDir = currentLocation.directionTo(centerLocation);
        decideRotationDirection();
    }

    public static MapLocation explore() throws GameActionException{
        if (!startedExplore)
            startExploration();
        updateDetails();
        MapLocation temp = null;
        if (UNIT_TYPE == RobotType.CARRIER && exploreRoundCount > MAX_EXPLORE_ROUNDS_ALLOWED){
            // System.out.println("Max explore rounds reached");
            if (DEBUG_PRINT && rc.getID() == DEBUG_ID)
                System.out.println("Max explore rounds reached");
            temp = Comms.findNearestLocationOfThisType(rc.getLocation(), Comms.COMM_TYPE.WELLS, Comms.resourceFlag(BotCarrier.getLocalPrioritizedResource()));
            if (temp != null) return temp;
            else return Explore.explore(true);
        }
        temp = doGeffnersExplore();
        if (temp != null) return temp;
        // if (rc.getID() == 13837)
        //     System.out.println("explore");
        if (!reachedPerimeter) return reachPerimeter();
        if (isCenterHQ && checkRevolutionComplete()){
            if (DEBUG_PRINT && rc.getID() == DEBUG_ID)
                System.out.println("Revolution complete");
            updateRevolution();
            return explore();
        }
        temp = computeNextTangentialLocation();
        if (DEBUG_PRINT && rc.getID() == DEBUG_ID){
            System.out.println("Next tangential location: " + temp);
            rc.setIndicatorDot(temp, 255, 0, 0);
        }
        return temp;
    }

    public static void printStatus(){
        rc.setIndicatorString("center: " + centerLocation + "centerDir: " + centerLocationDir + "lastDir: " + lastDir + "currentDist: " + currentDistanceFromHQ + "reachedPerimeter: " + reachedPerimeter);
    }

    public static void printStatus(MapLocation loc1, MapLocation loc2){
        rc.setIndicatorString("loc: " + loc1 + "; loc2: " + loc2 + "; center: " + centerLocation + ";centerDir: " + centerLocationDir + ";lastDir: " + lastDir + ";currentDist: " + currentDistanceFromHQ + ";reachedPerimeter: " + reachedPerimeter);
        if (rc.getID() == DEBUG_ID)
            System.out.println("loc1: " + loc1 + "; loc2: " + loc2 + "; center: " + centerLocation + ";centerDir: " + centerLocationDir + "; anticlockwise: " + rotateAntiClockwise + "; lastDir: " + lastDir + ";currentDist: " + currentDistanceFromHQ + ";reachedPerimeter: " + reachedPerimeter + ";revStartLoc: " + revolutionStartLocation + ";revStartRound: " + revolutionStartRound + "\n");
    }

    public static void startExploration() throws GameActionException{
        centerLocation = Comms.findNearestHeadquarter();
        initExplore();
        startedExplore = true;
        isCenterHQ = true;
        currentLocation = rc.getLocation();
        centerLocationDir = currentLocation.directionTo(centerLocation);
        decideRotationDirection();
    }


    ///////////////////////// Private Methods /////////////////////////

    private static void setCircleRadiusForHQ(){
        assert centerLocation != null;
        double centerCENTERDist = Math.sqrt(centerLocation.distanceSquaredTo(CENTER_OF_THE_MAP));
        double ans = centerCENTERDist - (Math.sqrt(UNIT_TYPE.visionRadiusSquared) - 1);
        if (ans > 0)
            MIN_DISTANCE_FROM_HQ_TO_EXPLORE = Math.min((int)(ans * ans), 52);
        else MIN_DISTANCE_FROM_HQ_TO_EXPLORE = Math.min(rc.getLocation().distanceSquaredTo(centerLocation), 20);
        MIN_DISTANCE_FROM_HQ_TO_EXPLORE = Math.max(MIN_DISTANCE_FROM_HQ_TO_EXPLORE, 15);
        PERIMETER_BUFFER = MIN_DISTANCE_FROM_HQ_TO_EXPLORE;
    }

    private static void updateCarrierStuff(){
        if (UNIT_TYPE != RobotType.CARRIER) return;
        int curRoundNum = rc.getRoundNum();
        if (curRoundNum - previousLocationRoundNum > 5){
            previousLocation = rc.getLocation();
            previousLocationRoundNum = curRoundNum;
            return;
        }
        if (curRoundNum - previousLocationRoundNum == PREVIOUS_LOCATION_ROUND_DIFF){
            previousLocation = rc.getLocation();
            previousLocationRoundNum = curRoundNum;
        }
    }

    private static boolean checkIfBlocked(){
        if (previousLocation == null || rc.getRoundNum() - previousLocationRoundNum != PREVIOUS_LOCATION_ROUND_DIFF - 1) return false;
        return rc.getLocation().distanceSquaredTo(previousLocation) < 4;
    }

    private static MapLocation doGeffnersExplore() throws GameActionException{
        if (UNIT_TYPE != RobotType.CARRIER || !checkIfBlocked()) return null;
        if (DEBUG_PRINT && rc.getID() == DEBUG_ID)
            System.out.println("Blocked. Doing geffners explore");
        MapLocation temp = Comms.findNearestLocationOfThisType(rc.getLocation(), Comms.COMM_TYPE.WELLS, Comms.resourceFlag(BotCarrier.getLocalPrioritizedResource()));
        if (temp != null) return temp;
        else return Explore.explore(true);
    }

    private static void setCircleRadiusForWells(){
        // MIN_DISTANCE_FROM_HQ_TO_EXPLORE = WELL_INITIAL_EXPLORE_RADIUS;
        double centerCENTERDist = Math.sqrt(centerLocation.distanceSquaredTo(CENTER_OF_THE_MAP));
        double ans = centerCENTERDist - (Math.sqrt(UNIT_TYPE.visionRadiusSquared) - 1);
        if (ans > 0)
            MIN_DISTANCE_FROM_HQ_TO_EXPLORE = Math.min((int)(ans * ans), WELL_INITIAL_EXPLORE_RADIUS);
        else MIN_DISTANCE_FROM_HQ_TO_EXPLORE = Math.min(rc.getLocation().distanceSquaredTo(centerLocation), 20);
        MIN_DISTANCE_FROM_HQ_TO_EXPLORE = Math.max(MIN_DISTANCE_FROM_HQ_TO_EXPLORE, 15);
        PERIMETER_BUFFER = MIN_DISTANCE_FROM_HQ_TO_EXPLORE;
    }

    private static void initExplore(){
        // MIN_DISTANCE_FROM_HQ_TO_EXPLORE = GameConstants.MIN_NEAREST_AD_DISTANCE - UNIT_TYPE.visionRadiusSquared; // 80 for carriers
        MIN_DISTANCE_FROM_HQ_TO_EXPLORE = 35;
        PERIMETER_BUFFER = 35;
        // setCircleRadiusForHQ();
        isCenterHQ = true;
        exploreRoundCount = 0;
        resetExplore();
    }

    private static void updateRevolution(){
        if (DEBUG_PRINT && rc.getID() == DEBUG_ID)
            System.out.println("Revolution complete");
        MIN_DISTANCE_FROM_HQ_TO_EXPLORE += MIN_DISTANCE_FROM_HQ_TO_EXPLORE;
        PERIMETER_BUFFER = MIN_DISTANCE_FROM_HQ_TO_EXPLORE;
        // isCenterHQ = false;
        reachedPerimeter = false;
        currentDistanceFromHQ = -1;
        revolutionStartLocation = null;
        revolutionStartRound = -1;
        lastDir = null;
        centerLocationDir = null;
    }

    private static void decideRotationDirection(){
        MapLocation loc1, loc2;
        Direction dir1 = centerLocationDir.rotateLeft().rotateLeft();
        Direction dir2 = centerLocationDir.rotateRight().rotateRight();
        loc1 = extrapolateAndReturn(dir1);
        loc2 = extrapolateAndReturn(dir2);
        if (loc1.distanceSquaredTo(CENTER_OF_THE_MAP) < loc2.distanceSquaredTo(CENTER_OF_THE_MAP))
            rotateAntiClockwise = false;
        else
            rotateAntiClockwise = true;
        if (DEBUG_PRINT && rc.getID() == DEBUG_ID)
            System.out.println("rotating anticlockwise: " + rotateAntiClockwise);
    }

    private static void updateDetails(){
        currentLocation = rc.getLocation();
        currentDistanceFromHQ = currentLocation.distanceSquaredTo(centerLocation);
        centerLocationDir = currentLocation.directionTo(centerLocation);
        // lastOnTheMapLocation = null;
        exploreRoundCount++;
        updateCarrierStuff();
        // rotateAntiClockwise = true;
    }


    private static void resetExplore(){
        startedExplore = false;
        // centerLocation = null;
        reachedPerimeter = false;
        currentDistanceFromHQ = -1;
        revolutionStartLocation = null;
        revolutionStartRound = -1;
        lastDir = null;
        centerLocationDir = null;
    }

    public static MapLocation extrapolateAndReturn(Direction dir){
        return extrapolateLocation(currentLocation, dir, EXTRAPOLATION_DISTANCE_UNSQUARED);
    }

    private static MapLocation goToPerimeter(){
        if (checkIfDistanceInBuffer(currentDistanceFromHQ))
            return null;
        if (currentDistanceFromHQ > MIN_DISTANCE_FROM_HQ_TO_EXPLORE + PERIMETER_BUFFER){
            return centerLocation;
        }
        Direction dir;
        dir = centerLocationDir.opposite();
        MapLocation temp = currentLocation.add(dir);
        if (rc.onTheMap(temp)){
            // if (DEBUG_PRINT && rc.getID() == DEBUG_ID)
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
                if (lastDir != null || currentLocation.x == 0 || currentLocation.x == MAP_WIDTH - 1)
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
                if (lastDir != null || currentLocation.y == 0 || currentLocation.y == MAP_HEIGHT - 1)
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
                if (lastDir != null || currentLocation.x == 0 || currentLocation.x == MAP_WIDTH - 1)
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
                if (lastDir != null || currentLocation.y == 0 || currentLocation.y == MAP_HEIGHT - 1)
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
        if (DEBUG_PRINT && rc.getID() == DEBUG_ID)
            System.out.println("reached perimeter");
        reachedPerimeter = true;
        revolutionStartLocation = currentLocation;
        revolutionStartRound = rc.getRoundNum();
        // lastDir = centerLocationDir.rotateRight().rotateRight();
        lastDir = updateDirection(centerLocationDir);
        lastDir = updateDirection(lastDir);
    }

    private static boolean checkIfLocationInBuffer(MapLocation loc){
        if (!rc.onTheMap(loc)) return false;
        // lastOnTheMapLocation = loc;
        int dist = loc.distanceSquaredTo(centerLocation);
        return checkIfDistanceInBuffer(dist);
    }

    private static boolean checkIfDistanceInBuffer(int dist){
        return dist <= MIN_DISTANCE_FROM_HQ_TO_EXPLORE + PERIMETER_BUFFER && dist >= MIN_DISTANCE_FROM_HQ_TO_EXPLORE;
    }

    private static Direction updateDirection(Direction dir){
        if (rotateAntiClockwise)
            return dir.rotateRight();
        else return dir.rotateLeft();
    }

    private static MapLocation locationComputationIterationOld(){
        Direction dir;
        MapLocation temp;
        lastOnTheMapLocation = null;
        // dir = centerLocationDir.rotateRight();
        dir = updateDirection(centerLocationDir);
        temp = currentLocation.add(dir);
        lastDir = dir;
        if (checkIfLocationInBuffer(temp))
            return extrapolateAndReturn(dir);
        // dir = dir.rotateRight();
        dir = updateDirection(dir);
        lastDir = dir;
        temp = currentLocation.add(dir);
        if (checkIfLocationInBuffer(temp))
            return extrapolateAndReturn(dir);
        dir = updateDirection(dir);
        lastDir = dir;
        temp = currentLocation.add(dir);
        if (checkIfLocationInBuffer(temp))
            return extrapolateAndReturn(dir);
        return null;
    }

    private static MapLocation locationComputationIteration(){
        Direction tangentialDir = updateDirection(updateDirection(centerLocationDir));
        
        if (!rc.onTheMap(extrapolateAndReturn(tangentialDir))) return null;
        if (currentDistanceFromHQ > MIN_DISTANCE_FROM_HQ_TO_EXPLORE + PERIMETER_BUFFER){
            lastDir = updateDirection(centerLocationDir);
            return extrapolateAndReturn(lastDir);
        }
        else if (currentDistanceFromHQ < MIN_DISTANCE_FROM_HQ_TO_EXPLORE){
            lastDir = updateDirection(updateDirection(centerLocationDir));
            return extrapolateAndReturn(lastDir);
        }
        lastDir = updateDirection(updateDirection(centerLocationDir));
        return extrapolateAndReturn(lastDir);
    }

    private static MapLocation computeNextTangentialLocation() throws GameActionException{
        // boolean withinBuffer = checkIfDistanceInBuffer(currentLocation.distanceSquaredTo(centerLocation));
        MapLocation nextLoc = locationComputationIteration();
        if (nextLoc != null) return nextLoc;
        if (DEBUG_PRINT && rc.getID() == DEBUG_ID) 
            System.out.println("flipping rotation direction");
        rotateAntiClockwise = !rotateAntiClockwise;
        nextLoc = locationComputationIteration();
        if (nextLoc != null) return nextLoc;
        return Explore.explore(true);
        // assert false : "rc.getID(): " + rc.getID() + "rn: " + rc.getRoundNum() + " currentLocation: " + currentLocation + " centerLocation: " + centerLocation + " centerLocationDir: " + centerLocationDir + " currentDistanceFromHQ: " + currentDistanceFromHQ;
        // return null;
    }

    private static MapLocation reachPerimeter() throws GameActionException{
        MapLocation dest = goToPerimeter();
        if (dest != null){
            if (DEBUG_PRINT && rc.getID() == DEBUG_ID){
                System.out.println("Going to perimeter: " + dest);
                rc.setIndicatorDot(dest, 255, 0, 0);
            }
            return dest;
        }
        setupRevolution();
        dest = computeNextTangentialLocation();
        if (DEBUG_PRINT && rc.getID() == DEBUG_ID){
            System.out.println("Next tangential location: " + dest);
            rc.setIndicatorDot(dest, 255, 0, 0);
        }
        return dest;
    }

    private static boolean checkRevolutionComplete() throws GameActionException{
        if (rc.getRoundNum() - revolutionStartRound <= REVOLUTION_BEGUN_ROUND_BUFFER)
            return false;
        return (currentLocation.distanceSquaredTo(revolutionStartLocation) <= REVOLUTION_COMPLETE_SQ_DISTANCE);
    }
}
