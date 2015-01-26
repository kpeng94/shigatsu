package qualifyingBotv1;

import battlecode.common.*;

public class Attack {

	public static void tryAttackClosestButKillIfPossible(RobotInfo[] enemies) throws GameActionException {
		if (enemies.length == 0) return;

		int minDist = 999999;
		int minHP = 999999;
		MapLocation minLoc = null;

		int minMissileDist = 999999;
		int minMissileHP = 999999;
		MapLocation minMissileLoc = null;
		for (int i = enemies.length; --i >= 0;) {
			RobotInfo enemy = enemies[i];
			if (enemies[i].type == RobotType.MISSILE) { // Focus lower hp missiles, followed by closer missiles
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
				if (enemy.health < Handler.typ.attackPower) { // insta kill low hp enemy
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

	//  Priorities:
	//	missiles with 1 hp
	//	enemies that can be killed in 1 shot
	//  enemy with the lowest action time
	//	closest enemy
	//  next acting missile
	//	lowest hp enemy
	//	lowest hp missile
	//	closest missile
	public static void tryAttackLowestDelay(RobotInfo[] enemies) throws GameActionException {
		if (enemies.length == 0)
			return;

		int minDist = 999999;
		int minHP = 999999;
		int minTurnsUntilAction = 999999;
		MapLocation minLoc = null;

		int minMissileDist = 999999;
		int minMissileHP = 999999;
		int minMissileTurnsUntilAction = 999999;
		MapLocation minMissileLoc = null;
		for (int i = enemies.length; --i >= 0;) {
			RobotInfo enemy = enemies[i];
			if (enemies[i].type == RobotType.MISSILE) { // Focus lower hp missiles, followed by closer missiles
				if (enemy.health == 1) { // insta kill low hp missile
					Handler.rc.attackLocation(enemy.location);
					return;
				}
				double moveDelay = enemy.coreDelay;
				int turnsUntilMove = moveDelay > 1.0 ?  MAGICI - (int) (MAGICD - (moveDelay - 1.0)): 0;
				int dist = enemy.location.distanceSquaredTo(Handler.myLoc);
				if (turnsUntilMove < minMissileTurnsUntilAction || (turnsUntilMove == minMissileTurnsUntilAction && enemy.health < minMissileHP)
						|| (turnsUntilMove == minMissileTurnsUntilAction && enemy.health == minMissileHP && dist < minMissileDist)) {
					minMissileDist = dist;
					minMissileHP = (int) enemy.health;
					minMissileLoc = enemy.location;
					minMissileTurnsUntilAction = turnsUntilMove;
				}
			} else {
				if (enemy.health < Handler.typ.attackPower) { // insta kill low hp enemy
					Handler.rc.attackLocation(enemy.location);
					return;
				}
				int supplyTurns = numberOfTurnsOfSupply(enemy);
				int turnsUntilAction = 0;
				int dist = enemy.location.distanceSquaredTo(Handler.myLoc);
				if (dist <= enemy.type.attackRadiusSquared || (enemy.type == RobotType.LAUNCHER && dist <= 18)) { // Enemy is in range to attack
					turnsUntilAction = numTurnsUntilAction(enemy, enemy.weaponDelay, supplyTurns);
				} else { // Check enemy movement
					turnsUntilAction = numTurnsUntilAction(enemy, enemy.coreDelay, supplyTurns);		
				}
				if (turnsUntilAction < minTurnsUntilAction || (turnsUntilAction == minTurnsUntilAction && dist < minDist)
						|| (turnsUntilAction == minTurnsUntilAction && dist == minDist && enemy.health < minHP)) {
					minDist = dist;
					minHP = (int) enemy.health;
					minLoc = enemy.location;
					minTurnsUntilAction = turnsUntilAction;
				}
			}
		}
		if (minDist == 999999) { // only missiles
			Handler.rc.attackLocation(minMissileLoc);
		} else {
			Handler.rc.setIndicatorString(0, Clock.getRoundNum() + " " + minLoc + " " + minTurnsUntilAction);
			Handler.rc.attackLocation(minLoc);
		}
	}


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

	public static RobotInfo getClosestEnemy(RobotInfo[] enemies) {
		int minDist = 999999;
		RobotInfo closestEnemy = null;
		for (int i = enemies.length; --i >= 0;) {
			MapLocation enemyLoc = enemies[i].location;
			int enemyDist = Handler.myLoc.distanceSquaredTo(enemyLoc);
			if (enemyDist < minDist) {
				minDist = enemyDist ;
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

	// Number of turns for an enemy's delay to reduce down to <= 1
	public static int numTurnsUntilAction(RobotInfo enemy, double delay) {
		if (delay <= 1.0) return 0;
		delay -= 1.0;
		int supplyTurns = numberOfTurnsOfSupply(enemy);
		if (delay > supplyTurns) {
			return supplyTurns + MAGICI - (int) (MAGICD - (delay - supplyTurns) / 0.5);
		} else {
			return MAGICI - (int) (MAGICD - delay); // Ceiling of delay
		}
	}

	// Calculates the number of turns it takes for the delay to reduce down to <= 1
	public static int numTurnsUntilAction(double delay) {
		if (delay <= 1.0) return 0;
		delay -= 1.0;
		int supplyTurns = numberOfTurnsOfSupply();
		if (delay > supplyTurns) {
			return supplyTurns + MAGICI - (int) (MAGICD - (delay - supplyTurns) / 0.5);
		} else {
			return MAGICI - (int) (MAGICD - delay); // Ceiling of delay
		}
	}

	// Number of turns for an enemy's delay to reduce down to <= 1
	public static int numTurnsUntilAction(RobotInfo enemy, double delay, int supplyTurns) {
		if (delay <= 1.0) return 0;
		delay -= 1.0;
		if (delay > supplyTurns) {
			return supplyTurns + MAGICI - (int) (MAGICD - (delay - supplyTurns) / 0.5);
		} else {
			return MAGICI - (int) (MAGICD - delay); // Ceiling of delay
		}
	}

	// Calculates the number of turns it takes for the delay to reduce down to <= 1
	public static int numTurnsUntilAction(double delay, int supplyTurns) {
		if (delay <= 1.0) return 0;
		delay -= 1.0;
		if (delay > supplyTurns) {
			return supplyTurns + MAGICI - (int) (MAGICD - (delay - supplyTurns) / 0.5);
		} else {
			return MAGICI - (int) (MAGICD - delay); // Ceiling of delay
		}
	}


	// Two magic constants used for fast ceil
	public static final double MAGICD = 32768.;
	public static final int MAGICI = 32768;

	// Calculates the minimum number of turns until the enemy can hit you in
	// Only use when close to enemy (too many bytecodes otherwise)
	public static int canBeAttackedInTurns(RobotInfo enemy) {
		int supplyTurns = numberOfTurnsOfSupply(enemy);
		int turns = 0;
		int dist = Handler.myLoc.distanceSquaredTo(enemy.location);
		double maxCore = enemy.coreDelay;
		double lastCoreChange = enemy.coreDelay;
		MapLocation check = enemy.location;
		while (dist > enemy.type.attackRadiusSquared) { // enemy has to move into range before attacking
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

		double remainingDelay = enemy.weaponDelay - delayPassed;
		double weaponDelay = remainingDelay > enemy.type.loadingDelay ? remainingDelay : enemy.type.loadingDelay;

		if (weaponDelay > 1.0) {
			weaponDelay -= 1.0;
			if (weaponDelay > supplyTurns) {
				return turns + supplyTurns + MAGICI - (int) (MAGICD - (weaponDelay - supplyTurns) / 0.5);
			} else {
				return turns + MAGICI - (int) (MAGICD - weaponDelay);
			}
		}
		return turns;
	}		
}
