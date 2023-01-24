package OPreSprintTwoBot;

import battlecode.common.*;

public class BotHeadquarters extends Utils{
    public static boolean CAN_SEE_HEADQUARTER;
    private static RobotInfo[] visibleEnemies;
    public static int vNonHQEnemies = 0;
    public static int inRNonHQEnemies = 0;
    public static int enemyHQInVision = 0;
    private static RobotInfo enemyHQ = null;
    private static boolean isEndangered = false;

    public static void initHeadquarters() throws GameActionException{
        BuilderWrapper.initBuilder();
        if (TRACKING_LAUNCHER_COUNT) Comms.resetRobotCount(RobotType.LAUNCHER);
        if (TRACKING_AMPLIFIER_COUNT) Comms.resetRobotCount(RobotType.AMPLIFIER);
        rc.writeSharedArray(Comms.SYMMETRY_CHANNEL, 0b111);
    }

    public static void updateEveryTurn() throws GameActionException{
        Comms.wipeCountChannels();
        combatCommsCleaner(vNonHQEnemies);
    }

    public static void runHeadquarters() throws GameActionException{
        if (rc.getRoundNum() == 2){
            Comms.initCommunicationsArray();
            alliedHQLocs = Comms.getAlliedHeadquartersLocationsList();
            guessEnemyHQLocation();
        }
        updateVisibleEnemiesInVision();
        BuilderWrapper.buildUnits(isEndangered);
        updateEveryTurn();
    }

    private static void updateVisibleEnemiesInVision() throws GameActionException{
        visibleEnemies = rc.senseNearbyRobots(UNIT_TYPE.visionRadiusSquared, ENEMY_TEAM);
        vNonHQEnemies = 0;
        enemyHQInVision = 0;
        enemyHQ = null;
        isEndangered = false;
        MapLocation combatUnit = null;
        for (int i = visibleEnemies.length; --i >= 0;) {
            if (visibleEnemies[i].type != RobotType.HEADQUARTERS) {
                vNonHQEnemies++;
                isEndangered = true;
                if (visibleEnemies[i].type == RobotType.LAUNCHER || visibleEnemies[i].type == RobotType.DESTABILIZER){
                    combatUnit = visibleEnemies[i].getLocation();
                }
            }
            else{
                enemyHQInVision++;
                enemyHQ = visibleEnemies[i];
                if (rc.canWriteSharedArray(0, 0) && rc.getRoundNum() > 1){
                    Comms.writeEnemyHeadquarterLocation(enemyHQ.location);
                }
            }
        }
        if (combatUnit != null && rc.getRoundNum() > 1)
            Comms.writeAndOverwriteLesserPriorityMessage(Comms.COMM_TYPE.COMBAT, combatUnit, Comms.SHAFlag.COMBAT_LOCATION);
    }
}
