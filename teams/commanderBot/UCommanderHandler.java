package commanderBot;

import battlecode.common.*;

public class UCommanderHandler extends UnitHandler {
	// Thresholds for when to stop safebugging around towers and HQ
	public static final int TOWER_THRESHOLD = 35;
	public static final int HQ_SMALL_THRESHOLD = 35;
	public static final int HQ_LARGE_THRESHOLD = 55;
	public static final int HQ_SPLASH_THRESHOLD = 75;
	public static final int ALLY_RADIUS = 15;
	public static final int AGGRO_FLASH_THRESHOLD = 50;
	public static final int LAUNCHER_DANGER_RESET_THRESHOLD = 25;
	
	public static int prevRally; // Previously rally point value (to determine when to update)
	public static MapLocation rallyPoint; // Commander rally point

	public static RobotInfo[] sensedEnemies; // enemies that I can sense
	public static RobotInfo[] attackableEnemies; // enemies that I can attack (subset of sensed)
	public static RobotInfo[] nearbyAllies; // allies within ALLY_RADIUS
	
	public static boolean prevNavTangent; // Whether the previous navigation was tangent bug or not
	public static boolean justFlashed;
	
	public static MapLocation lastLauncherLocation; // Weighted avg location of the last launcher blob spotted
	public static int lastLauncherRound; // round last seen a launcher at
	
	public static int safeTurns; // Number of safe turns in a row
	
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
		prevRally = 0;
		rallyPoint = enemyHQ;
		prevNavTangent = true;
		justFlashed = false;
		safeTurns = 0;
		lastLauncherLocation = null;
		lastLauncherRound = 0;
	}

	protected static void execute() throws GameActionException {
		executeUnit();
		Count.incrementBuffer(Comm.getCommanderId());
		
		int rally = rc.readBroadcast(Comm.COMMANDER_RALLY_DEST_CHAN); // Checks for updated rally point
		if (rally != prevRally) {
			prevRally = rally;
			rallyPoint = MapUtils.decode(rally);
			prevNavTangent = true;
			NavTangentBug.setDestForced(rallyPoint);
		}
		
		// Calculate nearby enemies and allies
		attackableEnemies = rc.senseNearbyRobots(typ.attackRadiusSquared, otherTeam);
		sensedEnemies = rc.senseNearbyRobots(typ.sensorRadiusSquared, otherTeam);
		nearbyAllies = rc.senseNearbyRobots(ALLY_RADIUS, myTeam);

		if (Clock.getRoundNum() - lastLauncherRound > LAUNCHER_DANGER_RESET_THRESHOLD) { // Reset old launcher locations
			lastLauncherLocation = null;
		}
		
//		if (Clock.getRoundNum() >= rc.readBroadcast(Comm.FINAL_PUSH_ROUND_CHAN)) { // FINAL PUSH!!! (ignore micro for most part)
//			if (rc.isWeaponReady()) {
//				Attack.tryAttackPrioritizeTowers();
//			}
//			if (rc.isCoreReady() && attackableEnemies.length == 0) {
//				if (enemyTowers.length > 0) {
//					MapLocation towerDest = Attack.getClosestTower();
//					NavTangentBug.setDest(towerDest);
//					NavTangentBug.calculate(2500);
//					Direction dir = NavTangentBug.getNextMove();
//					if (dir != Direction.NONE) {
//						NavSimple.walkTowards(dir);
//					}
//				} else {
//					NavTangentBug.setDest(enemyHQ);
//					NavTangentBug.calculate(2500);
//					Direction dir = NavTangentBug.getNextMove();
//					if (dir != Direction.NONE) {
//						NavSimple.walkTowards(dir);
//					}
//				}
//			}
//		} else {
			if (sensedEnemies.length > 0) { // Gotta micro
				commanderMicro();
			} else { // No enemies in sight, just nav
				if (rc.isCoreReady()) {
					commanderNav();
				}
			}
//		}

	}
	
	// Micro for commander
	// Attacks and moves on the same turn if possible because of no CD and LD
	protected static void commanderMicro() throws GameActionException {
		safeTurns = 0;
		if (rc.isWeaponReady()) {
			attackMicro();
		}
		if (rc.isCoreReady()) {
			moveMicro();
		}
	}
	
	protected static void attackMicro() throws GameActionException {
		if (rc.hasLearnedSkill(CommanderSkillType.HEAVY_HANDS)) {
			Attack.tryAttackLowestDelay(attackableEnemies);
		} else {
			Attack.tryAttackClosestButKillIfPossible(attackableEnemies);
		}
	}
	
	protected static void moveMicro() throws GameActionException {
		int dangerx = 0;
		int dangery = 0;
		int dangernum = 0;
		
		RobotInfo target = null;
		int targetDanger = 0; // 0 default, 3 immediate danger, 2 soon danger, 1 other
		
		int immediateDamage = 0;
		int soonDamage = 0;
		
		int missileSeen = 0;
		int missileClose = 0;
		
		int launcherx = 0;
		int launchery = 0;
		int launchernum = 0;
		
		double afterMoveDelay = rc.getCoreDelay() + typ.movementDelay;
		int turnsUntilNextMove = Attack.numTurnsUntilAction(afterMoveDelay);
		
		for (int i = sensedEnemies.length; --i >= 0;) { // Iterate over all enemies
			RobotInfo enemy = sensedEnemies[i];
			int dist = myLoc.distanceSquaredTo(enemy.location);
			if (enemy.type == RobotType.LAUNCHER) {
				launcherx += enemy.location.x;
				launchery += enemy.location.y;
				launchernum++;
						
				int numMissiles = enemy.missileCount;
				if (numMissiles > 0) { // has missiles to shoot
					if (dist <= 18) {
						dangerx += 3 * enemy.location.x;
						dangery += 3 * enemy.location.y;
						dangernum += 3;
					}
					if (dist <= 10) { // Can get hit by 3 missiles next turn
						int num = numMissiles >= 3 ? 3 : numMissiles;
						immediateDamage += num * RobotType.MISSILE.attackPower;
						soonDamage += num * RobotType.MISSILE.attackPower;
					} else if (dist <= 15) { // Can get hit by 2 missiles next turn
						int num = numMissiles >= 2 ? 2 : numMissiles;
						immediateDamage += num * RobotType.MISSILE.attackPower;
						soonDamage += num * RobotType.MISSILE.attackPower;
					} else if (dist <= 18) {
						immediateDamage += RobotType.MISSILE.attackPower;
						soonDamage += RobotType.MISSILE.attackPower;
					}
				}
				if (targetDanger < 1 && numMissiles == 0) {
					target = enemy;
					targetDanger = 1;
				}
				continue;
			}
			if (enemy.type == RobotType.MISSILE) {
				missileSeen += 1;
				if (dist <= 2) { // TODO: change constant?
					missileClose += 1;
					dangerx += 10 * enemy.location.x;
					dangery += 10 * enemy.location.y;
					dangernum += 10;
				} else {
					dangerx += 5 * enemy.location.x;
					dangery += 5 * enemy.location.y;
					dangernum += 5;
				}
			}
			
			int turnsUntilDanger = Attack.canBeAttackedInTurns(enemy);
			
			if (turnsUntilDanger <= 1) { // In immediate danger
				immediateDamage += enemy.type.attackPower;
				soonDamage += enemy.type.attackPower;
				dangerx += 3 * enemy.location.x;
				dangery += 3 * enemy.location.y;
				dangernum += 3;
				if (targetDanger < 3 && enemy.type != RobotType.MISSILE) {
					target = enemy;
					targetDanger = 3;
				}
				continue;
			}
			
			if (turnsUntilDanger <= turnsUntilNextMove + 2) { // TODO: change constant?
				soonDamage += enemy.type.attackPower;
				dangerx += 2 * enemy.location.x;
				dangery += 2 * enemy.location.y;
				dangernum += 2;
				if (targetDanger < 2) {
					target = enemy;
					targetDanger = 2;
				}
				continue;
			}
			
			if (enemy.type.attackRadiusSquared >= dist || enemy.type.attackRadiusSquared > typ.attackRadiusSquared) { // in range of enemy or enemy outranges us
				dangerx += enemy.location.x;
				dangery += enemy.location.y;
				dangernum++;
			}
			if (targetDanger < 1) {
				target = enemy;
				targetDanger = 1;
			}
		}
		
		if (launchernum > 0) { // seen a launcher
			lastLauncherLocation = new MapLocation(launcherx / launchernum, launchery / launchernum);
			lastLauncherRound = Clock.getRoundNum();
		}
		
		rc.setIndicatorString(1, Clock.getRoundNum() + " " + immediateDamage + " " + soonDamage);
		// Run if in too much immediate danger
		boolean flashSuccess = false;
		if (rc.getFlashCooldown() == 0 && (missileClose > 0 || missileSeen >= 3 || immediateDamage >= rc.getHealth() / 5)) {
			MapLocation dangerLoc = new MapLocation(dangerx / dangernum, dangery / dangernum);
			Direction runDir = dangerLoc.directionTo(myLoc);
			flashSuccess = flashTowardsDirSafe(runDir);
		}
		if (!flashSuccess) { // did not flash
			if (missileClose > 0 || missileSeen >= 3 || immediateDamage >= rc.getHealth() / 20) { // TODO: change constant?
				MapLocation dangerLoc = new MapLocation(dangerx / dangernum, dangery / dangernum);
				Direction runDir = dangerLoc.directionTo(myLoc);
				NavSimple.walkTowardsSafe(runDir);
			} else if (missileSeen == 0 && soonDamage < rc.getHealth() / 5 &&
					((rc.getSupplyLevel() > 150 && rc.getHealth() > 50) || (rc.getSupplyLevel() <= 150 && rc.getHealth() > 100))) { // Safe for a while, try to move closer
				if (target == null || attackableEnemies.length >= 2 || sensedEnemies.length >= 5) return;
				if (rc.getSupplyLevel() <= 50 && rc.getCoreDelay() > 0) return;
				rc.setIndicatorString(2, Clock.getRoundNum() + " walking forward");
				if (rand.nextAnd(31) == 0) NavSafeBug.resetDir(); // Randomly reset dir just in case we are stuck
				Direction towardsDir = NavSafeBug.dirToBugIn(target.location);
				if (lastLauncherLocation != null && myLoc.distanceSquaredTo(lastLauncherLocation) > myLoc.add(towardsDir).distanceSquaredTo(lastLauncherLocation)) return;
				if (towardsDir != Direction.NONE) {
					rc.move(towardsDir);
				}
			}
		}
	}
	
	// Navigation for commander, tries to get to rally point
	// Tries to tangent bug to rally point until it is no longer safe
	// Then it safe bugs around until it is far away from danger
	// Then it resumes tangent bugging
	// Assumes no enemies are nearby
	protected static void commanderNav() throws GameActionException {
		safeTurns++;
		MapLocation closestTower = Attack.getClosestTower();
		int HQ_threshold = enemyTowers.length >= 5 ? HQ_SPLASH_THRESHOLD : (enemyTowers.length >= 2 ? HQ_LARGE_THRESHOLD : HQ_SMALL_THRESHOLD);
		if ((closestTower == null || myLoc.distanceSquaredTo(closestTower) > TOWER_THRESHOLD) && myLoc.distanceSquaredTo(enemyHQ) > HQ_threshold) { // Out of range, resume tangent bugging
			Direction towardsRally = myLoc.directionTo(rallyPoint);
			rc.setIndicatorString(2, Clock.getRoundNum() + " tangenting");
			if (safeTurns >= AGGRO_FLASH_THRESHOLD && rc.getFlashCooldown() == 0 && flashTowardsDirSafe(towardsRally)) { // Successfully flashed in forward
				justFlashed = true;
			} else {
				if (!prevNavTangent || justFlashed || safeTurns <= 1) {
					NavTangentBug.setDestForced(rallyPoint);
					justFlashed = false;
				}
				NavTangentBug.calculate(2500);
				Direction nextMove = NavTangentBug.getNextMove();
				if (lastLauncherLocation != null && myLoc.distanceSquaredTo(lastLauncherLocation) > myLoc.add(nextMove).distanceSquaredTo(lastLauncherLocation)) return;
				if (nextMove != Direction.NONE) {
					NavSimple.walkTowardsDirected(nextMove);
				}
			}
			prevNavTangent = true;
		} else { // in danger range, safe bug around
			if (prevNavTangent) {
				rc.setIndicatorString(2, Clock.getRoundNum() + " bugging and reset dir");
				NavSafeBug.resetDir();
			}
			Direction dir = NavSafeBug.dirToBugIn(rallyPoint);
			if (lastLauncherLocation != null && myLoc.distanceSquaredTo(lastLauncherLocation) > myLoc.add(dir).distanceSquaredTo(lastLauncherLocation)) return;
			if (dir != Direction.NONE) {
				rc.move(dir);
			}
			prevNavTangent = false;
		}
	}
	
	
	/**** Flash Helpers ****/
	// Never try to flash to an adjacent square (it is a waste)
	// Does not check flash cooldown for you, will throw exception if flash is on cooldown
	
	// Tries to flash in a direction as far as it can
	// Returns if flash was succesful or not
	protected static boolean flashTowardsDir(Direction dir) throws GameActionException {
		MapLocation center = myLoc.add(dir).add(dir);
		if (!dir.isDiagonal()) {
			center = center.add(dir);
		}
		return flashToFarther(center);
	}
	
	// Same as flashTowardsDir except avoids towers and HQ
	protected static boolean flashTowardsDirSafe(Direction dir) throws GameActionException {
		MapLocation center = myLoc.add(dir).add(dir);
		if (!dir.isDiagonal()) {
			center = center.add(dir);
		}
		return flashToFartherSafe(center);
	}
	
	// Tries to flash to a destination
	// If fail, check all squares around, prioritizing farther squares
	protected static boolean flashToFarther(MapLocation dest) throws GameActionException {
		Direction[] aroundDirs = MapUtils.dirsAround(myLoc.directionTo(dest));
		MapLocation[] checks = new MapLocation[9];
		checks[0] = dest;
		for (int i = 0; i < aroundDirs.length; i++) {
			checks[i + 1] = dest.add(aroundDirs[i]);
		}
		return flashToPriority(checks);
	}
	
	// Same as flashToFarther except avoids towers and HQ
	protected static boolean flashToFartherSafe(MapLocation dest) throws GameActionException {
		Direction[] aroundDirs = MapUtils.dirsAround(myLoc.directionTo(dest));
		MapLocation[] checks = new MapLocation[9];
		checks[0] = dest;
		for (int i = 0; i < aroundDirs.length; i++) {
			checks[i + 1] = dest.add(aroundDirs[i]);
		}
		return flashToPrioritySafe(checks);
	}
	
	// Tries to flash to a priority sorted list of locations
	// Returns if flash succeeds or not
	// Ignores destinations that are adjacent to the commander
	protected static boolean flashToPriority(MapLocation[] dests) throws GameActionException {
		if (rc.getFlashCooldown() > 0) return false;
		for (int i = 0; i < dests.length; i++) {
			MapLocation dest = dests[i];
			int dist = myLoc.distanceSquaredTo(dest);
			if (dist > 10 || dist <= 2 || rc.senseTerrainTile(dest) != TerrainTile.NORMAL ||
				rc.senseRobotAtLocation(dest) != null) continue;
			rc.castFlash(dest);
			return true;
		}
		return false;
	}
	
	// Same as flashToPriority except it also makes sure the location you flash to is safe from towers and enemy HQ
	protected static boolean flashToPrioritySafe(MapLocation[] dests) throws GameActionException {
		for (int i = 0; i < dests.length; i++) {
			MapLocation dest = dests[i];
			int dist = myLoc.distanceSquaredTo(dest);
			if (dist > 10 || dist <= 2 || rc.senseTerrainTile(dest) != TerrainTile.NORMAL ||
				rc.senseRobotAtLocation(dest) != null || !NavSafeBug.safeTile(dest)) continue;
			rc.castFlash(dest);
			return true;
		}
		return false;
	}
	
}
