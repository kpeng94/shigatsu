package pusheenBot;

import battlecode.common.*;

public class SMinerFactoryHandler extends StructureHandler {

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
			if (Clock.getRoundNum() == 120 || Clock.getRoundNum() == 140 || (Clock.getRoundNum() > 200 && rc.getTeamOre() > 50)) {
				Spawner.trySpawn(myLoc.directionTo(enemyHQ), RobotType.MINER);
			}
		}
	}
	
}