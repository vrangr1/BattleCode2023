package APostQualsBot;

import battlecode.common.*;

public class BotDestabilizer extends BotLauncher{

    public static void initDestabilizer() throws GameActionException{
        initLauncher();
    }

    public static void runDestabilizer() throws GameActionException{
        updateVision();
        previousTurnResolution();
        bytecodeCheck();
        if (vNonHQEnemies == 0){
            manageHealingState();
            if (inHealingState){
                tryToHealAtIsland();
            }
            else{
                closerCombatDestination(); // [CUR_STATE] -> [CUR_STATE|MARCHING|EXPLORE]
                if (MAP_SIZE >= 1200){
                    if (doIdling()){
                        rc.setIndicatorString("Idling");
                        return;
                    }
                }
            }
        }
        bytecodeCheck();
        if (rc.isActionReady()) {
            updateInRangeEnemiesVision();
            bytecodeCheck(); //6
            if (vNonHQEnemies > 0) {
                chooseTargetAndAttack(visibleEnemies);
            }
        }
        if (!rc.isActionReady() || !rc.isMovementReady()) // No need to update vision if you haven't moved or attacked
            updateVision();

        if (sendCombatLocation());
        else {
            findNewCombatLocation();
        }
        moveAfterNonMovingCombat();
        if (rc.isActionReady()) {
            updateInRangeEnemiesVision();
            bytecodeCheck(); //6
            if (vNonHQEnemies > 0) {
                chooseTargetAndAttack(visibleEnemies);
            }
        }
        rc.setIndicatorString(launcherState + " " + destinationFlag);
    }

    public static void chooseTargetAndAttack(RobotInfo[] targets) throws GameActionException {
		RobotInfo bestTarget = null;
		double bestValue = -1;
        double value = -1;
		for (int i = targets.length; --i >= 0;) {
            //TODO: Change this for range shenanigans
            if (!rc.canDestabilize(targets[i].location)){
                continue;
            }
			value = getEnemyScore(targets[i]);
			if (value > bestValue) {
				bestValue = value;
				bestTarget = targets[i];
			}
		}
		if (bestTarget != null) {
            rc.destabilize(bestTarget.location);
            prevTurnHostile = bestTarget;
            launcherState = Status.ATTACKING;
		}
	}

}
