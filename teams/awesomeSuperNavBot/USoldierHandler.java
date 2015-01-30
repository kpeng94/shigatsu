package awesomeSuperNavBot;

import battlecode.common.*;

public class USoldierHandler extends UnitHandler {
	public static final int ALLY_RADIUS = 15;
	
	public static RobotInfo[] sensedEnemies;
	public static RobotInfo[] attackableEnemies;
	public static RobotInfo[] nearbyAllies;

	public static boolean surround = false;
	
	public static void loop(RobotController rcon) {
		try {
			init(rcon);
		} catch (Exception e) {
			// e.printStackTrace();
			System.out.println(typ + " Initialization Exception");
		}

		while (true) {
			try {
				execute();
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println(typ + " Execution Exception");
			}
			rc.yield(); // Yields to save remaining bytecodes
		}
	}

	protected static void init(RobotController rcon) throws GameActionException {
		initUnit(rcon);
		myLoc = rc.getLocation();
	}

	protected static void execute() throws GameActionException {
		executeUnit();
		Count.incrementBuffer(Comm.getSoldierId());
		
		attackableEnemies = rc.senseNearbyRobots(typ.attackRadiusSquared, otherTeam);
		sensedEnemies = rc.senseNearbyRobots(typ.sensorRadiusSquared, otherTeam);
		nearbyAllies = rc.senseNearbyRobots(ALLY_RADIUS, myTeam);
		
		int pushRound = rc.readBroadcast(Comm.FINAL_PUSH_ROUND_CHAN);
		if (pushRound > 0 && Clock.getRoundNum() >= pushRound) { // final push!!!
			if (rc.isWeaponReady()) {
				Attack.tryAttackPrioritizeTowers();
			}
			if (rc.isCoreReady() && attackableEnemies.length == 0) {
				if (enemyTowers.length > 0) {
					MapLocation towerDest = Attack.getClosestTower();
					NavTangentBug.setDest(towerDest);
					NavTangentBug.calculate(2500);
					Direction dir = NavTangentBug.getNextMove();
					if (dir != Direction.NONE) {
						if (myLoc.distanceSquaredTo(towerDest) > 35) {
							NavSimple.walkTowardsSafe(dir);
						} else {
							NavSimple.walkTowards(dir);
						}
					}
				} else {
					NavTangentBug.setDest(enemyHQ);
					NavTangentBug.calculate(2500);
					Direction dir = NavTangentBug.getNextMove();
					if (dir != Direction.NONE) {
						if (myLoc.distanceSquaredTo(enemyHQ) > 35) {
							NavSimple.walkTowardsSafe(dir);
						} else {
							NavSimple.walkTowards(dir);
						}
					}
				}
			}
		} else {
			if (sensedEnemies.length > 0) { // Gotta micro
				soldierMicro();
			} else { // No enemies in sight, just nav
				soldierNav();
			}
		}
		
		Supply.spreadSupplies(Supply.DEFAULT_THRESHOLD, nearbyAllies);
		Distribution.spendBytecodesCalculating(Handler.rc.getSupplyLevel() > 50 ? 7500 : 2500);
	}
	
	// Soldier Micro
	protected static void soldierMicro() throws GameActionException {
		int dangerx = 0;
		int dangery = 0;
		int dangernum = 0;

		RobotInfo inRangeEnemy = null;
		RobotInfo farTarget = null;

		if (rc.isWeaponReady()) {
			int minMissileDist = 999999;
			int minMissileHP = 999999;
			MapLocation minMissileLoc = null;
			
			for (int i = attackableEnemies.length; --i >= 0;) {
				RobotInfo enemy = attackableEnemies[i];
				if (enemy.type == RobotType.MISSILE) { // Focus lower hp missiles, followed by closer missiles
					if (enemy.health == 1) { // insta kill low hp missile
						rc.attackLocation(enemy.location);
						return;
					}
					int dist = enemy.location.distanceSquaredTo(myLoc);
					if (enemy.health < minMissileHP || (enemy.health == minMissileHP && dist < minMissileDist)) {
						minMissileDist = dist;
						minMissileHP = (int) enemy.health;
						minMissileLoc = enemy.location;
					}
				}
			}
			if (minMissileLoc != null) {
				rc.attackLocation(minMissileLoc);
			}
		}
		
		for (int i = sensedEnemies.length; --i >= 0;) {
			RobotInfo enemy = sensedEnemies[i];
			if (enemy.type == RobotType.MISSILE) { // dunno what to do with missiles right now
				continue;
			}
			int dist = myLoc.distanceSquaredTo(enemy.location);
			if (enemy.type.attackRadiusSquared >= dist) { // in range of enemy
				if (typ.attackRadiusSquared < dist) {// being outranged (2x danger)
					dangerx += 2 * enemy.location.x;
					dangery += 2 * enemy.location.y;
					dangernum += 2;
				} else { // both in range of each other
					if (inRangeEnemy == null && shouldAttack(enemy)) {
						inRangeEnemy = enemy;
					}
					dangerx += enemy.location.x;
					dangery += enemy.location.y;
					dangernum++;
				}
			} else { // not in enemy range
				if (typ.attackRadiusSquared < dist) { // neither in range
					if (typ.attackRadiusSquared >= enemy.type.attackRadiusSquared) { // Of equal range
						if (shouldAttack(enemy)) {
							farTarget = enemy;
						}
					} else { // outranged
						dangerx += enemy.location.x;
						dangery += enemy.location.y;
						dangernum++;
					}
				} else { // can shoot enemy without being shot back
					if (inRangeEnemy == null) {
						inRangeEnemy = enemy;
					}
				}
			}
		}
		
		if (dangernum > 1) {
			rc.setIndicatorString(0, Clock.getRoundNum() + " dangernum > 1");
			MapLocation dangerLoc = new MapLocation(dangerx / dangernum, dangery / dangernum);
			Direction runDir = dangerLoc.directionTo(myLoc);
			if (rc.isCoreReady()) {
				NavSimple.walkTowardsSafe(runDir);
			}
			if (inRangeEnemy != null && rc.isWeaponReady()) {
				rc.attackLocation(inRangeEnemy.location);
			}
		} else if (dangernum == 1) {
			rc.setIndicatorString(0, Clock.getRoundNum() + " dangernum = 1 ");
			if (inRangeEnemy != null && rc.isWeaponReady()) {
				rc.attackLocation(inRangeEnemy.location);
			} else {
				MapLocation dangerLoc = new MapLocation(dangerx, dangery);
				Direction runDir = dangerLoc.directionTo(myLoc);
				if (rc.isCoreReady()) {
					NavSimple.walkTowardsSafe(runDir);
				}
			}
		} else  {
			rc.setIndicatorString(0, Clock.getRoundNum() + " dangernum = 0");
			if (inRangeEnemy != null && rc.isWeaponReady()) {
				rc.attackLocation(inRangeEnemy.location);
			} else if (farTarget != null && rc.isCoreReady()) {
				NavSimple.walkTowardsSafe(myLoc.directionTo(farTarget.location));
			}
		}

	}
	
	protected static void soldierNav() throws GameActionException {
		if (rc.isCoreReady()) {
			if (!NavSafeBug.safeTile(myLoc.add(myLoc.directionTo(enemyHQ)))) {
				MapLocation next = myLoc.add(myLoc.directionTo(enemyHQ));
				if (next.distanceSquaredTo(enemyHQ) <= GameConstants.HQ_BUFFED_ATTACK_RADIUS_SQUARED) {
					int hqData = rc.readBroadcast(Comm.HQ_LOC);
					if (hqData == 0) { // Never seen HQ
						rc.broadcast(Comm.HQ_LOC, MapUtils.encode(enemyHQ));
						rc.broadcast(Comm.HQ_MAP, NavBFS.newBFSTask(myLoc));
					}
				}
				
				for (int i = enemyTowers.length; --i >= 0;) {
					MapLocation tower = enemyTowers[i];
					if (next.distanceSquaredTo(tower) <= RobotType.TOWER.attackRadiusSquared) {
						boolean seen = false;
						int j = 0;
						while (j < 6) {
							int towerData = rc.readBroadcast(Comm.TOWER0_LOC + j);
							if (towerData > 0) {
								MapLocation towerLoc = MapUtils.decode(towerData);
								if (towerLoc.equals(tower)) {
									seen = true;
									break;
								}
							} else {
								break;
							}
							j++;
						}

						if (!seen) {
							rc.broadcast(Comm.TOWER0_LOC + j, MapUtils.encode(tower));
							rc.broadcast(Comm.TOWER0_MAP + j, NavBFS.newBFSTask(myLoc));
						}
					}
				}
				surround = true;
			}
			Direction dir = NavSafeBug.dirToBugIn(enemyHQ);
			if (dir != Direction.NONE) {
				NavSimple.walkTowardsSafe(dir);
			}
		}
	}
	
	protected static void checkTowers() throws GameActionException {
		
	}

	// Two magic constants used for fast ceil
	public static final double MAGICD = 32768.;
	public static final int MAGICI = 32768;
	
	protected static boolean shouldAttack(RobotInfo enemy) {
		int numSoldiers = 0;
		for (int i = nearbyAllies.length; --i >= 0;) {
			if (nearbyAllies[i].type == RobotType.SOLDIER) {
				numSoldiers++;
			}
		}
		if (numSoldiers > 3 * sensedEnemies.length) {
			return true;
		}
		
		int shotsToKill = MAGICI - (int) (MAGICD - (enemy.health / typ.attackPower));
		int shotsToDie = MAGICI - (int) (MAGICD - (rc.getHealth() / enemy.type.attackPower));
		int supplyRounds = (int) (rc.getSupplyLevel() / typ.supplyUpkeep);
		int enemySupplyRounds = (int) (enemy.supplyLevel / enemy.type.supplyUpkeep);
		double totalDelay = rc.getWeaponDelay() + (shotsToKill - 1) * typ.attackDelay - 1;
		double enemyTotalDelay = enemy.weaponDelay + (shotsToDie - 1) * enemy.type.attackDelay - 1;
		double totalTurns = totalDelay > supplyRounds ? supplyRounds + ((totalDelay - supplyRounds) * 2) : totalDelay;
		double enemyTotalTurns = enemyTotalDelay > enemySupplyRounds ? enemySupplyRounds + ((enemyTotalDelay - enemySupplyRounds) * 2) : enemyTotalDelay;
		
		rc.setIndicatorString(1, Clock.getRoundNum() + " " + totalTurns + " " + enemyTotalTurns);
		
		return totalTurns < enemyTotalTurns;
	}
	
}
