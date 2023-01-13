package HardlyWorkingBot;

import HardlyWorkingBot.Comms.COMM_TYPE;
import battlecode.common.*;

public class BotLauncher extends CombatUtils{

    private enum Status {
        BORN, // State at start of first turn
        EXPLORE, // No directions given
        MARCHING, // Going to a location given by comms
        PURSUING,
        ENGAGING,
        FLANKING,
        GUARDING,
        RETREATING,
        HIDING, // In the clouds
        STANDOFF,
    }

    private static Status launcherState;

    private static RobotInfo[] visibleEnemies;
    private static RobotInfo[] inRangeEnemies;
    private static int vNonHQEnemies = 0;
    private static int inRNonHQEnemies = 0;
    private static boolean standOff = false;

    public static void initLauncher() throws GameActionException{
        launcherState = Status.BORN;
        updateVision();
        if (currentDestination == null) {
            currentDestination = CENTER_OF_THE_MAP;
            launcherState = Status.EXPLORE;
        }
        else{
            launcherState = Status.MARCHING;
        }
    }

    public static void runLauncher() throws GameActionException{
        updateVision();
        standOff = false;
        opportunisticCombatDestination();

        tryToMicro();
        updateVision();

        if (sendCombatLocation(visibleEnemies));
        else {
            findNewCombatLocation();
        }
        if (visibleEnemies.length == 0 && rc.isMovementReady()) {
            if (pathing.getCurrentDestination() == null && currentDestination != null) {
                pathing.setNewDestination(currentDestination);
            }
            pathing.moveToDestination(); // 2700 Bytecodes
            launcherState = Status.MARCHING;
            updateVision();
        }
        if (rc.isActionReady() && inRangeEnemies.length > 0) {
            chooseTargetAndAttack(inRangeEnemies);
            launcherState = Status.ENGAGING;
        }
    }

    private static void updateVision() throws GameActionException {
        visibleEnemies = rc.senseNearbyRobots(UNIT_TYPE.visionRadiusSquared, ENEMY_TEAM);
        inRangeEnemies = rc.senseNearbyRobots(UNIT_TYPE.actionRadiusSquared, ENEMY_TEAM);
        vNonHQEnemies = 0;
        inRNonHQEnemies = 0;
        for (int i = visibleEnemies.length; --i >= 0;) {
            if (visibleEnemies[i].type != RobotType.HEADQUARTERS) {
                vNonHQEnemies++;
            }
        }
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
            return 100000; // Instakill
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
		if (bestTarget != null) {
            rc.attack(bestTarget.location);
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
				rc.setIndicatorString("Trying to help");
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
            rc.setIndicatorString("Trying to attack production unit");
            return true;
        }
		return false;
	}

    private static boolean tryMoveToEngageOutnumberedEnemy(RobotInfo[] visibleHostiles, RobotInfo closestHostile) throws GameActionException {
        if(closestHostile == null) return false;
        MapLocation closestHostileLocation = closestHostile.location;
        int numNearbyHostiles = 0;
		for (int i = visibleHostiles.length; --i >= 0;) {
			if (visibleHostiles[i].type.canAttack()) {
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
			if (Movement.tryMoveInDirection(closestHostile.location))
                return true;
            else if(numNearbyAllies >= 1.5 * numNearbyHostiles && Movement.tryForcedMoveInDirection(closestHostile.location)){
                return true;
            }
            else {
                standOff = true;
                return false;
            }
		}
		return false;
	}

    private static boolean retreatIfOutnumbered(RobotInfo[] visibleHostiles) throws GameActionException {
		RobotInfo closestHostileThatAttacksUs = null;
		int closestDistSq = Integer.MAX_VALUE;
		int numHostilesThatAttackUs = 0;
		for (int i = visibleHostiles.length; --i >= 0;) {
            RobotInfo hostile = visibleHostiles[i];
			if (hostile.type.canAttack()) {
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
			if (ally.type.canAttack()) {
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
		for (int i = visibleHostiles.length; --i >= 0;) {
            RobotInfo hostile = visibleHostiles[i];
			if (!hostile.type.canAttack()) continue;			
			retreatTarget = retreatTarget.add(hostile.location.directionTo(rc.getLocation()));
		}
		if (!rc.getLocation().equals(retreatTarget)) {
			// Direction retreatDir = rc.getLocation().directionTo(retreatTarget);
			return Movement.tryForcedMoveInDirection(retreatTarget);
		}
		return false;
	}

    private static boolean tryToMicro() throws GameActionException {
        if (vNonHQEnemies == 0) { // TODO: Either wait or get out of any possible Watchtower range. Skip if charging
            return false;
        }

        if (rc.isMovementReady() && retreatIfOutnumbered(visibleEnemies)){
            launcherState = Status.RETREATING;
            return true;
        }

        if (rc.isActionReady()){
            if (inRNonHQEnemies > 0) {
                chooseTargetAndAttack(inRangeEnemies);
                launcherState = Status.ENGAGING;
            }
            else if (rc.isMovementReady() && vNonHQEnemies > 0) {
                RobotInfo closestHostile = getClosestUnitWithCombatPriority(visibleEnemies);
                if(tryMoveToHelpAlly(closestHostile)) {
                    launcherState = Status.FLANKING;
                    return true;
                }
                if(tryMoveToAttackProductionUnit(closestHostile)) {
                    launcherState = Status.PURSUING;
                    return true;
                }
            }
        }
        if (rc.isMovementReady()){
            // Most important function
            if (inRNonHQEnemies > 0 && tryToBackUpToMaintainMaxRangeSoldier(visibleEnemies)) {
                launcherState = Status.FLANKING;
                return true;
            }
            RobotInfo closestHostile = getClosestUnitWithCombatPriority(visibleEnemies);
            if (tryMoveToHelpAlly(closestHostile)) {
                launcherState = Status.FLANKING;
                return true; // Maybe add how many turns of attack cooldown here and how much damage being taken?
            }
            if (tryMoveToEngageOutnumberedEnemy(visibleEnemies, closestHostile)) {
                launcherState = Status.FLANKING;
                return true;
            }
            if (tryMoveToAttackProductionUnit(closestHostile)) {
                launcherState = Status.PURSUING;
                return true;
            }
        }
        return false;
    }

    public static boolean sendCombatLocation(RobotInfo[] visibleHostiles) throws GameActionException{
        if (vNonHQEnemies > 0){
			RobotInfo closestHostile = getClosestUnitWithCombatPriority(visibleHostiles);
            if (closestHostile == null) return false;
			if (!standOff){ 
                currentDestination = closestHostile.getLocation();
                // TODO: Check if this can pushed out of loop
                if (rc.canWriteSharedArray(0, 0))
				    Comms.writeAndOverwriteLesserPriorityMessage(Comms.COMM_TYPE.COMBAT, closestHostile.getLocation(), Comms.SHAFlag.COMBAT_LOCATION);
            }
            return true;
        }
        return false;
    }

    // If our current destination has no enemies left, move to the nearest new location with combat
    private static boolean findNewCombatLocation() throws GameActionException{
        if (currentDestination == null || (visibleEnemies.length == 0 && rc.getLocation().distanceSquaredTo(currentDestination) <= UNIT_TYPE.visionRadiusSquared)){
            MapLocation combatLocation = Comms.findNearestLocationOfThisTypeOutOfVision(rc.getLocation(), Comms.COMM_TYPE.COMBAT, Comms.SHAFlag.COMBAT_LOCATION);
            if (combatLocation != null) currentDestination = combatLocation;
            return true;
        }
        return false;
    }

    private static void simpleAttack() throws GameActionException{
        if (inRNonHQEnemies > 10 && rc.isActionReady()){
            chooseTargetAndAttack(inRangeEnemies);
        }
    }

    /**
    * This will try to update the destination of the soldier so as to not make it go away from fights to a predetermined location.
    */
    private static void opportunisticCombatDestination() throws GameActionException{
        MapLocation nearestCombatLocation = Comms.findNearestLocationOfThisTypeOutOfVision(rc.getLocation(), Comms.COMM_TYPE.COMBAT, Comms.SHAFlag.COMBAT_LOCATION);
        if (nearestCombatLocation != null){ 
            if (currentDestination == null || (!rc.canSenseLocation(currentDestination) && 
                rc.getLocation().distanceSquaredTo(currentDestination) > rc.getLocation().distanceSquaredTo(nearestCombatLocation))){
                currentDestination = nearestCombatLocation;
                launcherState = Status.MARCHING;
            }
        }
    }

}
