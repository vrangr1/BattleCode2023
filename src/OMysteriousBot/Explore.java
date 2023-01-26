package OMysteriousBot;

import battlecode.common.*;


class BoundInfo {
    int value;
    boolean known;
    BoundInfo(int value, boolean known){
        this.value = value;
        this.known = known;
    }

    public static Integer getBound(BoundInfo b){
        if (b == null) return null;
        return b.value;
    }
}


public class Explore extends Utils{

    public static double angle;
    private static final int exploreDist = 100;
    public static final BoundInfo lbX = new BoundInfo(0, true), lbY = new BoundInfo(0, true);
    public static final BoundInfo ubX = new BoundInfo(Globals.rc.getMapWidth(), true), ubY = new BoundInfo(Globals.rc.getMapHeight(), true);


    public static int eastCloser(){
        // return 
        Integer ux = BoundInfo.getBound(ubX), lx = BoundInfo.getBound(lbX);
        if (ux == null || lx == null) return 0;
        return  ux - rc.getLocation().x <= rc.getLocation().x - lx ?  1 : -1;
    }


    public static int northCloser(){
        Integer uy = BoundInfo.getBound(ubY), ly = BoundInfo.getBound(lbY);
        if (uy == null || ly == null) return 0;
        return  uy - rc.getLocation().y <= rc.getLocation().y - ly ? 1 : -1;
    }


    public static boolean movingOutOfMap(Direction dir){
        try {
            MapLocation loc = rc.getLocation().add(dir);
            // System.out.println("loc1: " + loc);
            if (!rc.onTheMap(loc)) {
                return true;
            }
            loc = loc.add(dir);
            // System.out.println("loc2: " + loc);
            if (!rc.onTheMap(loc)) {
                return true;
            }
            loc = loc.add(dir);
            // System.out.println("loc3: " + loc);
            if (!rc.onTheMap(loc)) {
                return true;
            }
            loc = loc.add(dir);
            if (!isValidMapLocation(loc)) {
                return true;
            }
            // loc = loc.add(dir);
            // // System.out.println("loc4: " + loc);
            // if (!isValidMapLocation(loc)) {
            //     return true;
            // }
        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    public static void assignExplore3Dir(Direction dir){
        exploreDir = dir;
        angle = Math.atan2(exploreDir.dy, exploreDir.dx);
        double x = rc.getLocation().x, y = rc.getLocation().y;
        x += Math.cos(angle)*exploreDist;
        y += Math.sin(angle)*exploreDist;
        explore3Target = new MapLocation((int)x, (int)y);
    }


    public static void getClosestExplore3Direction(){
        Direction dirl = exploreDir.rotateLeft();
        if (!movingOutOfMap(dirl)){
            assignExplore3Dir(dirl);
            return;
        }
        Direction dirr = exploreDir.rotateRight();
        if (!movingOutOfMap(dirr)){
            assignExplore3Dir(dirr);
            return;
        }
        Direction dirll = dirl.rotateLeft();
        if (!movingOutOfMap(dirll)){
            assignExplore3Dir(dirll);
            return;
        }
        Direction dirrr = dirr.rotateRight();
        if (!movingOutOfMap(dirrr)){
            assignExplore3Dir(dirrr);
            return;
        }
        Direction dirlll = dirll.rotateLeft();
        if (!movingOutOfMap(dirlll)){
            assignExplore3Dir(dirlll);
            return;
        }
        Direction dirrrr = dirrr.rotateRight();
        if (!movingOutOfMap(dirrrr)){
            assignExplore3Dir(dirrrr);
            return;
        }
        Direction dirllll = dirlll.rotateLeft();
        if (!movingOutOfMap(dirllll)){
            assignExplore3Dir(dirllll);
            return;
        }
    }


    public static void checkDirection(){
        //Direction actualDir = rc.getLocation().directionTo(explore3Target);
        if (!movingOutOfMap(exploreDir)) return;
        //System.err.println("Checking new direction!");
        switch(exploreDir){
            case SOUTHEAST:
            case NORTHEAST:
            case NORTHWEST:
            case SOUTHWEST:
                getClosestExplore3Direction();
                return;
            case NORTH:
            case SOUTH:
                int east = eastCloser();
                switch(east){
                    case 1:
                        assignExplore3Dir(Direction.WEST);
                        return;
                    case -1:
                        assignExplore3Dir(Direction.EAST);
                        return;
                }
                Direction dir = exploreDir.rotateLeft().rotateLeft();
                // Direction dir = exploreDir.rotateLeft().rotateLeft().rotateLeft();
                if (!movingOutOfMap(dir)) {
                    assignExplore3Dir(dir);
                }
                else {
                    assignExplore3Dir(dir.opposite());
                }
                return;
            case EAST:
            case WEST:
                int north = northCloser();
                switch(north){
                    case 1:
                        assignExplore3Dir(Direction.SOUTH);
                        return;
                    case -1:
                        assignExplore3Dir(Direction.NORTH);
                        return;
                }
                dir = exploreDir.rotateLeft().rotateLeft();
                // dir = exploreDir.rotateLeft().rotateLeft().rotateLeft();
                if (!movingOutOfMap(dir)) assignExplore3Dir(dir);
                else assignExplore3Dir(dir.opposite());
                return;
            case CENTER: return;
        }
    }

    public static MapLocation getExplore3Target(){
        checkDirection();
        return explore3Target;
    }

    public static void getExploreDir(boolean randomExploration) throws GameActionException{
        if (randomExploration){
            assignExplore3Dir(directions[rng.nextInt(2311) % 8]);
            return;
        }
        Direction away = directionAwayFromAllRobots();
        if (away != null){
            assignExplore3Dir(away);
            return;
        }
        MapLocation closestHQ = Comms.findNearestHeadquarter();
        if (rc.canSenseLocation(closestHQ)) 
            assignExplore3Dir(closestHQ.directionTo(currentLocation));
        else
            assignExplore3Dir(directions[Globals.rng.nextInt(8)]);
    }

    public static MapLocation explore(boolean randomExploration) throws GameActionException{
        if (exploreDir == CENTER)
            getExploreDir(randomExploration);
        return getExplore3Target();
    }

    public static void getExploreDir() throws GameActionException{
        Direction away = directionAwayFromAllRobots();
        if (away != null){
            assignExplore3Dir(away);
            return;
        }
        MapLocation closestHQ = Comms.findNearestHeadquarter();
        if (rc.canSenseLocation(closestHQ)) 
            assignExplore3Dir(closestHQ.directionTo(currentLocation));
        else
            assignExplore3Dir(directions[Globals.rng.nextInt(8)]);
    }

    public static MapLocation explore() throws GameActionException{
        if (exploreDir == CENTER)
            getExploreDir();
        return getExplore3Target();
    }
}