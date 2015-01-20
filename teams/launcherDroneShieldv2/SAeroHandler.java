package launcherDroneShieldv2;

import battlecode.common.*;

public class SAeroHandler extends StructureHandler {

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

	protected static void init(RobotController rcon) throws GameActionException {
		initStructure(rcon);
	}

	protected static void execute() throws GameActionException {
		executeStructure();
		if (rc.isCoreReady()) { // Try to spawn
			if (Count.getCount(Comm.getLauncherId()) < Count.getLimit(Comm.getLauncherId())) {
				Spawner.trySpawn(myLoc.directionTo(enemyHQ).opposite(), RobotType.LAUNCHER, Comm.getLauncherId());
			}
		}
	}

}
