package launcherBotv3;

import battlecode.common.*;

public class SHeliHandler extends StructureHandler {

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
		Supply.spreadSupplies(Supply.DEFAULT_THRESHOLD);
	}

	protected static void execute() throws GameActionException {
		executeStructure();
	    Count.incrementBuffer(Comm.getHeliId());
	    if (rc.isCoreReady()) { // Try to spawn
	        trySpawn();
	    }
		Supply.spreadSupplies(Supply.DEFAULT_THRESHOLD);
	}
	protected static void trySpawn() throws GameActionException {
	    if (Count.getCount(Comm.getDroneId()) < Count.getLimit(Comm.getDroneId())) {
	        Spawner.trySpawn(myLoc.directionTo(enemyHQ), RobotType.DRONE, Comm.getDroneId());
	    }
	}
}
