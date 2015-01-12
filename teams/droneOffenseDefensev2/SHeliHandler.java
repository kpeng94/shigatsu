package droneOffenseDefensev2;

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
		if (rc.isCoreReady() && rc.getTeamOre() >= RobotType.DRONE.oreCost) {
			for (Direction dir: MapUtils.dirs) {
				if (rc.canSpawn(dir, RobotType.DRONE)) {
					Spawner.trySpawn(dir, RobotType.DRONE);
					return;
				}
			}
		}
	}
}
