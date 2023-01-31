package APostQualsBot;

import battlecode.common.*;

public class Utils extends Globals{

    public static int intFromMapLocation(MapLocation loc){
        return (loc.x << 6) | loc.y; 
    }

    // TODO: new MapLocation might be a wrong thing to do soon when cloud and current apis are created.
    public static MapLocation mapLocationFromInt(int loc){
        return new MapLocation(loc >> 6, loc & 0x3F);
    }

    public static boolean closerToCenter(MapLocation firstLoc, MapLocation secondLoc){
        return (firstLoc.distanceSquaredTo(CENTER_OF_THE_MAP) < secondLoc.distanceSquaredTo(CENTER_OF_THE_MAP));
    }

    public static int intFromMapLocation(int x, int y){
        return (x << 6) | y;
    }

    public static MapLocation mapLocationFromCoords(int x, int y){
        return new MapLocation(x, y);
    }

    public static int intFromMapLocation(MapLocation loc, int shift){
        return (loc.x << shift) | loc.y; 
    }


    public static int manhattanDistance(MapLocation first, MapLocation second){
        return (Math.abs(first.x - second.x) + Math.abs(first.y - second.y));
    }

    public static int findPointByRatio(MapLocation src, MapLocation dest, int dist){
        int tot = src.distanceSquaredTo(dest);
        int diff = tot - dist;
        int x1 = src.x;
        int y1 = src.y;
        int x2 = dest.x;
        int y2 = dest.y;
        int x = (int)(((float)(diff * x1 + dist * x2)) / ((float) tot));
        int y = (int)(((float)(diff * y1 + dist * y2)) / ((float) tot));
        return intFromMapLocation(x, y);
    }

    public static final int getTurnFlag(){
        return (turnCount % 2);
    }

    public static final int flipTurnFlag(){
        return ((turnCount + 1) % 2);
    }


    // Set kth bit of the input number to 1
    public static final int setKthBit(int num, int k){
        return ((1 << k) | num);
    }

    // Set kth bit of the input number to 0
    public static final int unsetKthBit(int num, int k){
        return (num & ~(1 << k));
    }

    public static final int setKthBitByInput(int num, int k, int val){
        if (val == 1)
            return setKthBit(num, k);
        return unsetKthBit(num, k);
    }

    public static boolean isOnEdge(MapLocation loc){
        return (loc.x == 0 || loc.x == MAP_WIDTH - 1 || loc.y == 0 || loc.y == MAP_HEIGHT - 1);
    }

    // Very expensive computations (around 15-30 bytecodes)
    public static boolean isValidMapLocation(int x, int y){
        MapLocation loc = new MapLocation(x,y);
        return isValidMapLocation(loc);
    }


    // Bytecode Cost: 20-25
    public static boolean isValidMapLocation(MapLocation loc){ 
        return loc.x < rc.getMapWidth() && loc.x >= 0 && loc.y < rc.getMapHeight() && loc.y >= 0;
    }

    // public static boolean isOverCrowdedArchon(){
    //     int unitsBeingHealedByArchon = 0;
    //     MapLocation closest = archonLocations[0];
    //     for(int i = 0; i < archonCount; i++){
    //         if(closest!=null && rc.getLocation().distanceSquaredTo(archonLocations[i]) <= rc.getLocation().distanceSquaredTo(closest)){
    //             closest = archonLocations[i];
    //             unitsBeingHealedByArchon = Comms.readHealingUnitsNearby(i);
    //         }
    //     }
    //     return unitsBeingHealedByArchon > 5;
    // }

    public static RobotInfo getClosestUnit(RobotInfo[] units) {
		RobotInfo closestUnit = null;
		int minDistSq = Integer.MAX_VALUE;
        int distSq = 0;
        MapLocation lCR = rc.getLocation();
		for (int i = units.length; i --> 0; ) {
			distSq = lCR.distanceSquaredTo(units[i].location);
			if (distSq < minDistSq) {
				minDistSq = distSq;
				closestUnit = units[i];
			}
		}
		return closestUnit;
	}


    public static Direction directionAwayFromAllRobots(){
        RobotInfo[] senseRobots = rc.senseNearbyRobots();
        MapLocation currentTarget = rc.getLocation();
        for (int i = senseRobots.length; --i >= 0;) {
            RobotInfo aRobot = senseRobots[i];			
        	currentTarget = currentTarget.add(aRobot.location.directionTo(rc.getLocation()));
        }
        if (!rc.getLocation().equals(currentTarget)) {
        	return rc.getLocation().directionTo(currentTarget);
        }
        return null;
    }

    public static MapLocation[] createNullMapLocations(int count){
        MapLocation[] create = new MapLocation[count];
        for (int i = 0; i < count; ++i)
            create[i] = new MapLocation(-1,-1);
        return create;
    }
    public static Direction directionAwayFromGivenRobots(RobotInfo[] givenRobots){
        MapLocation currentTarget = rc.getLocation();
        for (int i = givenRobots.length; --i >= 0;) {
            RobotInfo aRobot = givenRobots[i];			
        	currentTarget = currentTarget.add(aRobot.location.directionTo(rc.getLocation()));
        }
        if (!rc.getLocation().equals(currentTarget)) {
        	return rc.getLocation().directionTo(currentTarget);
        }
        return null;
    }

    // public static MapLocation getClosestNonLeadLocation(MapLocation givenLocation) throws GameActionException{
    //     MapLocation targetLocations[] = rc.getAllLocationsWithinRadiusSquared(givenLocation, 13);
    //     MapLocation selectedLocation = null;
    //     int distToLoc = Integer.MAX_VALUE;
    //     for (int i = targetLocations.length; i --> 0; ){
    //         if (rc.canSenseLocation(targetLocations[i]) && rc.senseLead(targetLocations[i]) == 0 && !rc.canSenseRobotAtLocation(targetLocations[i]) && 
    //             targetLocations[i].distanceSquaredTo(rc.getLocation()) < distToLoc){
    //             selectedLocation =  targetLocations[i];
    //             distToLoc = selectedLocation.distanceSquaredTo(rc.getLocation());
    //         }
    //     }
    //     return selectedLocation;
    // }

    // public static int isInQuadrant(int centerX, int centerY){
    //     int dx = max(abs(px - x) - width / 2, 0);
    //     int dy = max(abs(py - y) - height / 2, 0);
    //     return dx * dx + dy * dy; //Returns zero if inside Quad(Rectangle)
    // }


    public static MapLocation findClosestCorner(MapLocation curLoc){
        int optDist = Integer.MAX_VALUE, dist;
        MapLocation loc = null;
        for (int i = -1; ++i < 4;){
            dist = MAP_CORNERS[i].distanceSquaredTo(curLoc);
            if (loc == null || dist < optDist){
                loc = MAP_CORNERS[i];
                optDist = dist;
            }
        }
        return loc;
    }


    public static void byteCodeTest(){
        // int temp = 1;
        // int index_i = 0, index_j =0;
        // int holder_1[] = new int[10];
        // int holder_2[][] = new int[10][10];
        // StringBuilder sb = new StringBuilder("111111111111111111111111111111111111111111111111111111111111");
        // StringBuilder s = sb.delete(20, 60); // 12 bytecodes irrespective of length of string
        // System.out.println("Bytecodes left before testing area: " + Clock.getBytecodesLeft()); // 7 bytecodes
        // StringBuilder s = sb.delete(20, 60); // 12 bytecodes irrespective of length of string
        // s.setCharAt(1, '2'); // 10 bytecodes (2/3 in reality)
        // s.setCharAt(2, '2');
        // s.setCharAt(3, '2');
        // s.replace(1,3,"2");
        // int holder_1[]; // 7 bytecodes (actual is 0 or 1)
        // holder_1 = new int[10]; //19 bytecodes (actual is 12/13) 
        // temp = holder_1[index_i]; // 11 bytecodes (actual is 4/5)
        // holder_1[index_i] = temp; // 11 bytecodes (actual is 4/5)
        // temp = holder_2[index_i][index_j]; // 13 bytecodes (actual is 6/7)
        // holder_2[index_i][index_j] = temp; // 13 bytecodes (actual is 6/7)	
        // System.out.println("Bytecodes left after testing area: " + Clock.getBytecodesLeft());
    }


    public static MapLocation translateByStepsCount(MapLocation src, MapLocation dest, int count) throws GameActionException{
        double angle = Math.atan2(dest.y-src.y, dest.x - src.x);
        double x = src.x, y = src.y;
        x += Math.cos(angle)*count;
        y += Math.sin(angle)*count;
        return new MapLocation((int)x, (int)y);
    }


    public static MapLocation translateByStepsCountUsingDir(MapLocation src, MapLocation dest, int count) throws GameActionException{
        Direction dir = src.directionTo(dest);
        MapLocation intermediateStep;
        for (int i = -1; ++i < count;){
            intermediateStep = src.add(dir);
            if (!isValidMapLocation(intermediateStep)) return src;
            src = intermediateStep;
        }
        return src;
    }

    public static MapInfo getMapInfo(MapLocation src) throws GameActionException{
        return rc.senseMapInfo(src);
    }

    /**
     * Returns the nearest location to the bot out of the list of locations provided
     * @param locations : list of locations
     * @return : the nearest location
     * @BytecodeCost: ~10 * locations.length
     */
    public static MapLocation getNearestLocation(MapLocation[] locations){
        MapLocation optLoc = null, myLoc = rc.getLocation();
        int optDist = -1, curDist;
        for (MapLocation loc: locations){
            curDist = myLoc.distanceSquaredTo(loc);
            if (optLoc == null){
                optLoc = loc;
                optDist = curDist;
                continue;
            }
            if (curDist < optDist){
                optLoc = loc;
                optDist = curDist;
            }
            else if (curDist == optDist){} // TODO: Deal with this case (by clouds/nonclouds, etc)
        }
        return optLoc;
    }

    public static int senseRubbleFriend(MapInfo locInfo) throws GameActionException{
        return (int) (1+locInfo.getCooldownMultiplier(MY_TEAM)) * 10;
    }

    public static int senseRubbleEnemy(MapInfo locInfo) throws GameActionException{
        return (int) (1+locInfo.getCooldownMultiplier(ENEMY_TEAM)) * 10;
    }

    public static int senseRubbleFriend(MapLocation loc) throws GameActionException{
        return (int) (1+rc.senseMapInfo(loc).getCooldownMultiplier(MY_TEAM)) * 10;
    }

    public static int senseRubbleEnemy(MapLocation loc) throws GameActionException{
        return (int) (1+rc.senseMapInfo(loc).getCooldownMultiplier(ENEMY_TEAM)) * 10;
    }

    public static void findAndWriteWellLocationsToComms() throws GameActionException{
        if (rc.canWriteSharedArray(0, 0)){
            WellInfo[] nearbyWells = rc.senseNearbyWells();
            MapLocation loc;
            Comms.SHAFlag flag;
            if (nearbyWells.length > 0){
                WellInfo well = nearbyWells[0];
                loc = well.getMapLocation();
                flag = Comms.resourceFlag(well.getResourceType());
                if (Comms.findIfLocationAlreadyPresent(loc, Comms.COMM_TYPE.WELLS, flag))
                    return;
                Comms.writeAndOverwriteLesserPriorityMessage(Comms.COMM_TYPE.WELLS, loc, flag);
            }
        }
    }

    public static void bytecodeCheck(){
        int bytecodesLeft = Clock.getBytecodesLeft();
        rc.setIndicatorString("BC " + bytecodesLeft +"|SNo. " + bytecodeCounter + " " + destinationFlag);
        bytecodeCounter++;
    }

    public static void bytecodeCheck(String flag){
        int bytecodesLeft = Clock.getBytecodesLeft();
        rc.setIndicatorString("BC " + bytecodesLeft +"| " + flag + " " + destinationFlag);
    }


    public static MapLocation interpolate(MapLocation loc, Direction dir){
        MapLocation lastViableLocation = null;
        while(currentLocation.distanceSquaredTo(loc) < UNIT_TYPE.actionRadiusSquared){
            if (rc.canActLocation(loc)) lastViableLocation = loc;
            loc = loc.add(dir);
        }
        return lastViableLocation;
    }

    // targetLocation is out of action radius
    public static MapLocation getInterpolatedActionReadyLocation(MapLocation targetLocation) throws GameActionException{
        currentLocation = rc.getLocation();
        Direction dir = currentLocation.directionTo(targetLocation);
        MapLocation loc = currentLocation.add(dir);
        MapLocation res = interpolate(loc, dir);
        if (res != null) return res;
        dir = dir.rotateLeft();
        loc = currentLocation.add(dir);
        res = interpolate(loc, dir);
        if (res != null) return res;
        dir = dir.rotateRight().rotateRight();
        loc = currentLocation.add(dir);
        res = interpolate(loc, dir);
        if (res != null) return res;
        return null;
    }

    private static boolean isMapLocationBlocked(MapLocation loc) throws GameActionException{
        Direction dir;
        MapLocation newLoc;
        currentLocation = rc.getLocation();
        for (int i = directions.length; --i>=0;){
            dir = directions[i];
            newLoc = loc.add(dir);
            if (!rc.canSenseLocation(newLoc))
                continue;
            if (rc.senseMapInfo(newLoc).isPassable()) return false;
        }
        return true;
    }

    public static MapLocation findNearestActReadyLocation(MapLocation targetLocation, RobotType type) throws GameActionException{
        currentLocation = rc.getLocation();
        MapLocation[] locations = rc.getAllLocationsWithinRadiusSquared(currentLocation, UNIT_TYPE.actionRadiusSquared);
        MapLocation optLoc = null;
        int optDist = -1, curDist;
        MapLocation loc;
        for (int i = locations.length; --i>=0;){
            loc = locations[i];
            // if (!rc.canActLocation(loc)) continue;
            if (!rc.canBuildRobot(type, loc)) continue;
            curDist = loc.distanceSquaredTo(targetLocation);
            if (optLoc == null || curDist < optDist){
                optLoc = loc;
                optDist = curDist;
                continue;
            }
        }
        return optLoc;
        // if (currentLocation.distanceSquaredTo(targetLocation) > UNIT_TYPE.actionRadiusSquared) return getInterpolatedActionReadyLocation(targetLocation);
        // return null;
    }

    public static void combatCommsCleaner(int vNonHQEnemies) throws GameActionException{
        MapLocation combatLoc = Comms.findNearestLocationOfThisType(rc.getLocation(), Comms.COMM_TYPE.COMBAT, Comms.SHAFlag.COMBAT_LOCATION);
        if (!rc.canWriteSharedArray(0, 0)) return;
        if (combatLoc == null)  return;
        if (vNonHQEnemies > 0)  return;
        if (rc.getLocation().distanceSquaredTo(combatLoc) <= UNIT_TYPE.visionRadiusSquared){
            Comms.wipeThisLocationFromChannels(Comms.COMM_TYPE.COMBAT, Comms.SHAFlag.COMBAT_LOCATION, combatLoc);
        }
    }

    public static MapLocation extrapolateLocation(MapLocation source, Direction dir, int unsquaredDistance){
        double angle = Math.atan2(dir.dy, dir.dx);
        double x = source.x, y = source.y;
        x += (Math.cos(angle)*(double)unsquaredDistance);
        y += (Math.sin(angle)*(double)unsquaredDistance);
        // if (CircularExplore.DEBUG_PRINT && rc.getID() == CircularExplore.DEBUG_ID){
        //     System.out.println("source: " + source + "; dir: " + dir + "; angle: " + angle + " extrapolated location: [" + (int)x + ", " + (int)y + "]");
        // }
        return new MapLocation((int)x, (int)y);
    }

}
