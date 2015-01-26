package qualifyingBotv1;

import battlecode.common.*;

public class STrainingHandler extends StructureHandler {
	public static final int MAX_COMMANDERS = 4;

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
		Count.incrementBuffer(Comm.getTrainingId());
		if (rc.isCoreReady() && rc.readBroadcast(Comm.COMMANDER_NUM_CHAN) < MAX_COMMANDERS) { // Try to spawn
			trySpawn();
		}
	}
	
	protected static void trySpawn() throws GameActionException {
		if (Count.getCount(Comm.getCommanderId()) < Count.getLimit(Comm.getCommanderId())) {
			if (Spawner.trySpawn(myLoc.directionTo(enemyHQ), RobotType.COMMANDER, Comm.getCommanderId())) { // Spawned
				rc.broadcast(Comm.COMMANDER_NUM_CHAN, rc.readBroadcast(Comm.COMMANDER_NUM_CHAN) + 1);
			}
		}
	}
	
}
