package droneOffenseDefensev2;

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

	protected static void init(RobotController rcon) {
		initStructure(rcon);
	}

	protected static void execute() throws GameActionException {
		executeStructure();
		int numMiners = Comm.readBlock(Comm.getMinerId(), 0);
		if (rc.isCoreReady() && rc.getTeamOre() >= RobotType.MINER.oreCost && numMiners <= Constants.NUM_OF_MINERS && Clock.getRoundNum() < 1000) {
			Spawner.trySpawn(myLoc.directionTo(enemyHQ), RobotType.MINER);				
		}
	}
	
}
