package qualifyingBotv3;

import battlecode.common.*;

public class STechHandler extends StructureHandler {

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
		Count.incrementBuffer(Comm.getTechId());
		if (rc.isCoreReady()) { // Try to spawn
			trySpawn();
		}
	}
	
	protected static void trySpawn() throws GameActionException {
		if (Count.getCount(Comm.getComputerId()) < Count.getLimit(Comm.getComputerId())) {
			Spawner.trySpawn(myLoc.directionTo(enemyHQ).opposite(), RobotType.COMPUTER, Comm.getComputerId());
		}
	}
}
