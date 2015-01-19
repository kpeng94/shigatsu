package basherSurround;

import battlecode.common.*;

public class UDroneHandler extends UnitHandler {
	private static boolean attackState = false;

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
	}

	protected static void execute() throws GameActionException {
		executeUnit();
		if (Comm.readBlock(Comm.getDroneId(), 1) > 50) {
			attackState = true;
		}
		if (attackState) {
			if (rc.isWeaponReady()) {
				tryAttackPrioritizeTowers();
			}
			if (rc.isCoreReady()) {
				if (enemyTowers.length > 0) {
					if (myLoc.distanceSquaredTo(enemyTowers[0]) > 35) {
						Direction dir = NavSafeBug.dirToBugIn(enemyTowers[0]);
						if (dir != Direction.NONE) {
							rc.move(dir);
						}
					} else {
						NavSimple.walkTowards(myLoc.directionTo(enemyTowers[0]));
					}
				} else {
					if (myLoc.distanceSquaredTo(enemyHQ) > 52) {
						Direction dir = NavSafeBug.dirToBugIn(enemyHQ);
						if (dir != Direction.NONE) {
							rc.move(dir);
						}
					} else {
						NavSimple.walkTowards(myLoc.directionTo(enemyHQ));
					}
				}
			}
		} else {
			if (rc.isWeaponReady()) {
				tryAttackNormal();
			}
			if (rc.isCoreReady()) {
				Direction dir = Orbit.orbit(rc.senseTowerLocations()[0], 24);
//				Direction dir = NavSafeBug.dirToBugIn(enemyHQ);
				if (dir != Direction.NONE) {
					rc.move(dir);
				}
			}
		}
		Supply.spreadSupplies(Supply.DEFAULT_THRESHOLD);
	}
	
	protected static void tryAttackNormal() throws GameActionException {
		RobotInfo[] enemies = rc.senseNearbyRobots(typ.attackRadiusSquared, otherTeam);
		if (enemies.length > 0) {
			rc.attackLocation(enemies[0].location);
		}
	}
	
	protected static void tryAttackPrioritizeTowers() throws GameActionException {
		if (rc.canAttackLocation(enemyHQ)) {
			rc.attackLocation(enemyHQ);
			return;
		}
		for (int i = enemyTowers.length; --i >= 0;) {
			if (rc.canAttackLocation(enemyTowers[i])) {
				rc.attackLocation(enemyTowers[i]);
				return;
			}
		}
		
		tryAttackNormal();
	}
	
}
