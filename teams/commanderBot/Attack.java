package commanderBot;

import battlecode.common.*;

public class Attack {

	//  Priorities:
	//	missiles with 1 hp
	//	enemies that can be killed in 1 shot
	//	closest enemy
	//	lowest hp enemy
	//	lowest hp missile
	//	closest missile
	public static void tryAttackClosestButKillIfPossible(RobotInfo[] enemies) throws GameActionException {
		if (enemies.length == 0)
			return;

		int minDist = 999999;
		int minHP = 999999;
		MapLocation minLoc = null;

		int minMissileDist = 999999;
		int minMissileHP = 999999;
		MapLocation minMissileLoc = null;
		for (int i = enemies.length; --i >= 0;) {
			RobotInfo enemy = enemies[i];
			if (enemies[i].type == RobotType.MISSILE) { // Focus lower hp
				// missiles, followed by
				// closer missiles
				if (enemy.health == 1) { // insta kill low hp missile
					Handler.rc.attackLocation(enemy.location);
					return;
				}
				int dist = enemy.location.distanceSquaredTo(Handler.myLoc);
				if (enemy.health < minMissileHP || (enemy.health == minMissileHP && dist < minMissileDist)) {
					minMissileDist = dist;
					minMissileHP = (int) enemy.health;
					minMissileLoc = enemy.location;
				}
			} else {
				if (enemy.health < Handler.typ.attackPower) { // insta kill low
					// hp enemy
					Handler.rc.attackLocation(enemy.location);
					return;
				}
				int dist = enemy.location.distanceSquaredTo(Handler.myLoc);
				if (dist < minDist || (dist == minDist && enemy.health < minHP)) {
					minDist = dist;
					minHP = (int) enemy.health;
					minLoc = enemy.location;
				}
			}
		}
		if (minDist == 999999) { // only missiles
			Handler.rc.attackLocation(minMissileLoc);
		} else {
			Handler.rc.attackLocation(minLoc);
		}
	}

	public static void tryAttackPrioritizeTowers() throws GameActionException {
		if (Handler.rc.canAttackLocation(Handler.enemyHQ)) {
			Handler.rc.attackLocation(Handler.enemyHQ);
			return;
		}
		for (int i = Handler.enemyTowers.length; --i >= 0;) {
			if (Handler.rc.canAttackLocation(Handler.enemyTowers[i])) {
				Handler.rc.attackLocation(Handler.enemyTowers[i]);
				return;
			}
		}

		tryAttackClosestButKillIfPossible(Handler.rc.senseNearbyRobots(Handler.typ.attackRadiusSquared, Handler.otherTeam));
	}

	// returns the closest tower
	public static MapLocation getClosestTower() {
		int minDist = 999999;
		MapLocation minTower = null;
		for (int i = Handler.enemyTowers.length; --i >= 0;) {
			MapLocation tower = Handler.enemyTowers[i];
			int towerDist = Handler.myLoc.distanceSquaredTo(tower);
			if (towerDist < minDist) {
				minDist = towerDist;
				minTower = tower;
			}
		}
		return minTower;
	}

	// Returns the closest enemy
	public static RobotInfo getClosestEnemy(RobotInfo[] enemies) {
		int minDist = 999999;
		RobotInfo closestEnemy = null;
		for (int i = enemies.length; --i >= 0;) {
			MapLocation enemyLoc = enemies[i].location;
			int enemyDist = Handler.myLoc.distanceSquaredTo(enemyLoc);
			if (enemyDist < minDist) {
				minDist = enemyDist;
				closestEnemy = enemies[i];
			}
		}
		return closestEnemy;
	}

	// Gets the maximum number of turns the unit can still be supplied for
	public static int numberOfTurnsOfSupply(RobotInfo enemy) {
		return (int) (enemy.supplyLevel / enemy.type.supplyUpkeep);
	}
	
	// Gets the maximum number of turns I can still be supplied for
	public static int numberOfTurnsOfSupply() {
		return (int) (Handler.rc.getSupplyLevel() / Handler.typ.supplyUpkeep);
	}
	
	// Two magic constants used for fast ceil
	public static final double MAGICD = 32768.;
	public static final int MAGICI = 32768;
	
	// Returns true if you can be hit within the turnLimit (<=)
	// Only use when close to enemy (too many bytecodes otherwise)
	public static boolean canBeAttackedInTurns(RobotInfo enemy, int turnLimit) {
		int supplyTurns = numberOfTurnsOfSupply(enemy);
		int turns = 0;
		int dist = Handler.myLoc.distanceSquaredTo(enemy.location);
		double maxCore = enemy.coreDelay;
		double lastCoreChange = 0;
		MapLocation check = enemy.location;
		while (dist > enemy.type.attackRadiusSquared) { // enemy has to move into range before attacking
			if (maxCore - 1 > turnLimit) return false; // Cannot be hit
			Direction dir = check.directionTo(Handler.myLoc);
			if (dir.isDiagonal()) {
				lastCoreChange = 1.4 * enemy.type.movementDelay;
			} else  {
				lastCoreChange = enemy.type.movementDelay;
			}
			check = check.add(dir);
			dist = check.distanceSquaredTo(Handler.myLoc);
			maxCore += lastCoreChange;
		}
		
		double delayPassed = 0;
		
		if (maxCore > 1.0) {
			maxCore = maxCore - lastCoreChange - 1.0; // Subtracts last change
			if (maxCore > supplyTurns) {
				turns = supplyTurns + MAGICI - (int) (MAGICD - (maxCore - supplyTurns) / 0.5); // supplyTurns + ceil((maxCore - supplyTurns) / 0.5)
				delayPassed = supplyTurns + (turns - supplyTurns) * 0.5;
				supplyTurns = 0;
			} else {
				turns = MAGICI - (int) (MAGICD - maxCore); // Ceiling of maxCore
				delayPassed = turns;
				supplyTurns -= turns;
			}
		}

		if (turns > turnLimit) return false;
		
		double remainingDelay = enemy.weaponDelay - delayPassed;
		double weaponDelay = remainingDelay > enemy.type.loadingDelay ? remainingDelay : enemy.type.loadingDelay;
		
		if (weaponDelay > 1.0) {
			weaponDelay -= 1.0;
			if (weaponDelay > supplyTurns) {
				turns += supplyTurns + MAGICI - (int) (MAGICD - (weaponDelay - supplyTurns) / 0.5);
			} else {
				turns += MAGICI - (int) (MAGICD - weaponDelay);
			}
		}
		return turns <= turnLimit;
	}
}
