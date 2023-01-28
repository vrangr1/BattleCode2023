package ASymmetryBot;

import battlecode.common.*;


public class SavvyBuilder extends Utils{
    private static RobotInfo[] visibleEnemies;
    private static boolean militaryUnitNearby;


    public static void initBuilder() throws GameActionException{
        updateVision();
        militaryUnitNearby = false;
    }

    private static void updateVision() throws GameActionException{
        visibleEnemies = rc.senseNearbyRobots(UNIT_TYPE.visionRadiusSquared, ENEMY_TEAM);
        // militaryUnitNearby = CombatUtils.hasMilitarUnit(visibleEnemies);
    }

    public static void updateBuilder() throws GameActionException{
        updateVision();
    }

    private static boolean tryBuildLauncher() throws GameActionException{
        return false;
    }

    public static void buildUnits() throws GameActionException{
        
    }
}