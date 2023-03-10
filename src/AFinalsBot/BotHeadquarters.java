package AFinalsBot;

import battlecode.common.*;

public class BotHeadquarters extends Utils{
    public static boolean CAN_SEE_HEADQUARTER;
    private static RobotInfo[] visibleEnemies;
    public static int vNonHQEnemies = 0;
    public static int inRNonHQEnemies = 0;
    public static int enemyHQInVision = 0;
    private static RobotInfo enemyHQ = null;
    private static boolean isEndangered = false;
    public static boolean canSeeEnemyHQ = false;

    public static void initHeadquarters() throws GameActionException{
        BuilderWrapper.initBuilder();
        if (TRACKING_LAUNCHER_COUNT) Comms.resetRobotCount(RobotType.LAUNCHER);
        if (TRACKING_AMPLIFIER_COUNT) Comms.resetRobotCount(RobotType.AMPLIFIER);
        rc.writeSharedArray(Comms.SYMMETRY_CHANNEL, 0b111);
        setCanSeeEnemyHQ();
        for (int i = Symmetry.SYMMETRY.values().length; --i >= 0;) {
            if (Symmetry.checkIfSymmetry(Symmetry.SYMMETRY.values()[i]) && !Symmetry.checkThisSymmetry(Symmetry.SYMMETRY.values()[i])){
                Symmetry.removeSymmetry(Symmetry.SYMMETRY.values()[i], "3");
                mapSymmetry[i] = false;
            }
        }
    }

    private static void setCanSeeEnemyHQ() throws GameActionException{
        canSeeEnemyHQ = rc.senseNearbyRobots(UNIT_TYPE.visionRadiusSquared, ENEMY_TEAM).length > 0;
    }

    private static void updateEndOfEveryTurn() throws GameActionException{
        if (rc.getRoundNum() > 1 && Comms.getHeadquarterIndex(rc.getLocation()) == Comms.getHeadquartersCount() - 1)
            Comms.wipeCountChannels();
        combatCommsCleaner(vNonHQEnemies);
    }

    private static void updateHeadquarter() throws GameActionException{
        updateVisibleEnemiesInVision();
        ElixirProducer.updateElixirStuff();
    }

    public static void runHeadquarters() throws GameActionException{
        if (rc.getRoundNum() == 2){
            Comms.initCommunicationsArray();
            alliedHQLocs = Comms.getAlliedHeadquartersLocationsList();
            Symmetry.guessEnemyHQLocation();
        }
        updateHeadquarter();
        BuilderWrapper.buildUnits(isEndangered);
        updateEndOfEveryTurn();
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
