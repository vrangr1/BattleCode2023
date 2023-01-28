package AnElixirBot;

import battlecode.common.*;

public class Symmetry extends Utils{
    public static enum SYMMETRY{
        VERTICAL(0b100),
        HORIZONTAL(0b010),
        ROTATIONAL(0b001);

        int symVal = 0;
        SYMMETRY(int symmetry){
            this.symVal = symmetry;
        }

        int getSym(){
            return symVal;
        }
    }

    private static boolean isCurrentSymmetricallyMatched(Direction dir1, Direction dir2, int dx, int dy) throws GameActionException{
        SYMMETRY sym = (dx == 0) ? SYMMETRY.HORIZONTAL : ((dy == 0) ? SYMMETRY.VERTICAL : SYMMETRY.ROTATIONAL);
        switch(sym){
            case VERTICAL:
                switch(dir1){
                    case CENTER:
                    case NORTH:
                    case SOUTH: return dir1 == dir2;
                    case NORTHEAST: return dir2 == Direction.NORTHWEST;
                    case SOUTHEAST: return dir2 == Direction.SOUTHWEST;
                    case NORTHWEST: return dir2 == Direction.NORTHEAST;
                    case SOUTHWEST: return dir2 == Direction.SOUTHEAST;
                    case EAST: return dir2 == Direction.WEST;
                    case WEST: return dir2 == Direction.EAST;
                }
            case HORIZONTAL:
                switch(dir1){
                    case CENTER:
                    case EAST:
                    case WEST: return dir1 == dir2;
                    case NORTHEAST: return dir2 == Direction.SOUTHEAST;
                    case SOUTHEAST: return dir2 == Direction.NORTHEAST;
                    case NORTHWEST: return dir2 == Direction.SOUTHWEST; 
                    case SOUTHWEST: return dir2 == Direction.NORTHWEST;
                    case NORTH: return dir2 == Direction.SOUTH;
                    case SOUTH: return dir2 == Direction.NORTH;
                }
            default : assert false; return true;
        }
    }

    private static boolean areEqualInPropertiesNoClouds(MapLocation first, MapLocation second) throws GameActionException{
        // walls, currents, wells, islands
        int firstIsland = rc.senseIsland(first), secondIsland = rc.senseIsland(second);
        switch(firstIsland){
            case -1:
                switch(secondIsland){
                    case -1: break;
                    default: return false;
                }
                break;
            default:
                switch(secondIsland){
                    case -1: return false;
                    default: return true;
                }
        }
        MapInfo firstInfo = rc.senseMapInfo(first), secondInfo = rc.senseMapInfo(second);
        if (firstInfo.isPassable() != secondInfo.isPassable()) return false;
        if (!isCurrentSymmetricallyMatched(firstInfo.getCurrentDirection(), secondInfo.getCurrentDirection(), first.x - second.x, first.y - second.y)) return false;
        WellInfo firstWell = rc.senseWell(first), secondWell = rc.senseWell(second);
        if (firstWell == null && secondWell != null) return false;
        if (firstWell != null && secondWell == null) return false;
        if (firstWell != null && !firstWell.getResourceType().equals(secondWell.getResourceType())) return false;
        return true;
    }

    private static boolean areEqualInProperties(MapLocation first, MapLocation second) throws GameActionException{
        // walls, currents, clouds, wells, islands
        boolean isCloud = rc.senseCloud(first);
        if (isCloud != rc.senseCloud(second)) return false;
        if (isCloud){
            boolean firstSense = first.isWithinDistanceSquared(currentLocation, GameConstants.CLOUD_VISION_RADIUS_SQUARED), secondSense = second.isWithinDistanceSquared(currentLocation, GameConstants.CLOUD_VISION_RADIUS_SQUARED);
            if (isCloud && firstSense && secondSense) return areEqualInPropertiesNoClouds(first, second);
            return true;
        }
        return areEqualInPropertiesNoClouds(first, second);
    }

    private static boolean symmetryIteration(int x, int y, int dx, int dy, int sdx, int sdy) throws GameActionException{
        currentLocation = rc.getLocation();
        int curX = x, curY = y;
        MapLocation first = new MapLocation(curX, curY), second = new MapLocation(curX + sdx, curY + sdy);
        int visionRadiusSquared = (rc.senseCloud(currentLocation) ? GameConstants.CLOUD_VISION_RADIUS_SQUARED : UNIT_TYPE.visionRadiusSquared);
        while(first.isWithinDistanceSquared(currentLocation, visionRadiusSquared) && second.isWithinDistanceSquared(currentLocation, visionRadiusSquared)){
            if (!rc.onTheMap(first) || !rc.onTheMap(second)) break;
            if (!areEqualInProperties(first, second))
                return false;
            curX += dx;
            curY += dy;
            first = new MapLocation(curX, curY);
            second = new MapLocation(curX + sdx, curY + sdy);
        }
        return true;
    }

    private static boolean checkVerticalSymmetry() throws GameActionException{
        currentLocation = rc.getLocation();
        int mid = MAP_WIDTH / 2;
        MapLocation left = new MapLocation(mid - 1, currentLocation.y), right;
        int visionRadiusSquared = UNIT_TYPE.visionRadiusSquared;
        int sdx = 1;
        if (rc.senseCloud(currentLocation)) visionRadiusSquared = GameConstants.CLOUD_VISION_RADIUS_SQUARED;
        switch(MAP_WIDTH % 2){
            case 0: right = new MapLocation(mid, currentLocation.y); break;
            case 1: right = new MapLocation(mid + 1, currentLocation.y); sdx = 2; break;
            default : right = left; assert false;
        }
        if (!left.isWithinDistanceSquared(currentLocation, visionRadiusSquared) || !right.isWithinDistanceSquared(currentLocation, visionRadiusSquared))
            return true;
        return symmetryIteration(left.x, left.y, 0, 1, sdx, 0) && symmetryIteration(left.x, left.y, 0, -1, sdx, 0);
    }

    private static boolean checkRotationalSymmetry() throws GameActionException{
        currentLocation = rc.getLocation();
        // switch(MAP_WIDTH % 2){
        //     case 0:
        //     case 1:
        //     default : assert false;
        // }
        return true;
    }

    private static boolean checkHorizontalSymmetry() throws GameActionException{
        currentLocation = rc.getLocation();
        int mid = MAP_HEIGHT / 2;
        MapLocation down = new MapLocation(currentLocation.x, mid - 1), up;
        int visionRadiusSquared = UNIT_TYPE.visionRadiusSquared;
        int sdy = 1;
        if (rc.senseCloud(currentLocation)) visionRadiusSquared = GameConstants.CLOUD_VISION_RADIUS_SQUARED;
        switch(MAP_HEIGHT % 2){
            case 0: up = new MapLocation(currentLocation.x, mid); break;
            case 1: up = new MapLocation(currentLocation.x, mid + 1); sdy = 2; break;
            default : up = down; assert false;
        }
        if (!down.isWithinDistanceSquared(currentLocation, visionRadiusSquared) || !up.isWithinDistanceSquared(currentLocation, visionRadiusSquared))
            return true;
        return symmetryIteration(up.x, up.y, 1, 0, 0, sdy) && symmetryIteration(down.x, down.y, -1, 0, 0, sdy);
    }

    /**
     * Returns whether the given symmetry is valid or invalid or cannot be determined yet
     * @param sym
     * @return 0 if invalid, 1 if valid, 2 if cannot be determined yet
     * @throws GameActionException
     */
    public static boolean checkThisSymmetry(SYMMETRY sym) throws GameActionException{
        switch(sym){
            case HORIZONTAL: return checkHorizontalSymmetry();
            case VERTICAL: return checkVerticalSymmetry();
            case ROTATIONAL: return checkRotationalSymmetry();
            default:
                break;
        }
        assert false;
        return true;
    }

    public static MapLocation returnEnemyOnSymmetry(Symmetry.SYMMETRY mapSym, MapLocation allyHQ) throws GameActionException{
        if (!checkIfSymmetry(mapSym)){
            return null;
        }
        switch(mapSym) {
            case VERTICAL:
                return new MapLocation(MAP_WIDTH - allyHQ.x - 1, allyHQ.y);
            case HORIZONTAL:
                return new MapLocation(allyHQ.x, MAP_HEIGHT - allyHQ.y - 1); // Intentional bug
            case ROTATIONAL:
                return new MapLocation(MAP_WIDTH - allyHQ.x - 1, MAP_HEIGHT - allyHQ.y - 1);
            default:
                return null;
        }
    }

    public static void guessEnemyHQLocation() throws GameActionException {
        rememberedEnemyHQLocations[0] = returnEnemyOnSymmetry(Symmetry.SYMMETRY.values()[0], parentHQLocation);
        rememberedEnemyHQLocations[1] = returnEnemyOnSymmetry(Symmetry.SYMMETRY.values()[1], parentHQLocation);
        rememberedEnemyHQLocations[2] = returnEnemyOnSymmetry(Symmetry.SYMMETRY.values()[2], parentHQLocation);
        for (int i = Symmetry.SYMMETRY.values().length; --i >= 0;) {
            if (checkIfSymmetry(Symmetry.SYMMETRY.values()[i]) && !Symmetry.checkThisSymmetry(SYMMETRY.values()[i])){
                removeSymmetry(SYMMETRY.values()[i], "3");
                mapSymmetry[i] = false;
                rememberedEnemyHQLocations[i] = null;
            }
            else if (!checkIfSymmetry(Symmetry.SYMMETRY.values()[i])){
                mapSymmetry[i] = false;
                rememberedEnemyHQLocations[i] = null;
            } 
        }
        MapLocation[] alliedHQs = Comms.getAlliedHeadquartersLocationsList();
        MapLocation[] enemyHQs = Comms.getEnemyHeadquartersLocationsList();
        for (int i = alliedHQs.length; --i >= 0;) {
            if (alliedHQs[i] == null) continue;
            for (int j = rememberedEnemyHQLocations.length; --j >= 0;) {         
                if (rememberedEnemyHQLocations[j] == null) continue;
                int hqDistance = alliedHQs[i].distanceSquaredTo(rememberedEnemyHQLocations[j]);
                if (hqDistance <= RobotType.HEADQUARTERS.visionRadiusSquared) {
                    boolean flag = true;
                    for (int k = enemyHQs.length; --k >= 0;) {
                        if (enemyHQs[k] == null) continue;
                        if (enemyHQs[k].equals(rememberedEnemyHQLocations[j])){
                            flag = false;
                        }
                    }
                    if (flag){
                        if (checkIfSymmetry(Symmetry.SYMMETRY.values()[j])){
                            removeSymmetry(Symmetry.SYMMETRY.values()[j], "2");
                        }
                        rememberedEnemyHQLocations[j] = null;
                        mapSymmetry[j] = false;
                    }
                }
            }
        }
    }

    public static MapLocation returnEnemyHQGuess() throws GameActionException{
        MapLocation guessDestination = CENTER_OF_THE_MAP;
        if (rc.getRoundNum() > 1){
            guessDestination = Comms.findNearestEnemyHeadquarterLocation();
        }
        if (guessDestination.equals(CENTER_OF_THE_MAP)){
            guessDestination = defaultEnemyLocation();
        }
        return guessDestination;
    }

    private static boolean isBottomLeftCorner(MapLocation loc){
        return loc.x <= MAP_WIDTH / 5 && loc.y <= MAP_HEIGHT / 5;
    }

    private static boolean isBottomRightCorner(MapLocation loc){
        return loc.x >= MAP_WIDTH - MAP_WIDTH / 5 && loc.y <= MAP_HEIGHT / 5;
    }

    private static boolean isTopLeftCorner(MapLocation loc){
        return loc.x <= MAP_WIDTH / 5 && loc.y >= MAP_HEIGHT - MAP_HEIGHT / 5;
    }

    private static boolean isTopRightCorner(MapLocation loc){
        return loc.x >= MAP_WIDTH - MAP_WIDTH / 5 && loc.y >= MAP_HEIGHT - MAP_HEIGHT / 5;
    }

    public static boolean areHQsCornered() throws GameActionException{
        int hqCount = Comms.getHeadquartersCount();
        MapLocation[] alliedHQs = Comms.getAlliedHeadquartersLocationsList();
        boolean topLeft = false, topRight = false, bottomLeft = false, bottomRight = false;
        MapLocation loc;
        for (int i = hqCount; i-- > 0;) {
            loc = alliedHQs[i];
            // if (UNIT_TYPE == RobotType.LAUNCHER && rc.getRoundNum() <= BIRTH_ROUND + 1) System.out.println("HQ " + i + " " + loc);
            if (loc == null || loc.x == -1) continue;
            if (isBottomLeftCorner(loc)) bottomLeft = true;
            else if (isBottomRightCorner(loc)) bottomRight = true;
            else if (isTopLeftCorner(loc)) topLeft = true;
            else if (isTopRightCorner(loc)) topRight = true;
        }
        // if (UNIT_TYPE == RobotType.LAUNCHER && rc.getRoundNum() <= BIRTH_ROUND + 1){
        //     System.out.println("TOP LEFT: " + topLeft);
        //     System.out.println("TOP RIGHT: " + topRight);
        //     System.out.println("BOTTOM LEFT: " + bottomLeft);
        //     System.out.println("BOTTOM RIGHT: " + bottomRight);
        // }
        if (bottomLeft && bottomRight) return true;
        if (bottomLeft && topLeft) return true;
        if (bottomRight && topRight) return true;
        if (topLeft && topRight) return true;
        return false;
    }

    public static MapLocation defaultEnemyLocation() throws GameActionException{
        if (UNIT_TYPE == RobotType.HEADQUARTERS){
            for (int i = rememberedEnemyHQLocations.length; --i >= 0;) {
                if (rememberedEnemyHQLocations[i] != null){
                    if (checkIfSymmetry(Symmetry.SYMMETRY.values()[i])){
                        return rememberedEnemyHQLocations[i];
                    }
                    else{
                        rememberedEnemyHQLocations[i] = null;
                        mapSymmetry[i] = false;
                    }
                }
            }
        }
        else {
            double factor = 1;
            int[] store; 
            if (MAP_SIZE < 1000)
                store = new int[] {1,0,2};
            else
                store = new int[] {1,2,0};
            for (int i : store) {
                if (checkIfSymmetry(Symmetry.SYMMETRY.values()[i]) && mapSymmetry[i]){
                    MapLocation closestEnemyHQ = returnEnemyOnSymmetry(Symmetry.SYMMETRY.values()[i],parentHQLocation); // Default go to location
                    if (closestEnemyHQ == null) continue;
                    double minDistance = (double) parentHQLocation.distanceSquaredTo(closestEnemyHQ);
                    for (int j = alliedHQLocs.length; --j >= 0;) {
                        if (alliedHQLocs[j] == null) continue;
                        MapLocation alliedHQEnemyHQ = returnEnemyOnSymmetry(Symmetry.SYMMETRY.values()[i], alliedHQLocs[j]);
                        if (alliedHQEnemyHQ == null) continue;
                        double parentDistance = (double) parentHQLocation.distanceSquaredTo(alliedHQEnemyHQ);
                        // double currDistance = (double) rc.getLocation().distanceSquaredTo(alliedHQEnemyHQ); // As distance will be updated
                        if (parentDistance > alliedHQLocs[j].distanceSquaredTo(alliedHQEnemyHQ)) continue; // Don't have all launchers go to the same place
                        if (parentDistance <= 1.0) continue;
                        MapLocation alreadyVisitedHQ = visitedHQList[visitedHQIndex % Comms.getHeadquartersCount()];
                        if (alreadyVisitedHQ != null && alreadyVisitedHQ.equals(alliedHQEnemyHQ)) continue;
                        if (parentDistance * factor < minDistance){
                            minDistance = parentDistance;
                            closestEnemyHQ = alliedHQEnemyHQ;
                        }
                    }
                    if (closestEnemyHQ != null){
                        return closestEnemyHQ;
                    }
                }
            }
        }
        return CENTER_OF_THE_MAP;
    }

    public static MapLocation defaultEnemyLocationWithExclusion(MapLocation excludeHQ) throws GameActionException{
        double factor = 2;
        int[] store = new int[] {2,1,0};
        if (MAP_SIZE > 2500 || areHQsCornered()){
            store = new int[] {0,1,2};
        }
        else if (Math.max(MAP_HEIGHT, MAP_WIDTH) >= 1.5 * Math.min(MAP_HEIGHT, MAP_WIDTH)){
            store = new int[] {1,2,0};
            factor = 1;
        }
        for (int i : store) {
            if (checkIfSymmetry(Symmetry.SYMMETRY.values()[i])){
                MapLocation closestEnemyHQ = returnEnemyOnSymmetry(Symmetry.SYMMETRY.values()[i],parentHQLocation);
                if (closestEnemyHQ == null) continue;
                double minDistance = (double) parentHQLocation.distanceSquaredTo(closestEnemyHQ);
                for (int j = alliedHQLocs.length; --j >= 0;) {
                    if (alliedHQLocs[j] == null) continue;
                    MapLocation alliedHQEnemyHQ = returnEnemyOnSymmetry(Symmetry.SYMMETRY.values()[i], alliedHQLocs[j]);
                    if (alliedHQEnemyHQ == null) continue;
                    if (Comms.getHeadquartersCount() > 1 && alliedHQEnemyHQ.equals(excludeHQ)) continue;
                    double currDistance = (double) parentHQLocation.distanceSquaredTo(alliedHQEnemyHQ);
                    if (currDistance <= 1.0) continue;
                    if (currDistance * factor < minDistance){
                        minDistance = currDistance;
                        closestEnemyHQ = alliedHQEnemyHQ;
                    }
                }
                if (closestEnemyHQ != null){
                    return closestEnemyHQ;
                }
            }
        }
        return CENTER_OF_THE_MAP;
    }

    public static boolean checkIfSymmetry(Symmetry.SYMMETRY sym) throws GameActionException{
        return (rc.readSharedArray(Comms.SYMMETRY_CHANNEL) & sym.getSym()) == sym.getSym();
    }

    public static void removeSymmetry(Symmetry.SYMMETRY sym, String s) throws GameActionException{
        int symVal = rc.readSharedArray(Comms.SYMMETRY_CHANNEL);
        if (!checkIfSymmetry(sym))
            return;
        symVal = symVal ^ sym.getSym();
        if (rc.canWriteSharedArray(0, 0))
            rc.writeSharedArray(Comms.SYMMETRY_CHANNEL, symVal);
    }
}