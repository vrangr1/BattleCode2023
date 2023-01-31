package APostQualsBot;

import battlecode.common.*;
import APostQualsBot.path.Nav;
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
        HEALING,
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
    public static int vNonHQCombatAllies = 0;
    private static RobotInfo enemyHQ = null;
    private static boolean standOff = false;
    private static RobotInfo prevTurnHostile = null;
    private static MapLocation circleLocation = null;
    private static int minCircleDistance = 10000;
    private static int maxCircleDistance = 10000;
    private static boolean isClockwise = true;
    private static int LOW_HEALTH_MARK = 30;
    private static int circlingCount;
    private static int CIRCLE_CHECK = 30;
    private static boolean inHealingState;
    private static MapLocation closestHealingIsland = null;
    private static MapLocation storedEnemyHQLoc = null;
    private static RobotInfo[] seenEnemyHQs;

    public static void initLauncher() throws GameActionException{
        launcherState = Status.BORN;
        inHealingState = false;
        seenEnemyHQs = new RobotInfo[Comms.getHeadquartersCount()];
        setBaseDestination();
    }


    public static void runLauncher() throws GameActionException{
        if (TRACKING_LAUNCHER_COUNT) Comms.incrementRobotCount(RobotType.LAUNCHER);
        
        updateVision();
        previousTurnResolution();
        findAndWriteWellLocationsToComms();
        bytecodeCheck(); //0
        if (vNonHQEnemies == 0) {
            manageHealingState();
            if (inHealingState){
                tryToHealAtIsland();
            }
            else{
                boolean seekingIsland = false;
                seekingIsland = seekEnemyIslandInVision(); // [CUR_STATE] -> [ISLAND_WORK|EXPLORE]
                closerCombatDestination(); // [CUR_STATE] -> [CUR_STATE|MARCHING|EXPLORE]
                if (!seekingIsland && MAP_SIZE >= 1200){
                    if (doIdling()){
                        rc.setIndicatorString("Idling");
                        return;
                    }
                }
            }
        }
        bytecodeCheck(); //1
        tryToMicro();
        if (!rc.isActionReady() || !rc.isMovementReady()) // No need to update vision if you haven't moved or attacked
            updateVision();
        bytecodeCheck(); //2
        circleEnemyHQ();

        if (sendCombatLocation());
        else {
            findNewCombatLocation();
        }

        if (!rc.isMovementReady() && rc.getRoundNum() < 120 + BIRTH_ROUND){
            midLineSymmetryCheck();
        }

        bytecodeCheck(); //3
        moveAfterNonMovingCombat(); // [CUR_STATE] -> [CUR_STATE] (Only works with [MARCHING|ISLAND_WORK|EXPLORE])
        if (rc.isActionReady()) {
            updateInRangeEnemiesVision();
            bytecodeCheck(); //6
            if (inRNonHQEnemies > 0) {
                chooseTargetAndAttack(inRangeEnemies);
            }
            else if (Clock.getBytecodesLeft() > 750) {
                attackCloud();
            }
        }

        rc.setIndicatorString(launcherState.toString() + " " + currentDestination + "|" + destinationFlag + 
            " |Move" + rc.isMovementReady());
    }

    private static void setBaseDestination() throws GameActionException {
        currentDestination = Comms.findNearestEnemyHeadquarterLocation();
        if (currentDestination.equals(visitedHQList[visitedHQIndex % Comms.getHeadquartersCount()])){
            currentDestination = CENTER_OF_THE_MAP;
        }
        for (int i = rememberedEnemyHQLocations.length; --i >= 0;){
            if (!mapSymmetry[i] || !Symmetry.checkIfSymmetry(Symmetry.SYMMETRY.values()[i])){
                Symmetry.removeSymmetry(Symmetry.SYMMETRY.values()[i], "3");
                mapSymmetry[i] = false;
                rememberedEnemyHQLocations[i] = null;
            }
            if (enemyHQ == null){
                for (int j = Comms.getHeadquartersCount(); --j >= 0;){
                    MapLocation symmetricEnemyHQ = Symmetry.returnEnemyOnSymmetry(Symmetry.SYMMETRY.values()[i], alliedHQLocs[j]);
                    if (symmetricEnemyHQ == null) continue;
                    if (symmetricEnemyHQ.distanceSquaredTo(rc.getLocation()) <= UNIT_TYPE.visionRadiusSquared &&
                        rc.canSenseLocation(symmetricEnemyHQ)){
                        rememberedEnemyHQLocations[i] = null;
                        mapSymmetry[i] = false;
                        if (Symmetry.checkIfSymmetry(Symmetry.SYMMETRY.values()[i])){
                            Symmetry.removeSymmetry(Symmetry.SYMMETRY.values()[i], "1");
                        } 
                    }  
                }
            }
        }
        if (currentDestination.equals(CENTER_OF_THE_MAP)){
            currentDestination = Symmetry.defaultEnemyLocation();
        }
        storedEnemyHQLoc = currentDestination;
        pathing.setNewDestination(currentDestination);
        destinationFlag += "sBD" + currentDestination;
        launcherState = Status.MARCHING;
    }

    private static boolean doIdling() throws GameActionException {
        if (vNonHQEnemies == 0 && rc.getRoundNum() <= BIRTH_ROUND + 1){
            militaryAlliesInVision();
            if (vNonHQCombatAllies == 0){
                return true;
            }
        }
        return false;
    }

    private static void previousTurnResolution() throws GameActionException {
        standOff = false;
        destinationFlag = "";
        cloudLocations = null;
        simplePursuit();
        if (launcherState != Status.PURSUING){
            prevTurnHostile = null;
        }
    }

    private static void midLineSymmetryCheck() throws GameActionException{
        for (int i = Symmetry.SYMMETRY.values().length; --i >= 0;) {
            
            if (Clock.getBytecodesLeft() > 2100 && mapSymmetry[i] && !Symmetry.checkThisSymmetry(Symmetry.SYMMETRY.values()[i])){
                Symmetry.removeSymmetry(Symmetry.SYMMETRY.values()[i], "3");
                mapSymmetry[i] = false;
                rememberedEnemyHQLocations[i] = null;
                if (currentDestination == null || currentDestination.equals(storedEnemyHQLoc)){
                    destinationFlag += "mLSC";
                    setBaseDestination();
                    break;
                }
            }
        }
    }

    // Minimal use
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

    // Unused
    private static void lowHealthCircle() throws GameActionException {
        if (vNonHQEnemies == 0 && rc.getHealth() <= LOW_HEALTH_MARK && rc.isMovementReady() && rc.getRoundNum() > 150){
            setCircleDestination(parentHQLocation, 10, 100);
        }
    }

    // Unused
    private static void setCircleDestination(MapLocation circleDestination, int minDist, int maxDist) throws GameActionException {
        if (rc.getLocation().distanceSquaredTo(circleDestination) <= maxDist && launcherState != Status.ISLAND_WORK){
            if (launcherState != Status.CIRCLING && circleLocation != circleDestination)
                setCircle(circleDestination, minDist, maxDist);
            circleWorks();
            launcherState = Status.CIRCLING;
        }
        else{
            currentDestination = circleDestination;
            pathing.setAndMoveToDestination(currentDestination);
            launcherState = Status.MARCHING;
        }
    }

    // Find enemy islands in vision and move towards them for anchor removal
    private static boolean seekEnemyIslandInVision() throws GameActionException {
        if (launcherState == Status.ISLAND_WORK) {
            int islandId = rc.senseIsland(rc.getLocation());
            if (islandId != -1 && rc.senseTeamOccupyingIsland(islandId) == ENEMY_TEAM){
                return true;
            }
        }

        int[] nearbyIslands = rc.senseNearbyIslands();
        MapLocation nearestLoc = null;
        int nearestDist = -1, curDist;
        for (int i = nearbyIslands.length; --i >= 0;){
            int islandId = nearbyIslands[i];
            // Skip neutral/friendly islands
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
            destinationFlag += "sEI1";
            sendIslandLocation(nearestLoc);
            launcherState = Status.ISLAND_WORK;
            return true;
        }
        else {
            destinationFlag += "sEI2";
            launcherState = Status.MARCHING;
            return false;
        }
    }

    private static void moveAfterNonMovingCombat() throws GameActionException {
        if (vNonHQEnemies != 0) return;
        if ((rc.getRoundNum() + 1)% 2 == 0 && rc.getHealth() == UNIT_TYPE.getMaxHealth()) return;
        if (rc.isMovementReady()) {
            if (currentDestination != null) {
                pathing.setNewDestination(currentDestination);
            }
            bytecodeCheck(); //4
            if (launcherState == Status.MARCHING || launcherState == Status.ISLAND_WORK) {
                if (currentDestination.equals(circleLocation) && rc.getLocation().distanceSquaredTo(circleLocation) <= maxCircleDistance){
                    return;
                }
                pathing.moveToDestination();
                if (rc.isMovementReady()){
                    destinationFlag += "| mANMC2  " + circleLocation;
                    Nav.goTo(currentDestination);
                }
                else{
                    destinationFlag += "| mANMC1  " + circleLocation;
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
        if (visibleEnemies.length > 0)
            updateInRangeEnemiesVision();
        else{
            inRangeEnemies = null;
            inRNonHQEnemies = 0;
        }
    }

    public static void militaryAlliesInVision() throws GameActionException{
        visibleAllies = rc.senseNearbyRobots(UNIT_TYPE.visionRadiusSquared, MY_TEAM);
        vNonHQCombatAllies = 0;
        for (int i = visibleAllies.length; --i >= 0;) {
            if (isMilitaryUnit(visibleAllies[i].type)) {
                vNonHQCombatAllies++;
                return;
            }
        }
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
                // destinationFlag+="|eHQ" + enemyHQ.location;
                updateSeenEnemyHQs(enemyHQ);
                if (rc.canWriteSharedArray(0, 0)){
                    Comms.writeEnemyHeadquarterLocation(enemyHQ.location);
                }
            }
        }
        seenEnemyHQInNonCloudVision();
    }

    public static void updateSeenEnemyHQs(RobotInfo giveEnemyHQ) throws GameActionException{
        for (int i = 0; i < seenEnemyHQs.length; i++){
            if (seenEnemyHQs[i] != null && (seenEnemyHQs[i].location).equals(giveEnemyHQ.location)) 
                return;
            if (seenEnemyHQs[i] == null){
                seenEnemyHQs[i] = giveEnemyHQ;
                return;
            }
        }
    }

    public static void seenEnemyHQInNonCloudVision() throws GameActionException{
        currentLocation = rc.getLocation();
        if (enemyHQ != null) return;
        for (int i = seenEnemyHQs.length; --i >= 0;){
            if (seenEnemyHQs[i] != null && currentLocation.distanceSquaredTo(seenEnemyHQs[i].location) <= UNIT_TYPE.visionRadiusSquared
            && !rc.canSenseLocation(seenEnemyHQs[i].location)){
                enemyHQInVision++;
                enemyHQ = seenEnemyHQs[i];
                return;
            }
        }
    }

    // public static nearbyHQInClouds() throws Game

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
				if (alliesAroundHostile[i].location.distanceSquaredTo(closestHostileLocation) <= alliesAroundHostile[i].type.visionRadiusSquared) {
					allyIsFighting = true;
					break;
				}
			}
		}
        destinationFlag += "mha1-";
		if (allyIsFighting) {
            Direction bestDir = Movement.forwardCombatMovement(visibleEnemies, rc.getLocation().directionTo(closestHostileLocation), false);
            destinationFlag += bestDir;
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
        if (!rc.isMovementReady() || Movement.tryFlagMoveInDirection(closestHostile.location, true)) {
            destinationFlag += "|tM2APU";
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
		RobotInfo[] visibleAllies = rc.senseNearbyRobots(closestHostile.location, UNIT_TYPE.visionRadiusSquared, MY_TEAM);
		int numNearbyAllies = 1; // Counts ourself
		for (int i = visibleAllies.length; --i >= 0;) {
			if (isMilitaryUnit(visibleAllies[i].type)) {
				numNearbyAllies += 1;
			}
		}
		
		if (numNearbyAllies >= numNearbyHostiles || (numNearbyHostiles == 1 && rc.getHealth() >= closestHostile.health - UNIT_TYPE.damage)) {
            if ((closestHostile.health <= UNIT_TYPE.damage) && rc.isActionReady()){
                Direction bestDir = Movement.forwardCombatMovement(visibleEnemies, rc.getLocation().directionTo(closestHostile.location), false);
                if (bestDir != null) {
                    rc.move(bestDir);
                    launcherState = Status.FLANKING;
                    return true;
                }
            }
            if (rc.isMovementReady()){
                standOff = true;
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
            else if (ally.type == RobotType.CARRIER) {
                WellInfo[] nearbyWells = rc.senseNearbyWells();
                if (nearbyWells.length > 0) {
                    destinationFlag+="Ter_ret";
                    return false;
                }
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
			Direction bestDir = Movement.combatMovement(visibleEnemies, rc.getLocation().directionTo(retreatTarget), false);
            destinationFlag += "rio" + bestDir;
            if (bestDir != null) {
                rc.move(bestDir);
                launcherState = Status.FLANKING;
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
            destinationFlag += "-ret";
            return true;
        }

        if (rc.isActionReady()){
            if (inRNonHQEnemies > 0) {
                chooseTargetAndAttack(inRangeEnemies);
            }
            else if (rc.isMovementReady() && vNonHQEnemies > 0) {
                RobotInfo closestHostile = getClosestUnitWithCombatPriority(visibleEnemies);
                if(rc.getHealth() >= 60 && tryMoveToHelpAlly(closestHostile)) {
                    destinationFlag += "ally1" + closestHostile.location.toString();
                    return true;
                }
                if(tryMoveToAttackProductionUnit(closestHostile)) {
                    destinationFlag += "prod1";
                    return true;
                }
            }
        }
        if (rc.isMovementReady()){
            RobotInfo closestHostile = getClosestUnitWithCombatPriority(visibleEnemies);
            // Most important function
            if (inRNonHQEnemies > 0 && tryToBackUpToMaintainMaxRangeLauncher(visibleEnemies)) {
                destinationFlag += "max1";
                launcherState = Status.FLANKING;
                return true;
            }
            tryMoveToEngageOutnumberedEnemy(closestHostile);
            if (!inHealingState && tryMoveToHelpAlly(closestHostile)) {
                destinationFlag += " ally2";
                return true; // Maybe add how many turns of attack cooldown here and how much damage being taken?
            }
            if (!inHealingState && tryMoveToAttackProductionUnit(closestHostile)) {
                destinationFlag += " prod2";
                return true;
            }
        }
        return false;
    }

    private static void manageHealingState() {
        if (rc.getHealth() <= rc.getType().getMaxHealth() * 3.0/10.0) {
            inHealingState = true;
            return;
        }
        else if (rc.getHealth() > 3.0/4.0 * rc.getType().getMaxHealth()) {
            if (launcherState == Status.HEALING){
                currentDestination = null;  
                launcherState = Status.MARCHING;
            }
            inHealingState = false;
            return;
        }
    }

    private static boolean tryToHealAtIsland() throws GameActionException {		
		closestHealingIsland = Comms.findNearestLocationOfThisType(rc.getLocation(), Comms.COMM_TYPE.OUR_ISLANDS, Comms.SHAFlag.OUR_ISLAND);
        int islandId = rc.senseIsland(rc.getLocation());
        if (islandId != -1 && rc.senseTeamOccupyingIsland(islandId) == MY_TEAM){
            launcherState = Status.HEALING;
            return true;
        }
        int[] nearbyIslands = rc.senseNearbyIslands();
        MapLocation nearestLoc = null;
        int nearestDist = -1, curDist;
        for (int i = nearbyIslands.length; --i >= 0;){
            islandId = nearbyIslands[i];
            // Skip neutral/enemy islands
            if (rc.senseTeamOccupyingIsland(islandId) != MY_TEAM){
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
        if (nearestDist != -1 && (closestHealingIsland == null || nearestDist <= rc.getLocation().distanceSquaredTo(closestHealingIsland))){
            closestHealingIsland = nearestLoc;
        }

		destinationFlag += "|trHAl "+closestHealingIsland+"|";
		if (closestHealingIsland == null){
            if (launcherState != Status.MARCHING && launcherState != Status.CIRCLING){
                currentDestination = null;
                launcherState = Status.MARCHING;
            }
            inHealingState = false;
            return false;
        }
        else if(!rc.getLocation().equals(closestHealingIsland)){
            currentDestination = closestHealingIsland;
            launcherState = Status.HEALING; //TODO: Change this to healing
            pathing.setAndMoveToDestination(currentDestination);
        }
        else{
            launcherState = Status.HEALING;
        }    
		return true;
	}

    public static boolean sendCombatLocation() throws GameActionException{
        if (vNonHQEnemies > 0 && rc.getLocation().distanceSquaredTo(parentHQLocation) > RobotType.HEADQUARTERS.actionRadiusSquared){
			RobotInfo closestHostile = getClosestUnitWithCombatPriority(visibleEnemies);
            if (closestHostile == null) return false;
			if (!standOff){ // The idea is to prevent standoffs from flooding comms with local info
                currentDestination = closestHostile.getLocation();
                if (rc.canWriteSharedArray(0, 0) && !Comms.findIfLocationAlreadyPresent(closestHostile.getLocation(), Comms.COMM_TYPE.COMBAT, Comms.SHAFlag.COMBAT_LOCATION))
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
        if (vNonHQEnemies == 0){
            if (currentDestination != null){
                if (launcherState == Status.ISLAND_WORK || launcherState == Status.HEALING) return false;
                if (!rc.canSenseLocation(currentDestination)) return false;
                int dist = rc.getLocation().distanceSquaredTo(currentDestination);
                if (MAP_SIZE <= 900 && dist > UNIT_TYPE.actionRadiusSquared) return false;
                if (dist > UNIT_TYPE.visionRadiusSquared) return false; //TODO: Remove this line
                Comms.wipeThisLocationFromChannels(Comms.COMM_TYPE.COMBAT, Comms.SHAFlag.COMBAT_LOCATION, currentDestination);
            }
            MapLocation combatLocation = Comms.findNearestLocationOfThisTypeOutOfVision(rc.getLocation(), Comms.COMM_TYPE.COMBAT, Comms.SHAFlag.COMBAT_LOCATION);
            if (combatLocation != null){
                currentDestination = combatLocation;
                destinationFlag += "|fNCL1";
                pathing.setNewDestination(currentDestination);
                launcherState = Status.MARCHING;
                return true;
            }
            else if (currentDestination == null || (currentDestination.distanceSquaredTo(rc.getLocation()) <= UNIT_TYPE.visionRadiusSquared 
                && enemyHQ == null && rc.canSenseLocation(currentDestination))){
                destinationFlag += "|fNCL2";
                setBaseDestination();
                return true;
            }
        }
        return false;
    }

    /**
    * This will try to update the destination of the soldier so as to not make it go away from closer fights
    */
    private static void closerCombatDestination() throws GameActionException{
        if (launcherState == Status.ISLAND_WORK || launcherState == Status.HEALING || launcherState == Status.CIRCLING) return;
        MapLocation nearestCombatLocation = Comms.findNearestLocationOfThisTypeOutOfVision(rc.getLocation(), Comms.COMM_TYPE.COMBAT, Comms.SHAFlag.COMBAT_LOCATION);
        if (currentDestination == null || (nearestCombatLocation!= null && !rc.canSenseLocation(currentDestination) && 
            rc.getLocation().distanceSquaredTo(currentDestination) > rc.getLocation().distanceSquaredTo(nearestCombatLocation))){
                currentDestination = nearestCombatLocation;
                destinationFlag += "cCD";
                launcherState = Status.MARCHING;
        }
    }

    private static void circleEnemyHQ() throws GameActionException{
        if (launcherState == Status.ISLAND_WORK) return; //TODO: Add healing
        if (enemyHQ == null) return;
        if (enemyHQInVision >= visibleEnemies.length && enemyHQInVision > 0  && (currentDestination == null || currentDestination.equals(enemyHQ.location))){
            if (circlingCount >= CIRCLE_CHECK && Comms.getHeadquartersCount() > 1 && rc.getID() % Comms.getHeadquartersCount() != 0){
                visitedHQList[++visitedHQIndex % Comms.getHeadquartersCount()] = circleLocation;
                circlingCount = 0;
                circleLocation = null;
                destinationFlag += "cE0";
                setBaseDestination();
                launcherState = Status.MARCHING;
            }
            if (enemyHQ.location.equals(visitedHQList[visitedHQIndex % Comms.getHeadquartersCount()])){
                return;
            }
            visibleAllies = rc.senseNearbyRobots(UNIT_TYPE.visionRadiusSquared, MY_TEAM);
            if (launcherState != Status.CIRCLING && circleLocation != enemyHQ.location){
                setCircle(enemyHQ.location, 9, 20);
                circlingCount = 0;
                launcherState = Status.CIRCLING;
                destinationFlag += "cE2";
            }
            circlingCount++;
            destinationFlag += "|cE1" + isClockwise + circlingCount;
            circleWorks();
        }
    }

    private static void setCircle(MapLocation cirCenter, int minDist, int maxDist) throws GameActionException{
        circleLocation = cirCenter;
        isClockwise = true;
        // decideRotationDirection(rc.getLocation().directionTo(circleLocation));
        minCircleDistance = minDist;
        maxCircleDistance = maxDist;
    }

    private static void circleWorks() throws GameActionException{
        Direction dirToCenter = rc.getLocation().directionTo(circleLocation);
        if (!rc.isMovementReady()) {
            launcherState = Status.CIRCLING;
            return;
        }
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
		if (rc.getHealth() >= 60)
            if (minHosDist > rc.getType().actionRadiusSquared) return false;
		
		Direction bestRetreatDir = null;
		double leastAttack = Double.MAX_VALUE;
        // double bestRubble = rc.senseMapInfo(rc.getLocation()).getCooldownMultiplier(ENEMY_TEAM) * 10.0;
		for (Direction dir : carDirections) {
			if (!rc.canMove(dir)) continue;
			MapLocation dirLoc = lCR.add(dir);
            Direction dirLocCurrentDir = rc.senseMapInfo(dirLoc).getCurrentDirection();
            double unitsAttacking = 0;
            if (enemyHQ != null && enemyHQ.location.distanceSquaredTo(dirLoc) <= RobotType.HEADQUARTERS.actionRadiusSquared){
                unitsAttacking += 4;
            }
            for (int i = visibleHostiles.length; --i >= 0;){
                if (isMilitaryUnit(visibleHostiles[i].type)){
                    int distToDirLoc = visibleHostiles[i].location.distanceSquaredTo(dirLoc);
                    if (distToDirLoc <= visibleHostiles[i].type.actionRadiusSquared){
                        unitsAttacking +=visibleHostiles[i].type.damage;
                    }
                    else if (distToDirLoc <= visibleHostiles[i].type.visionRadiusSquared){
                        unitsAttacking += visibleHostiles[i].type.damage/2;
                    }
                }
            }
            if (rc.senseCloud(dirLoc)){
                unitsAttacking += 4;
            }
            if (dirLocCurrentDir != Direction.CENTER && dirLocCurrentDir == dir.opposite()){
                unitsAttacking -= 5;
            }
            if (unitsAttacking < leastAttack){
                leastAttack = unitsAttacking;
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
        if (cloudLocations.length > 0 && rc.senseMapInfo(rc.getLocation()).getCooldownMultiplier(MY_TEAM) <= 1.2){
            double bestDistance = 4;
            int count = 0;
            for (int i = cloudLocations.length; --i >= 0;){
                double curDistance = rc.getLocation().distanceSquaredTo(cloudLocations[i]);
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
            destinationFlag += "|aC";
            rc.attack(cloudAttackLocation);
        }
    }

    // private static Direction safestDirTowards(MapLocation destination) throws GameActionException{
        
    // }
}
