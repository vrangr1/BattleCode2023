package AnExploreBot;

import battlecode.common.*;

public class CombatUtils extends Utils{

    public static boolean isMilitaryUnit(RobotType type){
        switch (type){
            case LAUNCHER:
            case DESTABILIZER:
                return true;
            default:
                return false;
        }
    }

    public static boolean isMilitaryUnit(RobotInfo bot){
        return isMilitaryUnit(bot.type);
    }

    public static boolean hasMilitaryUnit(RobotInfo[] bots){
        for (int i = bots.length; --i >= 0;)
            if (isMilitaryUnit(bots[i])) return true;
        return false;
    }

	public static boolean isMaxHealth(RobotInfo bot){
		return (bot.getHealth() == bot.type.getMaxHealth());
	}

	public static boolean isBotInjured(RobotInfo bot){
		double frac = 1.0d;
		return ((double)bot.health < frac * ((double)bot.type.getMaxHealth()));
	}

    public static int militaryCount(RobotInfo[] visibleRobots){
        int count = 0;
        for (int i = visibleRobots.length; --i >= 0;) {
            if (isMilitaryUnit(visibleRobots[i].type)) count++;
        }
        return count;
    }

    public static int militaryAndHeadquarterCount(RobotInfo[] visibleRobots){
        int count = 0;
        for (int i = visibleRobots.length; --i >= 0;) {
            if (isMilitaryUnit(visibleRobots[i].type) || visibleRobots[i].type == RobotType.HEADQUARTERS) count++;
        }
        return count;
    }

    public static RobotInfo getClosestUnitWithCombatPriority(RobotInfo[] units) {
        if (units.length == 0) return null;
		RobotInfo closestUnit = null;
        RobotInfo closestCombatUnit = null;
		int minDistSq = Integer.MAX_VALUE;
        int minCombatDistSq = Integer.MAX_VALUE;
        int distSq = 0;
        MapLocation lCR = rc.getLocation();
		for (int i = units.length; --i >= 0;) {
			distSq = lCR.distanceSquaredTo(units[i].location);
			if (distSq < minDistSq) {
				minDistSq = distSq;
				closestUnit = units[i];
			}
            if (isMilitaryUnit(units[i].getType()) && distSq < minCombatDistSq) {
				minCombatDistSq = distSq;
				closestCombatUnit = units[i];
			}
		}
        if (closestCombatUnit != null) return closestCombatUnit;
		else return closestUnit;
	}

    public static RobotInfo getClosestNonHQUnitWithCombatPriority(RobotInfo[] units) {
        if (units.length == 0) return null;
		RobotInfo closestUnit = null;
        RobotInfo closestCombatUnit = null;
		int minDistSq = Integer.MAX_VALUE;
        int minCombatDistSq = Integer.MAX_VALUE;
        int distSq = 0;
        MapLocation lCR = rc.getLocation();
		for (int i = units.length; --i >= 0;) {
            if (units[i].type == RobotType.HEADQUARTERS) continue;
			distSq = lCR.distanceSquaredTo(units[i].location);
			if (distSq < minDistSq) {
				minDistSq = distSq;
				closestUnit = units[i];
			}
            if (isMilitaryUnit(units[i].getType()) && distSq < minCombatDistSq) {
				minCombatDistSq = distSq;
				closestCombatUnit = units[i];
			}
		}
        if (closestCombatUnit != null) return closestCombatUnit;
		else return closestUnit;
	}

    public static RobotInfo getClosestMilitaryUnit(RobotInfo[] units) {
        RobotInfo closestCombatUnit = null;
        int minCombatDistSq = Integer.MAX_VALUE;
        int distSq = 0;
        MapLocation lCR = rc.getLocation();
		for (int i = units.length; --i >= 0; ) {
            if (!isMilitaryUnit(units[i].getType())) continue;
			distSq = lCR.distanceSquaredTo(units[i].location);
            if (distSq < minCombatDistSq) {
				minCombatDistSq = distSq;
				closestCombatUnit = units[i];
			}
		}
        return closestCombatUnit;
	}

    public static boolean sendGenericCombatLocation(RobotInfo[] visibleEnemies) throws GameActionException{
			RobotInfo closestHostile = getClosestNonHQUnitWithCombatPriority(visibleEnemies);
            if (closestHostile == null) return false;
            currentDestination = closestHostile.getLocation();
            if (rc.canWriteSharedArray(0, 0)){
                Comms.writeAndOverwriteLesserPriorityMessage(Comms.COMM_TYPE.COMBAT, closestHostile.getLocation(), Comms.SHAFlag.COMBAT_LOCATION);
                return true;
            }
        return false;
    }

    public static MapLocation tryToBackUpFromEnemyHQ(RobotInfo enemyHQ) throws GameActionException {
		int closestHostileDistSq = Integer.MAX_VALUE;
        MapLocation lCR = rc.getLocation();
		if (enemyHQ.type != RobotType.HEADQUARTERS) return null;
		closestHostileDistSq = lCR.distanceSquaredTo(enemyHQ.location);
		
        // We don't want to get out of our max range
		if (closestHostileDistSq > rc.getType().actionRadiusSquared) return null;
		
		MapLocation bestRetreatLoc = null;
		int bestDistSq = closestHostileDistSq;

		for (Direction dir : directions) {
			if (!rc.canMove(dir)) continue;
			MapLocation dirLoc = lCR.add(dir);
			int distSq = enemyHQ.location.distanceSquaredTo(dirLoc);
			if (distSq > bestDistSq) {
				bestDistSq = distSq;
				bestRetreatLoc = dirLoc;
			}
		}
		return bestRetreatLoc;
	}

    public static Direction retreatMoveToSurroundingCloud(RobotInfo[] visibleEnemies) throws GameActionException {
        if (visibleEnemies.length == 0) return null;
        MapLocation lCR = rc.getLocation();
        int attackEnemiesCount = Integer.MAX_VALUE;
        Direction bestRetreatDir = null;
        for (Direction dir : directions) {
            if (!rc.canMove(dir)) continue;
            MapLocation dirLoc = lCR.add(dir);
            if (!rc.senseCloud(dirLoc)) continue;
            int currentEnemies = 0;
            for (int j  = visibleEnemies.length; --j >= 0;) {
                RobotInfo hostile = visibleEnemies[j];
                if (!hostile.type.canAttack()) continue;
                int distSq = hostile.location.distanceSquaredTo(dirLoc);
                if (distSq <= hostile.type.actionRadiusSquared) 
                    currentEnemies++;
            }
            if (currentEnemies < attackEnemiesCount) {
                attackEnemiesCount = currentEnemies;
                bestRetreatDir = dir;
            }
        }
        return bestRetreatDir;
    }

}
