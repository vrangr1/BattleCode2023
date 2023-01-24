package APreSprintTwoBot;

import battlecode.common.*;
import APreSprintTwoBot.path.Nav;
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
        CIRCLING,
    }

    private static Status launcherState;

    public static RobotInfo[] visibleEnemies;
    public static RobotInfo[] inRangeEnemies;
    public static RobotInfo[] visibleAllies;
    private static MapLocation[] cloudLocations;
    public static int vNonHQEnemies = 0;
    public static int inRNonHQEnemies = 0;
    public static int enemyHQInVision = 0;
    private static RobotInfo enemyHQ = null;
    private static boolean standOff = false;
    private static RobotInfo prevTurnHostile = null;
    private static MapLocation prevTurnLocation = null;
    private static MapLocation circleLocation = null;
    private static int minCircleDistance = 0;
    private static int maxCircleDistance = 0;
    private static boolean isClockwise = true;
    private static int LOW_HEALTH_MARK = 30;

    public static void initLauncher() throws GameActionException{
        launcherState = Status.BORN;
        setBaseDestination();
    }


    public static void runLauncher() throws GameActionException{
        if (TRACKING_LAUNCHER_COUNT) Comms.incrementRobotCount(RobotType.LAUNCHER);
        
        updateVision();
        previousTurnResolution();
        findAndWriteWellLocationsToComms();
        bytecodeCheck(); //0
        if (vNonHQEnemies == 0) {
            seekEnemyIslandInVision(); // [CUR_STATE] -> [ISLAND_WORK|EXPLORE]
            // closerCombatDestination(); // [CUR_STATE] -> [CUR_STATE|MARCHING|EXPLORE]
            // lowHealthCircle(); // [CUR_STATE] -> [CUR_STATE|MARCHING|CIRCLING]
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
            else {
                attackCloud();
            }
        }
        rc.setIndicatorString(launcherState.toString() + " " + currentDestination + " |Des flag " + destinationFlag + 
            " |Can move " + rc.isMovementReady());
    }

    private static void setBaseDestination() throws GameActionException {
        if (areHQsCornered()){
            currentDestination = defaultEnemyLocation();
            pathing.setNewDestination(currentDestination);
            destinationFlag = "sBD " + currentDestination;
            // if (rc.getRoundNum() <= BIRTH_ROUND + 1){
            //     System.out.println("id: " + rc.getID() + "; rn: " + rc.getRoundNum() + "; currentDest" + currentDestination + "; curLoc: " + rc.getLocation());
            // }
            launcherState = Status.MARCHING;
            return;
        }
        currentDestination = Comms.findNearestEnemyHeadquarterLocation();
        for (int i = rememberedEnemyHQLocations.length; --i >= 0;){
            if (!mapSymmetry[i])
                removeSymmetry(SYMMETRY.values()[i], "3");
            if (!checkIfSymmetry(SYMMETRY.values()[i])){
                mapSymmetry[i] = false;
                rememberedEnemyHQLocations[i] = null;
            }
            else if (enemyHQ == null && rememberedEnemyHQLocations[i] != null &&
                rememberedEnemyHQLocations[i].distanceSquaredTo(rc.getLocation()) <= UNIT_TYPE.visionRadiusSquared &&
                rc.canSenseLocation(rememberedEnemyHQLocations[i])){
                rememberedEnemyHQLocations[i] = null;
                mapSymmetry[i] = false;
                if (checkIfSymmetry(SYMMETRY.values()[i])){
                    removeSymmetry(SYMMETRY.values()[i], "1");
                }
            }
        }
        if (currentDestination.equals(CENTER_OF_THE_MAP)){
            if (rc.getRoundNum() <= BIRTH_ROUND + 1){
                currentDestination = defaultEnemyLocation();
            }
            else {
                int shortestDist = Integer.MAX_VALUE;
                for (int i = rememberedEnemyHQLocations.length; --i >= 0;){
                    if (rememberedEnemyHQLocations[i] != null){
                        int dist = rememberedEnemyHQLocations[i].distanceSquaredTo(rc.getLocation());
                        if (dist < shortestDist){
                            shortestDist = dist;
                            currentDestination = rememberedEnemyHQLocations[i];
                        }
                    }
                }
            }

        }
        pathing.setNewDestination(currentDestination);
        destinationFlag = "sBD " + currentDestination;
        launcherState = Status.MARCHING;
    }

    private static void previousTurnResolution() throws GameActionException {
        standOff = false;
        destinationFlag = "";
        cloudLocations = null;
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
            destinationFlag += " cW";
            launcherState = Status.CLOUD_WORK;
            return;
        }
    }

    // TODO: Check why this is not triggering
    private static void simplePursuit() throws GameActionException{
        if (launcherState == Status.ATTACKING && vNonHQEnemies == 0 && prevTurnHostile != null) {
            launcherState = Status.GUARDING;
            boolean willDieIfPursuit = CombatUtils.isMilitaryUnit(prevTurnHostile.getType()) && rc.getHealth() <= prevTurnHostile.getType().damage;
            if (!willDieIfPursuit && rc.isMovementReady() && Movement.tryFlagMoveInDirection(prevTurnHostile.location, false)){
                launcherState = Status.PURSUING;
                destinationFlag += " sP";
            }
        }
    }

    private static void lowHealthCircle() throws GameActionException {
        if (vNonHQEnemies == 0 && rc.getHealth() <= LOW_HEALTH_MARK && rc.isMovementReady() && rc.getRoundNum() > 150){
            if (rc.getLocation().distanceSquaredTo(parentHQLocation) <= 100 && launcherState != Status.ISLAND_WORK){
                if (launcherState != Status.CIRCLING && circleLocation != parentHQLocation)
                    setCircle(parentHQLocation, 10, 100);
                circleWorks();
                launcherState = Status.CIRCLING;
            }
            else{
                currentDestination = parentHQLocation;
                pathing.setAndMoveToDestination(currentDestination);
                launcherState = Status.MARCHING;
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
            destinationFlag += " sEI";
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
                if (currentDestination.equals(circleLocation))
                    return;
                pathing.moveToDestination();
                if (rc.isMovementReady()){
                    Nav.goTo(currentDestination);
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
        enemyHQ = null;
        for (int i = visibleEnemies.length; --i >= 0;) {
            if (visibleEnemies[i].type != RobotType.HEADQUARTERS) {
                vNonHQEnemies++;
            }
            else{
                enemyHQInVision++;
                enemyHQ = visibleEnemies[i];
                if (rc.canWriteSharedArray(0, 0)){
                    Comms.writeEnemyHeadquarterLocation(enemyHQ.location);
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
            return 100000 + enemyHealth + enemyType.damage; // Instakill
        // TODO: Factor in time dilation cost
        // int rubbleAtLocation = rc.senseRubble(enemyUnit.getLocation());
		switch(enemyType) {
            case HEADQUARTERS:
		    	return -100.0;
		    case AMPLIFIER:
		    	return 1;
            case CARRIER:
		    case BOOSTER:
                return 100.0 / Math.ceil((double)enemyHealth/UNIT_TYPE.damage);
            case DESTABILIZER:
		    case LAUNCHER:
		    	return 10000.0 / Math.ceil((double)enemyHealth/UNIT_TYPE.damage);
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
            updateInRangeEnemiesVision();
            rc.attack(bestTarget.location);
            prevTurnHostile = bestTarget;
            launcherState = Status.ATTACKING;
		}
	}

    private static boolean tryMoveToHelpAlly(RobotInfo closestHostile) throws GameActionException {
        if(closestHostile == null) return false;
        //# if (inRNonHQEnemies > 0) return false;
		MapLocation closestHostileLocation = closestHostile.location;
		
		boolean allyIsFighting = false;
		RobotInfo[] alliesAroundHostile = rc.senseNearbyRobots(closestHostileLocation, UNIT_TYPE.actionRadiusSquared, MY_TEAM);
		if (alliesAroundHostile.length == 0) return false;
        for (int i = alliesAroundHostile.length; --i >= 0;) {
			if (alliesAroundHostile[i].type.canAttack()) {
				if (alliesAroundHostile[i].location.distanceSquaredTo(closestHostileLocation) <= alliesAroundHostile[i].type.actionRadiusSquared) {
					allyIsFighting = true;
					break;
				}
			}
		}
		if (allyIsFighting) {
            Direction bestDir = Movement.combatMovement(visibleEnemies, rc.getLocation().directionTo(closestHostileLocation));
            if (bestDir != null) {
                rc.move(bestDir);
                launcherState = Status.FLANKING;
                return true;
            }
        }
		return false;
	}

    private static boolean tryMoveToAttackProductionUnit(RobotInfo closestHostile) throws GameActionException {
        if (closestHostile == null) return false;
		if (CombatUtils.isMilitaryUnit(closestHostile.type) || closestHostile.type == RobotType.HEADQUARTERS) 
            return false;
	    pathing.setAndMoveToDestination(closestHostile.location);
        if (!rc.isMovementReady() || Movement.tryFlagMoveInDirection(closestHostile.location, true)) {
            destinationFlag += " tM2APU";
            launcherState = Status.PURSUING;
            return true;
        }
		return false;
	}

    private static boolean tryMoveToEngageOutnumberedEnemy(RobotInfo closestHostile) throws GameActionException {
        if (closestHostile == null) return false;
        if (isMilitaryUnit(closestHostile.type) && closestHostile.location.distanceSquaredTo(rc.getLocation()) <= UNIT_TYPE.actionRadiusSquared) return false;
        int numNearbyHostiles = 0;
		for (int i = visibleEnemies.length; --i >= 0;) {
			if (isMilitaryUnit(visibleEnemies[i].type)) {
					numNearbyHostiles += 1;
			}
		}
        if (numNearbyHostiles == 0) return false;
		RobotInfo[] visibleAllies = rc.senseNearbyRobots(closestHostile.location, UNIT_TYPE.actionRadiusSquared, MY_TEAM);
		int numNearbyAllies = 1; // Counts ourself
		for (int i = visibleAllies.length; --i >= 0;) {
			if (isMilitaryUnit(visibleAllies[i])) {
				numNearbyAllies += 1;
			}
		}
		
		if (numNearbyAllies >= numNearbyHostiles || (numNearbyHostiles == 1 && rc.getHealth() >= closestHostile.health) || rc.getHealth() <= 30) {
			if (Movement.tryFlagMoveInDirection(closestHostile.location, false)){
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
		RobotInfo closestHosAttack = null;
		int closestDistSq = Integer.MAX_VALUE;
		int numHostilesThatAttackUs = 0;
		for (int i = visibleEnemies.length; --i >= 0;) {
            RobotInfo hostile = visibleEnemies[i];
			if (isMilitaryUnit(hostile.type)) {
				int distSq = hostile.location.distanceSquaredTo(rc.getLocation());
				if (distSq <= hostile.type.actionRadiusSquared) {
					if (distSq < closestDistSq) {
						closestDistSq = distSq;
						closestHosAttack = hostile;
					}
					numHostilesThatAttackUs += 1;
				}
			}
		}
		
		if (numHostilesThatAttackUs == 0) {
			return false;
		}
		
		int numAlliesAttackingClosestHostile = 0;
		if (rc.getLocation().distanceSquaredTo(closestHosAttack.location) <= UNIT_TYPE.actionRadiusSquared) {
			numAlliesAttackingClosestHostile += 1;
		}

		RobotInfo[] nearbyAllies = rc.senseNearbyRobots(closestHosAttack.location, UNIT_TYPE.visionRadiusSquared, MY_TEAM);
		for (int i = nearbyAllies.length; --i >= 0;) {
            RobotInfo ally = nearbyAllies[i];
			if (isMilitaryUnit(ally.type)) {
				numAlliesAttackingClosestHostile += 1;
			}
		}
		
		if (numAlliesAttackingClosestHostile > numHostilesThatAttackUs) {
			return false;
		} 
		if (numAlliesAttackingClosestHostile == numHostilesThatAttackUs) {
			if (numHostilesThatAttackUs == 1) {
                if (Math.ceil((double)rc.getHealth()/closestHosAttack.getType().damage) >= 
                    Math.ceil((double)closestHosAttack.health/ UNIT_TYPE.damage)) {
					return false;
				}
			} 
            else {
				return false;
			}
		}
		
		MapLocation retreatTarget = rc.getLocation();
		for (int i = visibleEnemies.length; --i >= 0;) {
            RobotInfo hostile = visibleEnemies[i];
            if (!isMilitaryUnit(hostile.getType())) 
                continue;
			retreatTarget = retreatTarget.add(hostile.location.directionTo(rc.getLocation()));
		}
		if (!rc.getLocation().equals(retreatTarget)) {
            if (rc.isActionReady() && inRNonHQEnemies > 0) {
                chooseTargetAndAttack(inRangeEnemies);
            }
			Direction bestDir = Movement.combatMovement(visibleEnemies, rc.getLocation().directionTo(retreatTarget));
            if (bestDir != null) {
                destinationFlag += " " + bestDir;
                rc.move(bestDir);
                launcherState = Status.RETREATING;
                return true;
            }
		}
		return false;
	}

    private static boolean tryToMicro() throws GameActionException {
        if (vNonHQEnemies == 0) { // TODO: Either wait or get out of any possible Destabilizer range
            return false;
        }

        if (rc.isMovementReady() && retreatIfOutnumbered()){
            destinationFlag += " -1";
            return true;
        }

        if (rc.isActionReady()){
            if (inRNonHQEnemies > 0) {
                chooseTargetAndAttack(inRangeEnemies);
            }
            else if (rc.isMovementReady() && vNonHQEnemies > 0) {
                RobotInfo closestHostile = getClosestUnitWithCombatPriority(visibleEnemies);
                if(tryMoveToHelpAlly(closestHostile)) {
                    destinationFlag += " 1 " + closestHostile.location.toString();
                    return true;
                }
                if(tryMoveToAttackProductionUnit(closestHostile)) {
                    destinationFlag += " 2";
                    return true;
                }
            }
        }
        if (rc.isMovementReady()){
            RobotInfo closestHostile = getClosestUnitWithCombatPriority(visibleEnemies);
            // Most important function
            if (inRNonHQEnemies > 0 && tryToBackUpToMaintainMaxRangeLauncher(visibleEnemies)) {
                destinationFlag += " 2.5";
                launcherState = Status.FLANKING;
                return true;
            }
            if (tryMoveToEngageOutnumberedEnemy(closestHostile)) {
                destinationFlag += " 4";
                return true;
            }
            if (tryMoveToHelpAlly(closestHostile)) {
                destinationFlag += " 3";
                return true; // Maybe add how many turns of attack cooldown here and how much damage being taken?
            }
            if (tryMoveToAttackProductionUnit(closestHostile)) {
                destinationFlag += " 5";
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
        circleEnemyHQ();
        if (vNonHQEnemies == 0 && (currentDestination == null || (launcherState != Status.ISLAND_WORK && 
            rc.getLocation().distanceSquaredTo(currentDestination) <= UNIT_TYPE.actionRadiusSquared))){
            Comms.wipeThisLocationFromChannels(Comms.COMM_TYPE.COMBAT, Comms.SHAFlag.COMBAT_LOCATION, currentDestination);
            MapLocation combatLocation = Comms.findNearestLocationOfThisTypeOutOfVision(rc.getLocation(), Comms.COMM_TYPE.COMBAT, Comms.SHAFlag.COMBAT_LOCATION);
            if (combatLocation != null){
                currentDestination = combatLocation;
                destinationFlag += " fNCL";
                pathing.setNewDestination(currentDestination);
                launcherState = Status.MARCHING;
                return true;
            }
            else if (currentDestination == null || currentDestination.distanceSquaredTo(rc.getLocation()) <= 5 && enemyHQ == null){
                setBaseDestination();
            }
        }
        return false;
    }

    /**
    * This will try to update the destination of the soldier so as to not make it go away from closer fights
    */
    private static void closerCombatDestination() throws GameActionException{
        circleEnemyHQ();
        MapLocation nearestCombatLocation = Comms.findNearestLocationOfThisTypeOutOfVision(rc.getLocation(), Comms.COMM_TYPE.COMBAT, Comms.SHAFlag.COMBAT_LOCATION);
        if (currentDestination == null && nearestCombatLocation == null){
            launcherState = Status.EXPLORE;
        }
        else if (currentDestination == null || (nearestCombatLocation!= null && !rc.canSenseLocation(currentDestination) && 
            rc.getLocation().distanceSquaredTo(currentDestination) > rc.getLocation().distanceSquaredTo(nearestCombatLocation))){
                currentDestination = nearestCombatLocation;
                destinationFlag += " cCD";
                launcherState = Status.MARCHING;
        }
    }

    private static void circleEnemyHQ() throws GameActionException{
        if (enemyHQInVision == visibleEnemies.length && enemyHQInVision > 0 && launcherState != Status.ISLAND_WORK){
            visibleAllies = rc.senseNearbyRobots(UNIT_TYPE.visionRadiusSquared, MY_TEAM);
            if (launcherState != Status.CIRCLING && circleLocation != enemyHQ.location){
                setCircle(enemyHQ.location, 9, 20);
                launcherState = Status.CIRCLING;
            }
            destinationFlag = "cEHQ";
            circleWorks();
        }
    }

    private static void setCircle(MapLocation cirCenter, int minDist, int maxDist) throws GameActionException{
        circleLocation = cirCenter;
        isClockwise = true;
        minCircleDistance = minDist;
        maxCircleDistance = maxDist;
    }

    private static void circleWorks() throws GameActionException{
        Direction dirToCenter = rc.getLocation().directionTo(circleLocation);
        if (!rc.isMovementReady()) return;
        Direction[] dirs;
        if (isClockwise){
            dirs = new Direction[] {dirToCenter.rotateRight(), dirToCenter, dirToCenter.rotateRight().rotateRight(), 
                dirToCenter.rotateRight().rotateRight().rotateRight(), dirToCenter.rotateRight().rotateRight().rotateRight().rotateRight()};
        }
        else{
            dirs = new Direction[] {dirToCenter.rotateLeft(), dirToCenter, dirToCenter.rotateLeft().rotateLeft(), 
                dirToCenter.rotateLeft().rotateLeft().rotateLeft(), dirToCenter.rotateLeft().rotateLeft().rotateLeft().rotateLeft()};
        }
        for (Direction dir: dirs){
            MapLocation newLocation = rc.getLocation().add(dir);
            if (!rc.canSenseLocation(newLocation)) continue;
            int distance = newLocation.distanceSquaredTo(circleLocation);
            if (distance <= minCircleDistance && distance <= rc.getLocation().distanceSquaredTo(circleLocation)) continue;
            if (distance > maxCircleDistance) continue;
            if (rc.senseMapInfo(newLocation).getCurrentDirection() != Direction.CENTER) continue;
            if (rc.canMove(dir)){
                rc.move(dir);
                launcherState = Status.CIRCLING;
                return;
            }
        }
        isClockwise = !isClockwise;
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

    private static boolean tryToBackUpToMaintainMaxRangeLauncher(RobotInfo[] visibleHostiles) throws GameActionException {
		int minHosDist = Integer.MAX_VALUE;
        MapLocation lCR = rc.getLocation();
        for (RobotInfo hostile : visibleHostiles) {
			if (!CombatUtils.isMilitaryUnit(hostile.type) && hostile.type != RobotType.HEADQUARTERS) continue;
			int distSq = lCR.distanceSquaredTo(hostile.location);
			if (distSq < minHosDist) {
				minHosDist = distSq;
			}
		}
		
        // We don't want to get out of our max range, not in this function
		if (minHosDist > rc.getType().actionRadiusSquared) return false;
		
		Direction bestRetreatDir = null;
		int bestDistSq = minHosDist;
        double bestRubble = rc.senseMapInfo(rc.getLocation()).getCooldownMultiplier(ENEMY_TEAM) * 10.0;

		for (Direction dir : directions) {
			if (!rc.canMove(dir)) continue;
			MapLocation dirLoc = lCR.add(dir);
            MapInfo dirLocMapInfo = rc.senseMapInfo(dirLoc);
            // double dirLocRubble = rc.senseMapInfo(dirLoc).getCooldownMultiplier(ENEMY_TEAM) * 10.0;
            // if (dirLocRubble > bestRubble) continue; // Don't move to even more rubble
            if (rc.senseCloud(dirLoc) && !rc.isActionReady()){
                bestRetreatDir = dir;
                break;
            }
            if (dirLocMapInfo.getCurrentDirection() != Direction.CENTER) continue; // Don't move into a moving tile
			int smallestDistSq = Integer.MAX_VALUE;
			for (int j  = visibleHostiles.length; --j >= 0;) {
                RobotInfo hostile = visibleHostiles[j];
				if (!CombatUtils.isMilitaryUnit(hostile.type)) continue;
				int distSq = hostile.location.distanceSquaredTo(dirLoc);
				if (distSq < smallestDistSq) {
					smallestDistSq = distSq;
				}
			}
			if (smallestDistSq > bestDistSq) {
				bestDistSq = smallestDistSq;
				bestRetreatDir = dir;
			}
		}
		if (bestRetreatDir != null) {
            rc.setIndicatorString("Backing: " + bestRetreatDir);
			rc.move(bestRetreatDir);
            currentLocation = rc.getLocation();
			return true;
		}
		return false;
	}

    private static void attackCloud() throws GameActionException{
        if (!rc.isActionReady()) return;
        cloudLocations = rc.senseNearbyCloudLocations(UNIT_TYPE.actionRadiusSquared);
        MapLocation cloudAttackLocation = null;
        if (cloudLocations.length > 0 && rc.senseMapInfo(rc.getLocation()).getCooldownMultiplier(MY_TEAM) <= 1){
            int bestDistance = 4;
            int count = 0;
            for (int i = cloudLocations.length; --i >= 0;){
                int curDistance = rc.getLocation().distanceSquaredTo(cloudLocations[i]);
                if (curDistance == bestDistance) count++;
                else if (curDistance > bestDistance){
                    count = 1;
                    bestDistance = curDistance;
                }
                else{
                    continue;
                }
                if (rng.nextInt(count) == 0){
                    cloudAttackLocation = cloudLocations[i];
                }
            }
        }
        if (cloudAttackLocation != null && rc.canAttack(cloudAttackLocation)){
            destinationFlag += " aC " + cloudAttackLocation + " clen " + cloudLocations.length;
            rc.attack(cloudAttackLocation);
        }
    }

    // private static Direction safestDirTowards(MapLocation destination) throws GameActionException{
        
    // }
}
