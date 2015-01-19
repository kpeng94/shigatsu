package launcherDroneShield;

import battlecode.common.*;

public class UDroneHandler extends UnitHandler {

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
		if (rc.isWeaponReady()) {
			Attack.tryAttackClosestButKillIfPossible();
		}
		if (rc.isCoreReady()) {
			if (Count.getCount(Comm.getLauncherId()) < 1) {
				MapLocation guardPt = Rally.getGuardPt();
				if (guardPt != null) {
					Direction dir = Orbit.orbit(guardPt, RobotType.TOWER.attackRadiusSquared);
					if (dir != Direction.NONE) {
						rc.move(dir);
					}
				} else {
					Direction dir = Orbit.orbit(rc.senseTowerLocations()[0], RobotType.TOWER.attackRadiusSquared);
					if (dir != Direction.NONE) {
						rc.move(dir);
					}
				}
			} else {
				MapLocation targetPt = Rally.getTargetPt();
				if (targetPt != null) {
					Direction dir = Orbit.orbit(targetPt, RobotType.TOWER.attackRadiusSquared);
					if (dir != Direction.NONE) {
						rc.move(dir);
					}
				} else {
					Direction dir = Orbit.orbit(rc.senseTowerLocations()[0], RobotType.TOWER.attackRadiusSquared);
					if (dir != Direction.NONE) {
						rc.move(dir);
					}
				}
			}
		}
		Supply.spreadSupplies(Supply.DEFAULT_THRESHOLD);
	}
}
