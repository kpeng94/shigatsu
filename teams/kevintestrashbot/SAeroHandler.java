package kevintestrashbot;

import battlecode.common.*;

public class SAeroHandler extends StructureHandler {

	private static double oreAmount = 0.0;
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
		oreAmount = rc.getTeamOre();
		executeStructure();
		readBroadcasts();
		if (rc.isCoreReady() && rc.getTeamOre() >= RobotType.LAUNCHER.oreCost) {
			Spawner.trySpawn(myHQToEnemyHQ, RobotType.LAUNCHER, oreAmount);
		}
		rc.yield();
	}
	
	public static void readBroadcasts() throws GameActionException {
//		numberOfLiveMiners  = Comm.readBlock(Comm.getMinerId(), 1);
	}	

}
