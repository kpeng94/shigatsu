package droneScoutRecall;

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

	protected static void init(RobotController rcon) {
		initStructure(rcon);
	}

	protected static void execute() throws GameActionException {
		executeStructure();
		if (rc.isCoreReady()) {
			spawnDrone();
		}
	}

	protected static void spawnDrone() throws GameActionException {
		if (rc.getTeamOre() >= 125 && Comm.readBlock(Comm.getDroneId(), 0) < 2) {
			Spawner.trySpawn(MapUtils.dirs[rand.nextInt(4) * 2], RobotType.DRONE); // spawn in four cardinal directions
		}
	}

}
