package launcherDroneShieldv2;

import battlecode.common.*;

public class UDroneHandler extends UnitHandler {
	
	public static final int DRONE_GUARD_RALLY_NUM = 10;
	public static final int DRONE_SHIELD_RALLY_NUM = 11;

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
			MapLocation targetPt = Rally.get(DRONE_SHIELD_RALLY_NUM);
			if (Count.getCount(Comm.getLauncherId()) < 1 || targetPt == null) {
				if (Count.getCount(Comm.getLauncherId()) < 1)
					Comm.writeBlock(Comm.getLauncherId(), ULauncherHandler.NUM_RALLIED_CHANNEL, 0);
				Rally.deactivate(DRONE_SHIELD_RALLY_NUM);
				tryGuard();
			} else {
				Direction dir = Orbit.orbit(targetPt, RobotType.TOWER.attackRadiusSquared);
				if (dir != Direction.NONE) {
					rc.move(dir);
				}
			}
		}
		Supply.spreadSupplies(Supply.DEFAULT_THRESHOLD);
	}
	
	private static void tryGuard() throws GameActionException {
		MapLocation guardPt = Rally.get(DRONE_GUARD_RALLY_NUM);
		if (guardPt != null) {
			RobotInfo[] enemies = rc.senseNearbyRobots(guardPt, 52, otherTeam);
			if (enemies.length > 0)
				NavSimple.walkTowards(myLoc.directionTo(enemies[0].location));
			else {
				Direction dir = Orbit.orbit(guardPt, RobotType.TOWER.attackRadiusSquared);
				if (dir != Direction.NONE) {
					rc.move(dir);
				}
			}
		} else {
			MapLocation[] towers = rc.senseTowerLocations();
			Direction dir;
			if (towers.length == 0) {
				dir = Orbit.orbit(myHQ, RobotType.TOWER.attackRadiusSquared);
			} else {
				dir = Orbit.orbit(rc.senseTowerLocations()[0], RobotType.TOWER.attackRadiusSquared);
			}
			if (dir != Direction.NONE) {
				rc.move(dir);
			}
		}
	}
}
