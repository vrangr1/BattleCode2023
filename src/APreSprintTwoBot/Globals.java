package APreSprintTwoBot;

import battlecode.common.*;
import java.util.Random;

import APreSprintTwoBot.path.*;

public class Globals {
    
    public static final int MAX_FUZZY_MOVES = 3;
    public static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };
    public static final Direction NORTH = Direction.NORTH;
    public static final Direction NORTHEAST = Direction.NORTHEAST;
    public static final Direction EAST = Direction.EAST;
    public static final Direction SOUTHEAST = Direction.SOUTHEAST;
    public static final Direction SOUTH = Direction.SOUTH;
    public static final Direction SOUTHWEST = Direction.SOUTHWEST;
    public static final Direction WEST = Direction.WEST;
    public static final Direction NORTHWEST = Direction.NORTHWEST;
    public static final Direction CENTER = Direction.CENTER;
    
    /**
     * A random number generator.
     * We will use this RNG to make some random moves. The Random class is provided by the java.util.Random
     * import at the top of this file. Here, we *seed* the RNG with a constant number (6147); this makes sure
     * we get the same sequence of numbers every time this code is run. This is very useful for debugging!
     */
    static Random rng = new Random(6147);

    public static final int COMMS_VARCOUNT = GameConstants.SHARED_ARRAY_LENGTH;

    public static RobotController rc;
    public static Pathing pathing;
    public static int MAX_WELLS_COUNT; // 144 in the best possible case (60 x 60 map size and full 4% of map is wells)
    public static Direction exploreDir = CENTER;
    public static MapLocation explore3Target;
    public static MapLocation[] headquartersLocations;

    public static int turnCount;
    public static int BIRTH_ROUND;

    public static RobotType UNIT_TYPE;

    public static Team MY_TEAM;

    public static Team ENEMY_TEAM;

    public static int headquartersCount;
    public static int ID;
    public static int myRobotCount;
    public static int myHealth;
    public static MapLocation START_LOCATION;
    public static MapLocation currentLocation;
    public static MapLocation currentDestination = null;
    public static MapLocation parentHeadquarterLocation = null;
    public static MapLocation headquarterLocations[];
    public static MapLocation rememberedEnemyHeadquarterLocations[];
    public static MapLocation[] MAP_CORNERS = new MapLocation[4];
    public static boolean botFreeze;
    public static int turnBroadcastCount;
    public static final int TURN_BROADCAST_LIMIT = 4;

    public static boolean underAttack;
    public static boolean isSafe=false;
    public static boolean hasMoved=false;
    public static int MAX_HEALTH;

    // Map Related
    public static int MAP_WIDTH;
    public static int MAP_HEIGHT;
    public static int MAP_SIZE;
    public static boolean isMapSquare = true;
    public static MapLocation CENTER_OF_THE_MAP;
    public static int ISLAND_COUNT;

    // Bot Production Related
    public static final boolean TRACKING_LAUNCHER_COUNT = true;
    public static final boolean TRACKING_AMPLIFIER_COUNT = false;
    public static int MAX_AMPLIFIER_COUNT;

    // public static final Direction droidVisionDirs[] = new Direction[]{
    //     Direction.NORTHWEST, Direction.NORTHWEST, Direction.NORTH, Direction.NORTH, 
    //     Direction.NORTH, Direction.NORTH, Direction.NORTHEAST, Direction.NORTHEAST, 
    //     Direction.EAST, Direction.EAST, Direction.EAST, Direction.EAST, 
    //     Direction.SOUTHEAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTH, 
    //     Direction.SOUTH, Direction.SOUTH, Direction.SOUTHWEST, Direction.SOUTHWEST, 
    //     Direction.WEST, Direction.WEST, Direction.WEST, Direction.NORTHWEST, 
    //     Direction.NORTHWEST, Direction.NORTH, Direction.NORTH, Direction.NORTH, 
    //     Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.EAST, 
    //     Direction.EAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, 
    //     Direction.SOUTH, Direction.SOUTH, Direction.SOUTH, Direction.SOUTHWEST, 
    //     Direction.WEST, Direction.WEST, Direction.WEST, Direction.NORTHWEST, 
    //     Direction.NORTH, Direction.NORTH, Direction.NORTH, Direction.NORTH, 
    //     Direction.EAST, Direction.EAST, Direction.EAST, Direction.EAST, 
    //     Direction.SOUTH, Direction.SOUTH, Direction.SOUTH, Direction.SOUTH, 
    //     Direction.WEST, Direction.WEST, Direction.WEST, Direction.NORTH, 
    //     Direction.NORTH, Direction.NORTH, Direction.EAST, Direction.EAST,
    //     Direction.SOUTH, Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.CENTER};

    public static int bytecodeCounter = 0;
    public static MapLocation parentHQLocation = null;
    public static MapLocation rememberedEnemyHQLocations[];
    public static final boolean END_EARLY = false;
    public static final int END_EARLY_ROUND_NUM = 1000;
    public static boolean[] mapSymmetry = {true, true, true}; // {Vertical, Horizontal, Rotational}
    public static MapLocation[] alliedHQLocs;
    public static RobotInfo shepherdUnit;
    public static String destinationFlag = "";

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

    public static void initGlobals(RobotController rc1) throws GameActionException{
        rc = rc1;
        pathing = new Pathing();
        MAX_WELLS_COUNT = (int)(GameConstants.MAX_MAP_PERCENT_WELLS * ((float)(rc.getMapHeight() * rc.getMapWidth())));

        BIRTH_ROUND = rc.getRoundNum();
        turnCount = BIRTH_ROUND;
        UNIT_TYPE = rc.getType();
        MAX_HEALTH = UNIT_TYPE.getMaxHealth();
        MAP_WIDTH = rc.getMapWidth();
        MAP_HEIGHT = rc.getMapHeight();
        MAP_SIZE = MAP_WIDTH * MAP_HEIGHT;
        MAX_AMPLIFIER_COUNT = MAP_SIZE / 50;
        MY_TEAM = rc.getTeam();
        ENEMY_TEAM = MY_TEAM.opponent();
        START_LOCATION = rc.getLocation();
        ID = rc.getID();

        MAP_CORNERS[0] = new MapLocation(0,0);
        MAP_CORNERS[1] = new MapLocation(MAP_WIDTH - 1, 0);
        MAP_CORNERS[2] = new MapLocation(0, MAP_HEIGHT - 1);
        MAP_CORNERS[3] = new MapLocation(MAP_WIDTH - 1, MAP_HEIGHT - 1);

        currentLocation = START_LOCATION;
        botFreeze = false;
        myHealth = rc.getHealth();
        underAttack = false;
        CENTER_OF_THE_MAP = new MapLocation(MAP_WIDTH/2, MAP_HEIGHT/2);
        ISLAND_COUNT = rc.getIslandCount();
        Comms.initCommunicationsArray();
        getParentHQLocation();
        rememberedEnemyHQLocations = new MapLocation[3];
        if (UNIT_TYPE != RobotType.HEADQUARTERS){
            alliedHQLocs = Comms.getAlliedHeadquartersLocationsList();
            guessEnemyHQLocation();
        }
        shepherdUnit = null;
    }

    public static void getParentHQLocation() throws GameActionException{
        if (UNIT_TYPE == RobotType.HEADQUARTERS) 
            parentHQLocation = rc.getLocation();
        else if (rc.getRoundNum() > 1) {
            parentHQLocation = Comms.findNearestHeadquarter();
        }
        if (parentHQLocation == null) {
            parentHQLocation = new MapLocation(0,0);
        }
    }

    public static void updateGlobals() throws GameActionException{
        currentLocation = rc.getLocation();
        bytecodeCounter = 0;
        MAX_HEALTH = UNIT_TYPE.getMaxHealth();
        int curRound = rc.getRoundNum();

        if (curRound != turnCount + 1 && curRound != BIRTH_ROUND) {
            botFreeze = true;
            System.out.println("Birth round " + BIRTH_ROUND + " Current Round "+ curRound + " Turn Count " + turnCount + " Bot Froze"); 
        }
        turnCount = curRound;

        int curHealth = rc.getHealth();
        if (curHealth < myHealth) underAttack = true;
        else underAttack = false;
        myHealth = curHealth;
    }

    public static MapLocation returnEnemyOnSymmetry(SYMMETRY mapSym, MapLocation allyHQ) {
        switch(mapSym) {
            case VERTICAL:
                return new MapLocation(MAP_WIDTH - allyHQ.x - 1, allyHQ.y);
            case HORIZONTAL:
                return new MapLocation(allyHQ.x - 1, MAP_HEIGHT - allyHQ.y - 1); // Intentional bug
            case ROTATIONAL:
                return new MapLocation(MAP_WIDTH - allyHQ.x - 1, MAP_HEIGHT - allyHQ.y - 1);
            default:
                return null;
        }
    }

    public static void guessEnemyHQLocation() throws GameActionException {
        rememberedEnemyHQLocations[0] = returnEnemyOnSymmetry(SYMMETRY.values()[0], parentHQLocation);
        rememberedEnemyHQLocations[1] = returnEnemyOnSymmetry(SYMMETRY.values()[1], parentHQLocation);
        rememberedEnemyHQLocations[2] = returnEnemyOnSymmetry(SYMMETRY.values()[2], parentHQLocation);
        for (int i = SYMMETRY.values().length; --i >= 0;) {
            if (!checkIfSymmetry(SYMMETRY.values()[i])){
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
                        if (checkIfSymmetry(SYMMETRY.values()[j])){
                            removeSymmetry(SYMMETRY.values()[j], "2");
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
                    if (checkIfSymmetry(SYMMETRY.values()[i])){
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
                if (checkIfSymmetry(SYMMETRY.values()[i])){
                    MapLocation closestEnemyHQ = returnEnemyOnSymmetry(SYMMETRY.values()[i],parentHQLocation);
                    double minDistance = (double) parentHQLocation.distanceSquaredTo(closestEnemyHQ);
                    for (int j = alliedHQLocs.length; --j >= 0;) {
                        double currDistance = (double) parentHQLocation.distanceSquaredTo(returnEnemyOnSymmetry(SYMMETRY.values()[i], alliedHQLocs[j]));
                        if (currDistance <= 1.0) continue;
                        if (currDistance * factor < minDistance){
                            minDistance = currDistance;
                            closestEnemyHQ = returnEnemyOnSymmetry(SYMMETRY.values()[i], alliedHQLocs[j]);
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

    public static boolean checkIfSymmetry(SYMMETRY sym) throws GameActionException{
        return (rc.readSharedArray(Comms.SYMMETRY_CHANNEL) & sym.getSym()) == sym.getSym();
    }

    public static void removeSymmetry(SYMMETRY sym, String s) throws GameActionException{
        int symVal = rc.readSharedArray(Comms.SYMMETRY_CHANNEL);
        if (!checkIfSymmetry(sym))
            return;
        symVal = symVal ^ sym.getSym();
        if (rc.canWriteSharedArray(0, 0))
            rc.writeSharedArray(Comms.SYMMETRY_CHANNEL, symVal);
    }
}
