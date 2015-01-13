package kevintestrashbot;

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
		int count = Comm.readBlock(Comm.getDroneId(), 1);
		if (rc.isCoreReady() && count < 4) {
			Spawner.trySpawn(myLoc.directionTo(enemyHQ), RobotType.DRONE, RobotType.DRONE.oreCost);
		}
		Supply.spreadSupplies(Supply.DEFAULT_THRESHOLD);
	}
	
}
