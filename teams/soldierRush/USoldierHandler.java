package soldierRush;

import battlecode.common.*;

public class USoldierHandler extends UnitHandler {

	static Direction[] directions = { Direction.NORTH, Direction.NORTH_EAST,
			Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH,
			Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST };
	static MapLocation[] enemyTowers;
	static boolean attacking = false;

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

		enemyTowers = rc.senseEnemyTowerLocations();

		if (rc.isWeaponReady()) {
			attackSomething();
		}
		if (rc.isCoreReady()) {
			if (!attacking) {
				if (rc.readBroadcast(1) > 50) {
					attacking = true;
					if (enemyTowers.length > 0)
						tryMove(rc.getLocation().directionTo(enemyTowers[0]));
					else
						tryMove(rc.getLocation().directionTo(
								rc.senseEnemyHQLocation()));
				} else {
					if (enemyTowers.length > 0)
						tryMove(rc.getLocation().directionTo(
								MapUtils.pointSection(rc.senseHQLocation(),
										enemyTowers[0], 0.5)));
					else
						tryMove(rc.getLocation().directionTo(
								MapUtils.pointSection(rc.senseHQLocation(),
										rc.senseEnemyHQLocation(), 0.5)));
				}
			} else {
				if (rc.readBroadcast(1) > 20) {
					if (enemyTowers.length > 0)
						tryMove(rc.getLocation().directionTo(enemyTowers[0]));
					else
						tryMove(rc.getLocation().directionTo(
								rc.senseEnemyHQLocation()));
				} else {
					attacking = false;
					tryMove(rc.getLocation().directionTo(
							MapUtils.pointSection(rc.senseHQLocation(),
									rc.senseEnemyHQLocation(), 0.5)));
				}
			}
		}
	}

	// This method will attack an enemy in sight, if there is one
	static void attackSomething() throws GameActionException {
		RobotInfo[] enemies = rc.senseNearbyRobots(typ.attackRadiusSquared, rc
				.getTeam().opponent());
		if (enemies.length > 0) {
			RobotInfo worstEnemy = enemies[0];
			double worstHealth = worstEnemy.health;
			for (int i = 1; i < enemies.length; i++) {
				if (enemies[i].health < worstHealth) {
					worstEnemy = enemies[i];
					worstHealth = worstEnemy.health;
				}
			}
			rc.attackLocation(worstEnemy.location);
		}
	}

	// This method will attempt to move in Direction d (or as close to it as
	// possible)
	static void tryMove(Direction d) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = { 0, 1, -1, 2, -2 };
		int dirint = directionToInt(d);
		boolean blocked = false;
		while (offsetIndex < 5
				&& !rc.canMove(directions[(dirint + offsets[offsetIndex] + 8) % 8])) {
			offsetIndex++;
		}
		if (offsetIndex < 5) {
			rc.move(directions[(dirint + offsets[offsetIndex] + 8) % 8]);
		}
	}

	static int directionToInt(Direction d) {
		switch (d) {
		case NORTH:
			return 0;
		case NORTH_EAST:
			return 1;
		case EAST:
			return 2;
		case SOUTH_EAST:
			return 3;
		case SOUTH:
			return 4;
		case SOUTH_WEST:
			return 5;
		case WEST:
			return 6;
		case NORTH_WEST:
			return 7;
		default:
			return -1;
		}
	}

}
