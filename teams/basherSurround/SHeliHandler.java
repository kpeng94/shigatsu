package basherSurround;

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
	}

	protected static void execute() throws GameActionException {
		executeStructure();
		if (rc.isCoreReady()) { // Try to spawn
			trySpawn();
		}
		Supply.spreadSupplies(Supply.DEFAULT_THRESHOLD);
	}
	
	protected static void trySpawn() throws GameActionException {
		int numDrones = Comm.readBlock(Comm.getDroneId(), 1);
		int droneLimit = Comm.readBlock(Comm.getDroneId(), 2);
		if (numDrones < droneLimit) {
			Spawner.trySpawn(myLoc.directionTo(enemyHQ).opposite(), RobotType.DRONE, Comm.getDroneId());
		}
	}
	
}
