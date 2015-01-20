package tankBotv4;

import battlecode.common.*;

public class SAeroHandler extends StructureHandler {

	private static double oreAmount = 0.0;
	private static int lastReset = 0;

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
		if (lastReset != Clock.getRoundNum()) {
			writeBroadcasts();
		}
		if (rc.isCoreReady()) {
//			Spawner.trySpawn(myHQToEnemyHQ, RobotType.LAUNCHER);
		}
		Supply.spreadSupplies(Supply.DEFAULT_THRESHOLD);
	}

	public static void readBroadcasts() throws GameActionException {
//		numberOfLiveMiners  = Comm.readBlock(Comm.getMinerId(), 1);
		lastReset = Comm.readBlock(Comm.getLauncherId(), Comm.RESET_ROUND);
	}

	public static void writeBroadcasts() throws GameActionException {
		Comm.writeBlock(Comm.getLauncherId(), Comm.COUNT_NEARRALLYPOINT_OFFSET, 0);
		Comm.writeBlock(Comm.getLauncherId(), Comm.RESET_ROUND, Clock.getRoundNum());
	}


}
