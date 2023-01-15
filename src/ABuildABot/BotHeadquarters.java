package ABuildABot;

import battlecode.common.*;

public class BotHeadquarters extends Utils{

    public static void initHeadquarters() throws GameActionException{
        Builder.initBuilder();
        if (TRACKING_LAUNCHER_COUNT) Comms.resetRobotCount(RobotType.LAUNCHER);
    }

    public static void updateEveryTurn() throws GameActionException{
        Comms.wipeCountChannels();
    }

    public static void runHeadquarters() throws GameActionException{
        if (rc.getRoundNum() == 2)
            Comms.initCommunicationsArray();
        updateEveryTurn();
        Builder.buildUnits();
        if (Clock.getBytecodeNum() > 700){
            RobotInfo[] visibleEnemies = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, ENEMY_TEAM);
            if (visibleEnemies.length > 0){
				Comms.writeAndOverwriteLesserPriorityMessage(Comms.COMM_TYPE.COMBAT, visibleEnemies[0].getLocation(), Comms.SHAFlag.COMBAT_LOCATION);
            }
        }
    }
}
