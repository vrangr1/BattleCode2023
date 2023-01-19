package OPostPatchBot;

import battlecode.common.*;
import java.util.Random;

import OPostPatchBot.path.*;

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
        rememberedEnemyHQLocations = new MapLocation[4];
        if (UNIT_TYPE != RobotType.HEADQUARTERS){
            guessEnemyHQLocation();
        }
    }

    public static void getParentHQLocation() throws GameActionException{
        if (UNIT_TYPE == RobotType.HEADQUARTERS) 
            parentHQLocation = rc.getLocation();
        else if (rc.getRoundNum() > 1) {
            parentHQLocation = Comms.findNearestHeadquarter();
        }
        if (parentHQLocation == null) {
            parentHQLocation =  new MapLocation(0,0);
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

    public static void guessEnemyHQLocation() throws GameActionException {
        if (MAP_WIDTH - parentHQLocation.x >= 0){
            rememberedEnemyHQLocations[0] = new MapLocation(MAP_WIDTH - parentHQLocation.x, parentHQLocation.y);
        }
        if (MAP_HEIGHT - parentHQLocation.y >= 0){
            rememberedEnemyHQLocations[1] = new MapLocation(parentHQLocation.x, MAP_HEIGHT - parentHQLocation.y);
        }
        if (MAP_WIDTH - parentHQLocation.x >= 0 && MAP_HEIGHT - parentHQLocation.y >= 0){
            rememberedEnemyHQLocations[2] = new MapLocation(MAP_WIDTH - parentHQLocation.x, MAP_HEIGHT - parentHQLocation.y);
        }
        MapLocation[] alliedHQs = Comms.getAlliedHeadquartersLocationsList();
        for (int i = alliedHQs.length; --i >= 0;) {
            if (alliedHQs[i] == null) continue;
            for (int j = rememberedEnemyHQLocations.length; --j >= 0;) {         
                if (rememberedEnemyHQLocations[j] == null) continue;
                int hqDistance = alliedHQs[i].distanceSquaredTo(rememberedEnemyHQLocations[j]);
                if (hqDistance > RobotType.HEADQUARTERS.visionRadiusSquared) continue;
                else{
                    rememberedEnemyHQLocations[j] = null;
                }
            }
        }
    }
}
