package OHardlyWorkingBot;

import battlecode.common.*;

public class Movement extends Utils{
    
    public static boolean tryMoveInDirection(MapLocation dest) throws GameActionException {
        try{
            if (dest == null) return false;
            if(!rc.isMovementReady()) return false;
            MapLocation lCR = rc.getLocation();
            if (lCR.equals(dest)) return false;
            Direction forward = lCR.directionTo(dest);
            MapLocation dirLoc = null;
            Direction[] dirs;
            if (preferLeft(dest)) {
                dirs = new Direction[] { forward, forward.rotateLeft(), forward.rotateRight()};         
            } else {
                dirs = new Direction[] { forward, forward.rotateRight(), forward.rotateLeft()};
            }
            Direction bestDir = null;
            double bestRubble = 0.0; //rc.senseRubble(rc.getLocation());
            int currentDistSq = lCR.distanceSquaredTo(dest);
            for (Direction direction : dirs) {
                dirLoc = lCR.add(direction);
                if (!rc.onTheMap(dirLoc)) continue; // The location will always be in vision
                if (bestDir != null && dirLoc.distanceSquaredTo(dest) > currentDistSq) continue;
                double rubble = 0.0; // rc.senseRubble(dirLoc);
                if ((rubble < bestRubble || rubble == 0) && rc.canMove(direction)) {
                    bestRubble = rubble;
                    bestDir = direction;
                }
            }
        
            if (bestDir != null) {
                rc.move(bestDir);
                currentLocation = rc.getLocation();
                return true;
            }
            return false;
        }
        catch (Exception e) {
            System.out.println("Exception in tryMoveInDirection: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static boolean tryForcedMoveInDirection(MapLocation dest) throws GameActionException {
        try{
            if(!rc.isMovementReady()) return false;
            MapLocation lCR = rc.getLocation();
            if (lCR.equals(dest)) return false;
            Direction forward = lCR.directionTo(dest);
            MapLocation dirLoc = null;
            Direction[] dirs;
            if (preferLeft(dest)) {
                dirs = new Direction[] { forward, forward.rotateLeft(), forward.rotateRight()};         
            } else {
                dirs = new Direction[] { forward, forward.rotateRight(), forward.rotateLeft()};
            }
            Direction bestDir = null;
            double bestRubble = 0.0; // rc.senseRubble(rc.getLocation());
            int currentDistSq = lCR.distanceSquaredTo(dest);
            for (Direction direction : dirs) {
                dirLoc = lCR.add(direction);
                if (!rc.onTheMap(dirLoc)) continue; // The location will always be in vision
                if (bestDir != null && dirLoc.distanceSquaredTo(dest) > currentDistSq) continue;
                double rubble = 0.0; // rc.senseRubble(dirLoc);
                if ((rubble <= bestRubble || rubble == 0) && rc.canMove(direction)) {
                    bestRubble = rubble;
                    bestDir = direction;
                }
            }
        
            if (bestDir != null) {
                rc.move(bestDir);
                currentLocation = rc.getLocation();
                return true;
            }
            return false;
        }
        catch (Exception e) {
            System.out.println("Exception in tryMoveInDirection: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Takes around 400 bytecodes to run 
    public static boolean goToDirect(MapLocation dest) throws GameActionException {
        try{
            MapLocation lCR = rc.getLocation();
            if(!rc.isMovementReady()) return false;
            if (lCR.equals(dest)) return false;
            Direction forward = lCR.directionTo(dest);
            if (lCR.isAdjacentTo(dest)) {
                if (rc.canMove(forward)) {
                    rc.move(forward);
                    currentLocation = rc.getLocation();
                    return true;
                }
            }   
            
            Direction[] dirs;
            if (preferLeft(dest)) {
                dirs = new Direction[] { forward, forward.rotateLeft(), forward.rotateRight(),
                        forward.rotateLeft().rotateLeft(), forward.rotateRight().rotateRight()};            
            } else {
                dirs = new Direction[] { forward, forward.rotateRight(), forward.rotateLeft(), 
                        forward.rotateRight().rotateRight(), forward.rotateLeft().rotateLeft()};
            }
        
            Direction bestDir = null;
            double bestRubble = 0.0; // MAX_RUBBLE+1;
            int currentDistSq = lCR.distanceSquaredTo(dest);
            for (Direction dir : dirs) {
                MapLocation dirLoc = lCR.add(dir);
                if (!rc.onTheMap(dirLoc)) continue; // The 5 directions around you are in vision
                if (bestDir!= null && dirLoc.distanceSquaredTo(dest) > currentDistSq) continue;
                double rubble = 0.0; //rc.senseRubble(dirLoc);
                if (rubble < bestRubble && rc.canMove(dir)) {
                    bestRubble = rubble;
                    bestDir = dir;
                }
            }
        
            if (bestDir != null) {
                rc.move(bestDir);
                currentLocation = rc.getLocation();
                return true;
            }
            return false;
        }
        catch (Exception e) {
            System.out.println("Exception in goToDirect: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static boolean moveToDest(MapLocation dest) throws GameActionException{
        return goToDirect(dest);
    }
    
    public static boolean preferLeft(MapLocation dest) {
        Direction toDest = rc.getLocation().directionTo(dest);
        MapLocation leftLoc = rc.getLocation().add(toDest.rotateLeft());
        MapLocation rightLoc = rc.getLocation().add(toDest.rotateRight());
        if (dest.distanceSquaredTo(leftLoc) == dest.distanceSquaredTo(rightLoc)) return closerToCenter(leftLoc, rightLoc);
        return (dest.distanceSquaredTo(leftLoc) < dest.distanceSquaredTo(rightLoc)); // Team preference
    }


    public static void moveRandomly() throws GameActionException {
        Direction dir = Globals.directions[Globals.rng.nextInt(Globals.directions.length)];
        if (rc.canMove(dir)) {
            rc.move(dir);
            currentLocation = rc.getLocation();
        }
    }

    
    public static MapLocation moveToLattice(int minLatticeDist, int weights){
        try { 
            MapLocation lCurrentLocation = rc.getLocation();
            MapLocation lArchonLocation = Comms.findNearestHeadquarter();
            MapLocation bestLoc = null;
            MapLocation myLoc = rc.getLocation();

            int bestDist = 0;
            // int byteCodeSaver=0;
            if (lArchonLocation == null) return null;
            int congruence = (lArchonLocation.x + lArchonLocation.y + 1) % 2;

            if ((myLoc.x + myLoc.y)%2 == congruence && myLoc.distanceSquaredTo(lArchonLocation) >= minLatticeDist + weights){
                bestDist = myLoc.distanceSquaredTo(lArchonLocation);
                bestLoc = myLoc;
                // return bestLoc;
            }

            for (int i = droidVisionDirs.length; i-- > 0; ) { //TODO: Add a bytecode check
                if (Clock.getBytecodesLeft() < 2000) return bestLoc;
                lCurrentLocation = lCurrentLocation.add(droidVisionDirs[i]);
                if ((lCurrentLocation.x + lCurrentLocation.y) % 2 != congruence) continue;
                if (!rc.canSenseLocation(lCurrentLocation)) continue;
                if (!rc.onTheMap(lCurrentLocation)) continue;
                if (rc.canSenseRobotAtLocation(lCurrentLocation)) continue;
                if (UNIT_TYPE == RobotType.CARRIER && rc.senseWell(lCurrentLocation) == null) continue;

                int estimatedDistance = lCurrentLocation.distanceSquaredTo(lArchonLocation);

                if (estimatedDistance < minLatticeDist + weights) continue;

                if (bestLoc == null  || estimatedDistance < bestDist){
                    bestLoc = lCurrentLocation;
                    bestDist = estimatedDistance;
                }
            }
            if (bestLoc != null){
                return bestLoc;
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
    
    public static boolean retreatIfNecessary(RobotInfo[] visibleFriends, RobotInfo[] visibleHostiles) throws GameActionException {
        if (visibleHostiles.length == 0 //|| visibleFriends.length >= visibleHostiles.length
        ) return false;
        
        boolean mustRetreat = false;
        int bestClosestDistSq = Integer.MAX_VALUE;
        MapLocation lCR = rc.getLocation();
        for (RobotInfo hostile : visibleHostiles) {         
            //Sage killing one miner is worth for sage action cooldown
            if (hostile.type.canAttack()) {
                int distSq = lCR.distanceSquaredTo(hostile.location);
                if (distSq <= hostile.type.actionRadiusSquared) {
                    mustRetreat = true;
                    if (distSq < bestClosestDistSq) {
                        bestClosestDistSq = distSq;
                    }
                }
            }
        }
        if (!mustRetreat) return false;
        
        Direction bestDir = null;
        Direction[] dirs = Direction.values();
        for (int i = dirs.length; i--> 0;) {
            Direction dir = dirs[i];
            if (!rc.canMove(dir)) continue;
            MapLocation dirLoc = lCR.add(dir);
            int dirClosestDistSq = Integer.MAX_VALUE;
            for (RobotInfo hostile : visibleHostiles) {
                if (hostile.type.canAttack()) {
                    int distSq = dirLoc.distanceSquaredTo(hostile.location);
                    if (distSq < dirClosestDistSq) {
                        dirClosestDistSq = distSq;                      
                        if (dirClosestDistSq <= bestClosestDistSq) break;
                    }
                }
            }
            if (dirClosestDistSq > bestClosestDistSq) {
                bestClosestDistSq = dirClosestDistSq;
                bestDir = dir;
            }
        }
        
        if (bestDir != null) {
            rc.move(bestDir);
            currentLocation = rc.getLocation();
            return true;
        }
        return false;
    }

    
    /**
     * Moves away from closest archon when passed null.
     */
    public static boolean moveAwayFromLocation(MapLocation loc){
        try{
            if (loc == null) loc = Comms.findNearestHeadquarter();
            if (loc == null) return false;
            Direction dir = loc.directionTo(rc.getLocation());
            MapLocation ret = rc.getLocation();
            ret = ret.translate(dir.dx, dir.dy);
            ret = ret.translate(dir.dx, dir.dy);
            ret = ret.translate(dir.dx, dir.dy);
            ret = ret.translate(dir.dx, dir.dy);
            ret = ret.translate(dir.dx, dir.dy);
            return goToDirect(ret);
        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

}
