package OPostSprintBot;

import battlecode.common.*;

public class BotLauncher extends CombatUtils{

    private enum Status {
        BORN, // State at start of first turn
        EXPLORE, // No directions given
        MARCHING, // Going to a location given by comms
        PURSUING,
        ENGAGING,
        ATTACKING,
        FLANKING,
        GUARDING,
        ISLAND_WORK,
        RETREATING,
        CLOUD_WORK, // In the clouds
        STANDOFF,
    }

    private static Status launcherState;

    public static RobotInfo[] visibleEnemies;
    public static RobotInfo[] inRangeEnemies;
    public static RobotInfo[] visibleAllies;
    public static int vNonHQEnemies = 0;
    public static int inRNonHQEnemies = 0;
    public static int enemyHQInVision = 0;
    private static MapLocation enemyHQLocation = null;
    private static boolean standOff = false;
    private static RobotInfo prevTurnHostile = null;
    private static MapLocation prevTurnLocation = null;

    public static void initLauncher() throws GameActionException{
        launcherState = Status.BORN;
        setBaseDestination();
    }


    public static void runLauncher() throws GameActionException{
        if (TRACKING_LAUNCHER_COUNT) Comms.incrementRobotCount(RobotType.LAUNCHER);
        
        updateVision();
        standOff = false;
        previousTurnResolution();
        bytecodeCheck(); //0
        if (vNonHQEnemies == 0) {
            seekEnemyIslandInVision(); // [CUR_STATE] -> [ISLAND_WORK|EXPLORE]
            closerCombatDestination(); // [CUR_STATE] -> [CUR_STATE|MARCHING|EXPLORE]
        }
        bytecodeCheck(); //1
        tryToMicro();
        updateVision();
        bytecodeCheck(); //2
        if (sendCombatLocation());
        else {
            findNewCombatLocation();
        }
        bytecodeCheck(); //3
        moveAfterNonMovingCombat(); // [CUR_STATE] -> [CUR_STATE] (Only works with [MARCHING|ISLAND_WORK|EXPLORE])
        if (rc.isActionReady()) {
            updateInRangeEnemiesVision();
            bytecodeCheck(); //6
            if (inRNonHQEnemies > 0) {
                chooseTargetAndAttack(inRangeEnemies);
            }
        }
        rc.setIndicatorString(launcherState.toString() + " " + currentDestination);
    }

    private static void setBaseDestination() throws GameActionException {
        currentDestination = Comms.findNearestEnemyHeadquarterLocation();
        if (currentDestination.equals(CENTER_OF_THE_MAP)){
            if (rememberedEnemyHQLocations[2] != null)  
            currentDestination = rememberedEnemyHQLocations[2];
            else if (rememberedEnemyHQLocations[0] != null) 
                currentDestination = rememberedEnemyHQLocations[0];
            else if (rememberedEnemyHQLocations[1] != null) 
                currentDestination = rememberedEnemyHQLocations[1];
            else{
                findNewCombatLocation();
            }
        }
        pathing.setNewDestination(currentDestination);
        launcherState = Status.MARCHING;
    }

    private static void previousTurnResolution() throws GameActionException {
        cloudCentral();
        simplePursuit();
        if (launcherState != Status.PURSUING){
            prevTurnHostile = null;
            prevTurnLocation = null;
        }
    }

    private static void cloudCentral() throws GameActionException {
        boolean currentlyInCloud = rc.senseCloud(rc.getLocation());
        if (!currentlyInCloud || launcherState != Status.CLOUD_WORK) return;
        if (currentlyInCloud && (launcherState == Status.MARCHING || launcherState == Status.EXPLORE)) {
            launcherState = Status.CLOUD_WORK;
            return;
        }
    }

    private static void simplePursuit() throws GameActionException{
        if (launcherState == Status.ATTACKING && vNonHQEnemies == 0 && prevTurnHostile != null) {
            launcherState = Status.GUARDING;
            boolean willDieIfPursuit = prevTurnHostile.getType().canAttack() && rc.getHealth() <= prevTurnHostile.getType().damage;
            if (!willDieIfPursuit && rc.isMovementReady() && Movement.tryMoveInDirection(prevTurnHostile.location)){
                launcherState = Status.PURSUING;
            }
        }
    }

    private static boolean seekEnemyIslandInVision() throws GameActionException {
        if (launcherState == Status.ISLAND_WORK && (rc.senseIsland(rc.getLocation()) != -1)) {
            return true;
        }

        int[] nearbyIslands = rc.senseNearbyIslands();
        MapLocation nearestLoc = null;
        int nearestDist = -1, curDist;
        for (int i = nearbyIslands.length; --i >= 0;){
            int islandId = nearbyIslands[i];
            if (rc.senseTeamOccupyingIsland(islandId) != ENEMY_TEAM){
                continue;
            }
            MapLocation[] locations = rc.senseNearbyIslandLocations(islandId);
            for (int j = locations.length; --j >= 0;){
                MapLocation loc = locations[j];
                if (rc.isLocationOccupied(loc)) continue;
                curDist = currentLocation.distanceSquaredTo(loc);
                if (nearestLoc == null || curDist < nearestDist){
                    nearestLoc = loc;
                    nearestDist = curDist;
                }
            }
        }
        if (nearestLoc != null){
            currentDestination = nearestLoc;
            sendIslandLocation(nearestLoc);
            launcherState = Status.ISLAND_WORK;
            return true;
        }
        else{
            launcherState = Status.MARCHING;
            return false;
        }
    }

    private static void moveAfterNonMovingCombat() throws GameActionException {
        if (vNonHQEnemies == 0 && rc.isMovementReady()) {
            if (currentDestination != null) {
                pathing.setNewDestination(currentDestination);
            }
            bytecodeCheck(); //4
            if (launcherState == Status.MARCHING || launcherState == Status.ISLAND_WORK) {
                pathing.moveToDestination();
                if (rc.isMovementReady()){
                    Movement.tryMoveInDirection(currentDestination);
                }
            }
            else if (launcherState == Status.EXPLORE) {
                pathing.setAndMoveToDestination(Explore.explore());
            }
            bytecodeCheck(); //5
        }
    }

    public static void updateVision() throws GameActionException {
        updateVisibleEnemiesVision();
        updateInRangeEnemiesVision();
    }
    
    public static void updateVisibleEnemiesVision() throws GameActionException{
        visibleEnemies = rc.senseNearbyRobots(UNIT_TYPE.visionRadiusSquared, ENEMY_TEAM);
        vNonHQEnemies = 0;
        enemyHQInVision = 0;
        for (int i = visibleEnemies.length; --i >= 0;) {
            if (visibleEnemies[i].type != RobotType.HEADQUARTERS) {
                vNonHQEnemies++;
            }
            else{
                enemyHQInVision++;
                enemyHQLocation = visibleEnemies[i].location;
                if (rc.canWriteSharedArray(0, 0)){
                    Comms.writeEnemyHeadquarterLocation(enemyHQLocation);
                }
            }
        }
    }

    public static void updateInRangeEnemiesVision() throws GameActionException{
        inRangeEnemies = rc.senseNearbyRobots(UNIT_TYPE.actionRadiusSquared, ENEMY_TEAM);
        inRNonHQEnemies = 0;
        for (int i = inRangeEnemies.length; --i >= 0;) {
            if (inRangeEnemies[i].type != RobotType.HEADQUARTERS) {
                inRNonHQEnemies++;
            }
        }
    }

    private static double getEnemyScore(RobotInfo enemyUnit) throws GameActionException{
        RobotType enemyType = enemyUnit.type;
        int enemyHealth = enemyUnit.getHealth();
        if (enemyHealth <= UNIT_TYPE.damage && enemyType != RobotType.HEADQUARTERS) 
            return 100000 + enemyHealth; // Instakill
        // TODO: Factor in time dilation cost
        // int rubbleAtLocation = rc.senseRubble(enemyUnit.getLocation());
		switch(enemyType) {
            case HEADQUARTERS:
		    	return -100.0;
		    case AMPLIFIER:
		    	return 1;
            case CARRIER:
		    case BOOSTER:
            case DESTABILIZER:
		    	return 0.22 / (enemyHealth * (10.0)); // Max= 0.22, Min = 0.005 Low priority
		    case LAUNCHER:
		    	return 220.0 * enemyType.damage / (enemyHealth * (10.0));
            default:
                return -2.0;
		}
	}

    private static void chooseTargetAndAttack(RobotInfo[] targets) throws GameActionException {
		RobotInfo bestTarget = null;
		double bestValue = -1;
        double value = -1;
		for (int i = targets.length; --i >= 0;) {
			value = getEnemyScore(targets[i]);
			if (value > bestValue) {
				bestValue = value;
				bestTarget = targets[i];
			}
		}
		if (bestTarget != null && rc.canAttack(bestTarget.location)) {
            rc.attack(bestTarget.location);
            prevTurnHostile = bestTarget;
            launcherState = Status.ATTACKING;
		}
	}

    private static boolean tryMoveToHelpAlly(RobotInfo closestHostile) throws GameActionException {
        if(closestHostile == null) return false;
		MapLocation closestHostileLocation = closestHostile.location;
		
		boolean allyIsFighting = false;
		RobotInfo[] alliesAroundHostile = rc.senseNearbyRobots(closestHostileLocation, UNIT_TYPE.actionRadiusSquared, MY_TEAM);
		for (int i = alliesAroundHostile.length; --i >= 0;) {
			if (alliesAroundHostile[i].type.canAttack()) {
				if (alliesAroundHostile[i].location.distanceSquaredTo(closestHostileLocation) <= alliesAroundHostile[i].type.actionRadiusSquared) {
					allyIsFighting = true;
					break;
				}
			}
		}
		if (allyIsFighting) 
			if (Movement.tryMoveInDirection(closestHostileLocation)) {
				launcherState = Status.FLANKING;
                return true;
            }
		return false;
	}

    private static boolean tryMoveToAttackProductionUnit(RobotInfo closestHostile) throws GameActionException {
        if (closestHostile == null) return false;
		if (closestHostile.type.canAttack() || closestHostile.type == RobotType.HEADQUARTERS) 
            return false;
	    pathing.setAndMoveToDestination(closestHostile.location);
        if (!rc.isMovementReady() || Movement.tryMoveInDirection(closestHostile.location)) {
            launcherState = Status.PURSUING;
            return true;
        }
		return false;
	}

    private static boolean tryMoveToEngageOutnumberedEnemy(RobotInfo closestHostile) throws GameActionException {
        if(closestHostile == null) return false;
        MapLocation closestHostileLocation = closestHostile.location;
        int numNearbyHostiles = 0;
		for (int i = visibleEnemies.length; --i >= 0;) {
			if (visibleEnemies[i].type.canAttack()) {
					numNearbyHostiles += 1;
			}
		}
		RobotInfo[] visibleAllies = rc.senseNearbyRobots(UNIT_TYPE.visionRadiusSquared, MY_TEAM);
		int numNearbyAllies = 1; // Counts ourself
		for (int i = visibleAllies.length; --i >= 0;) {
			if (visibleAllies[i].type.canAttack() && visibleAllies[i].health >= visibleAllies[i].type.getMaxHealth()/2.0) {
				numNearbyAllies += 1;
			}
		}
		
		if (numNearbyAllies >= numNearbyHostiles || (numNearbyHostiles == 1 && rc.getHealth() > closestHostile.health)) {
			if (Movement.tryMoveInDirection(closestHostile.location)){
                launcherState = Status.FLANKING;
                return true;
            }
            else if(numNearbyAllies >= 1.5 * numNearbyHostiles && Movement.tryForcedMoveInDirection(closestHostile.location)){
                launcherState = Status.FLANKING;
                return true;
            }
            else {
                standOff = true;
                return false;
            }
		}
		return false;
	}

    private static boolean retreatIfOutnumbered() throws GameActionException {
		RobotInfo closestHostileThatAttacksUs = null;
		int closestDistSq = Integer.MAX_VALUE;
		int numHostilesThatAttackUs = 0;
		for (int i = visibleEnemies.length; --i >= 0;) {
            RobotInfo hostile = visibleEnemies[i];
			if (hostile.type == RobotType.LAUNCHER) {
				int distSq = hostile.location.distanceSquaredTo(rc.getLocation());
				if (distSq <= hostile.type.actionRadiusSquared) {
					if (distSq < closestDistSq) {
						closestDistSq = distSq;
						closestHostileThatAttacksUs = hostile;
					}
					numHostilesThatAttackUs += 1;
				}
			}
		}
		
		if (numHostilesThatAttackUs == 0) {
			return false;
		}
		
		int numAlliesAttackingClosestHostile = 0;
		if (rc.getLocation().distanceSquaredTo(closestHostileThatAttacksUs.location) <= UNIT_TYPE.actionRadiusSquared) {
			numAlliesAttackingClosestHostile += 1;
		}

		RobotInfo[] nearbyAllies = rc.senseNearbyRobots(closestHostileThatAttacksUs.location, UNIT_TYPE.visionRadiusSquared, MY_TEAM);
		for (int i = nearbyAllies.length; --i >= 0;) {
            RobotInfo ally = nearbyAllies[i];
			if (ally.type == RobotType.LAUNCHER) {
				if (ally.location.distanceSquaredTo(closestHostileThatAttacksUs.location)
						<= ally.type.actionRadiusSquared) {
					numAlliesAttackingClosestHostile += 1;
				}
			}
		}
		
		if (numAlliesAttackingClosestHostile > numHostilesThatAttackUs) {
			return false;
		} 
		if (numAlliesAttackingClosestHostile == numHostilesThatAttackUs) {
			if (numHostilesThatAttackUs == 1) {
				if (rc.getHealth() >= closestHostileThatAttacksUs.health) {
					return false;
				}
			} else {
				return false;
			}
		}
		
		MapLocation retreatTarget = rc.getLocation();
		for (int i = visibleEnemies.length; --i >= 0;) {
            RobotInfo hostile = visibleEnemies[i];
			if (!(hostile.type == RobotType.LAUNCHER)) continue;			
			retreatTarget = retreatTarget.add(hostile.location.directionTo(rc.getLocation()));
		}
		if (!rc.getLocation().equals(retreatTarget)) {
            if (rc.isActionReady() && inRNonHQEnemies > 0) {
                chooseTargetAndAttack(inRangeEnemies);
            }
			return Movement.tryForcedMoveInDirection(retreatTarget);
		}
		return false;
	}

    private static boolean tryToMicro() throws GameActionException {
        if (vNonHQEnemies == 0) { // TODO: Either wait or get out of any possible Destabilizer range
            return false;
        }

        if (rc.isMovementReady() && retreatIfOutnumbered()){
            launcherState = Status.RETREATING;
            return true;
        }

        if (rc.isActionReady()){
            if (inRNonHQEnemies > 0) {
                chooseTargetAndAttack(inRangeEnemies);
            }
            else if (rc.isMovementReady() && vNonHQEnemies > 0) {
                RobotInfo closestHostile = getClosestUnitWithCombatPriority(visibleEnemies);
                if(tryMoveToHelpAlly(closestHostile)) {
                    return true;
                }
                if(tryMoveToAttackProductionUnit(closestHostile)) {
                    return true;
                }
            }
        }
        if (rc.isMovementReady()){
            // Most important function
            if (inRNonHQEnemies > 0 && tryToBackUpToMaintainMaxRangeLauncher(visibleEnemies)) {
                launcherState = Status.FLANKING;
                return true;
            }
            RobotInfo closestHostile = getClosestUnitWithCombatPriority(visibleEnemies);
            if (tryMoveToHelpAlly(closestHostile)) {
                return true; // Maybe add how many turns of attack cooldown here and how much damage being taken?
            }
            if (tryMoveToEngageOutnumberedEnemy(closestHostile)) {
                return true;
            }
            if (tryMoveToAttackProductionUnit(closestHostile)) {
                return true;
            }
        }
        return false;
    }

    public static boolean sendCombatLocation() throws GameActionException{
        if (vNonHQEnemies > 0){
			RobotInfo closestHostile = getClosestUnitWithCombatPriority(visibleEnemies);
            if (closestHostile == null) return false;
			if (!standOff){ // The idea is to prevent standoffs from flooding comms with local info
                currentDestination = closestHostile.getLocation();
                if (rc.canWriteSharedArray(0, 0))
				    Comms.writeAndOverwriteLesserPriorityMessage(Comms.COMM_TYPE.COMBAT, closestHostile.getLocation(), Comms.SHAFlag.COMBAT_LOCATION);
            }
            return true;
        }
        return false;
    }

    public static boolean sendIslandLocation(MapLocation closestIslandLocation) throws GameActionException{
        if (rc.canWriteSharedArray(0, 0)){
			Comms.writeAndOverwriteLesserPriorityMessage(Comms.COMM_TYPE.COMBAT, closestIslandLocation, Comms.SHAFlag.COMBAT_LOCATION);
            return true;
        }
        return false;
    }
    
    // If our current destination has no enemies left, move to the nearest new location with combat
    private static boolean findNewCombatLocation() throws GameActionException{
        if (circleEnemyHQ()) return true;
        if (currentDestination == null || (vNonHQEnemies == 0 && launcherState != Status.ISLAND_WORK && 
            rc.getLocation().distanceSquaredTo(currentDestination) <= UNIT_TYPE.visionRadiusSquared)){
            Comms.wipeThisLocationFromChannels(Comms.COMM_TYPE.COMBAT, Comms.SHAFlag.COMBAT_LOCATION, currentDestination);
            MapLocation combatLocation = Comms.findNearestLocationOfThisTypeOutOfVision(rc.getLocation(), Comms.COMM_TYPE.COMBAT, Comms.SHAFlag.COMBAT_LOCATION);
            if (combatLocation != null){
                currentDestination = combatLocation;
                pathing.setNewDestination(currentDestination);
                launcherState = Status.MARCHING;
                return true;
            }
        }
        return false;
    }

    /**
    * This will try to update the destination of the soldier so as to not make it go away from closer fights
    */
    private static void closerCombatDestination() throws GameActionException{
        if (circleEnemyHQ()) return;
        MapLocation nearestCombatLocation = Comms.findNearestLocationOfThisTypeOutOfVision(rc.getLocation(), Comms.COMM_TYPE.COMBAT, Comms.SHAFlag.COMBAT_LOCATION);
        if (currentDestination == null && nearestCombatLocation == null){
            launcherState = Status.EXPLORE;
        }
        else if (currentDestination == null || (nearestCombatLocation!= null && !rc.canSenseLocation(currentDestination) && 
            rc.getLocation().distanceSquaredTo(currentDestination) > rc.getLocation().distanceSquaredTo(nearestCombatLocation))){
                currentDestination = nearestCombatLocation;
                launcherState = Status.MARCHING;
        }
    }

    private static boolean circleEnemyHQ(){
        if (enemyHQInVision == visibleEnemies.length && enemyHQInVision > 0 && launcherState != Status.ISLAND_WORK){
            currentDestination = enemyHQLocation;
            launcherState = Status.MARCHING;
            return true;
        }
        return false;
    }

    public static void coverInCloud() throws GameActionException{
        if (rc.senseCloud(rc.getLocation())) return;
        for (int i = 0; i < 8; i++){
            Direction dir = directions[i];
            if (rc.senseCloud(rc.getLocation().add(dir))){
                if (rc.isMovementReady()){
                    prevTurnLocation = rc.getLocation();
                    rc.move(dir);
                }
            }
        }
    }
}
