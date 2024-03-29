package droneSupplyAll;

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
		if (rc.getTeamOre() >= 125
				&& rand.nextInt(10000) < Math.pow(
						1.2,
						15 - rc.readBroadcast(NUM_BEAVER_CHANNEL)
								+ rc.readBroadcast(NUM_DRONE_CHANNEL)) * 10000) {
			Spawner.trySpawn(MapUtils.dirs[rand.nextInt(8)], RobotType.DRONE);
		}
	}

}
