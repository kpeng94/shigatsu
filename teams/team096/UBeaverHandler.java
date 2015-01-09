package team096;

import battlecode.common.*;

public class UBeaverHandler extends UnitHandler {
	private static RobotInfo[] inRangeEnemies;

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
				// e.printStackTrace();
				System.out.println(typ + " Execution Exception");
			}
			rc.yield(); // Yields to save remaining bytecodes
		}
	}

	protected static void init(RobotController rcon) {
		initUnit(rcon);
	}

	protected static void execute() throws GameActionException {
		executeUnit();
		if (rc.isWeaponReady()) {
			inRangeEnemies = rc.senseNearbyRobots(typ.attackRadiusSquared,
					otherTeam);
			tryAttack();
		}
		if (rc.isCoreReady()) {
			walkBeaver();
		}
	}

	protected static void tryAttack() throws GameActionException {
		if (inRangeEnemies.length > 0) {
			MapLocation minLoc = inRangeEnemies[0].location;
			int minRange = myLoc.distanceSquaredTo(minLoc);
			for (int i = inRangeEnemies.length - 1; i > 0; i--) { // Get minimum
																	// in array
				RobotInfo enemy = inRangeEnemies[i];
				MapLocation enemyLoc = enemy.location;
				int enemyRange = myLoc.distanceSquaredTo(enemyLoc);
				if (enemyRange < minRange) {
					minRange = enemyRange;
					minLoc = enemyLoc;
				}
			}

			rc.attackLocation(minLoc);
		}
	}

	protected static void walkBeaver() throws GameActionException {
		int fate = rand.nextInt(1000);
		if (fate < 8 && rc.getTeamOre() >= 300) {
			Spawner.tryBuild(MapUtils.dirs[rand.nextInt(8)], RobotType.HELIPAD);
		} else if (fate < 600) {
			rc.mine();
		} else if (fate < 900) {
			NavSimple.walkRandom();
		} else {
			NavSimple.walkTowards(myHQ.directionTo(rc.getLocation()));
		}
	}

}
