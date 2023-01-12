package FirstBot;

import battlecode.common.*;

public class BotLauncher extends CombatUtils{

    private static RobotInfo[] visibleEnemies;
    private static RobotInfo[] inRangeEnemies;
    private static MapLocation finalDestination = null; 
    private static boolean standOff = false;

    public static void initLauncher() throws GameActionException{
        findNewCombatLocation();
        if (currentDestination == null) currentDestination = CENTER_OF_THE_MAP;
    }

    public static void runLauncher() throws GameActionException{
        updateVision();
        standOff = false;
        opportunisticCombatDestination();
;
        tryToMicro();
        updateVision();

        if (sendCombatLocation(visibleEnemies));
        else {
            findNewCombatLocation();
        }
        if (visibleEnemies.length == 0 && rc.isMovementReady()) {
            if (pathing.getCurrentDestination() == null) {
                pathing.setNewDestination(currentDestination);
            }
            pathing.moveToDestination();; // 2700 Bytecodes
            updateVision();
        }
        if (rc.isActionReady()){
            if (inRangeEnemies.length > 0) {
                chooseTargetAndAttack(inRangeEnemies);
            }
        }

        // TODO: Remove this Also try to move randomly.
        Direction dir = directions[rng.nextInt(directions.length)];
        if (rc.canMove(dir)) {
            rc.move(dir);
        }
    }

    private static void updateVision() throws GameActionException {
        visibleEnemies = rc.senseNearbyRobots(UNIT_TYPE.visionRadiusSquared, ENEMY_TEAM);
        inRangeEnemies = rc.senseNearbyRobots(UNIT_TYPE.actionRadiusSquared, ENEMY_TEAM);
    }

    private static double getEnemyScore(RobotInfo enemyUnit) throws GameActionException{
        RobotType enemyType = enemyUnit.type;
        int enemyHealth = enemyUnit.getHealth();
        if (enemyHealth <= 6 && enemyType != RobotType.HEADQUARTERS) return 100000; // Instakill
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
			return 0.22 /(enemyHealth * (10.0)); // Max= 0.22, Min = 0.005 Low priority
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
        if(closestHostile == null) return false;
		if (closestHostile.type.canAttack()) 
            return false;
	    pathing.setAndMoveToDestination(closestHostile.location);
        if (!rc.isMovementReady()){
            return true;
        } 
        else if (Movement.tryMoveInDirection(closestHostile.location)) {
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
				// if (!SMALL_MAP && visibleHostiles[i].location.distanceSquaredTo(closestHostileLocation) <= UNIT_TYPE.actionRadiusSquared) {
					numNearbyHostiles += 1;
				// }
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
        if (visibleEnemies.length == 0) { // TODO: Either wait or get out of any possible Watchtower range. Skip if charging
            return false;
        }

        if (rc.isMovementReady()){
            if(retreatIfOutnumbered(visibleEnemies)) return true;
            // if(retreatFromEnemyWatchTowerRange()) return true;
        }

        if (rc.isActionReady()){
            if (inRangeEnemies.length > 0) {
                chooseTargetAndAttack(inRangeEnemies);
            }
            else if (rc.isMovementReady() && visibleEnemies.length > 0) {
                RobotInfo closestHostile = getClosestUnitWithCombatPriority(visibleEnemies);
                if(tryMoveToHelpAlly(closestHostile)) return true;
                if(tryMoveToAttackProductionUnit(closestHostile)) return true;
            }
        }
        if (rc.isMovementReady()){
            // Most important function
            if (inRangeEnemies.length > 0 && tryToBackUpToMaintainMaxRangeSoldier(visibleEnemies)) return true; // Cant attack, try to move out
            RobotInfo closestHostile = getClosestUnitWithCombatPriority(visibleEnemies);
            if (tryMoveToHelpAlly(closestHostile)) return true; // Maybe add how many turns of attack cooldown here and how much damage being taken?
            if (tryMoveToEngageOutnumberedEnemy(visibleEnemies, closestHostile)) return true;
            if (tryMoveToAttackProductionUnit(closestHostile)) return true;
        }
        return false;
    }

    public static boolean sendCombatLocation(RobotInfo[] visibleHostiles) throws GameActionException{
        if (visibleHostiles.length != 0){
			RobotInfo closestHostile = getClosestUnitWithCombatPriority(visibleHostiles);
            if (closestHostile == null) return false;
			if (!standOff) currentDestination = closestHostile.location;
            if (closestHostile != null)
                if (!standOff && rc.canWriteSharedArray(0, 0))
				    Comms.writeAndOverwriteLesserPriorityMessage(Comms.COMM_TYPE.COMBAT, closestHostile.getLocation(), Comms.SHAFlag.COMBAT_LOCATION);
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
        if (inRangeEnemies.length > 10 && rc.isActionReady()){
            chooseTargetAndAttack(inRangeEnemies);
        }
    }

    /**
    * This will try to update the destination of the soldier so as to not make it go away from fights to a predetermined location.
    */
    private static void opportunisticCombatDestination() throws GameActionException{
        MapLocation nearestCombatLocation = Comms.findNearestLocationOfThisTypeOutOfVision(rc.getLocation(), Comms.COMM_TYPE.COMBAT, Comms.SHAFlag.COMBAT_LOCATION);
        if (nearestCombatLocation != null){ 
            if (currentDestination == null) currentDestination = nearestCombatLocation;
            else if (!rc.canSenseLocation(currentDestination) && rc.getLocation().distanceSquaredTo(currentDestination) > rc.getLocation().distanceSquaredTo(nearestCombatLocation)){
                currentDestination = nearestCombatLocation;
            }
        }
    }

}
