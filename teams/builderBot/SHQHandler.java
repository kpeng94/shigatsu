package builderBot;

import battlecode.common.*;

public class SHQHandler extends StructureHandler {
	public static final int SPLASH_RANGE = 52;

	public static MapLocation[] towerLocs;
	public static int towerNum;

	public static int range;
	public static boolean splash;
	
	private static int[] countChans;

	public static void loop(RobotController rcon) {
		try {
			init(rcon);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(typ + " Initialization Exception");
		}

		while (true) {
			try {
				execute();
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println(typ + " Execution Exception");
			}
			rc.yield(); // Yields to save remaining bytecodes
		}
	}

	protected static void init(RobotController rcon) throws GameActionException {
		initStructure(rcon);
		
		countChans = new int[]{Comm.getBeaverId(), Comm.getMinerfactId()};
		
		initCounts();

		Count.setLimit(Comm.getBeaverId(), 1); // Maintain 1 beaver
	}

	protected static void execute() throws GameActionException {
		executeStructure();
		for (int i = countChans.length; --i >= 0;) {
			Count.resetBuffer(countChans[i]);
		}
		if (rc.isCoreReady()) { // Try to spawn
			trySpawn();
		}
		updateBuildStates();
		Count.incrementBuffer(Comm.getHqId());
	}
	
	protected static void trySpawn() throws GameActionException {
		rc.setIndicatorString(0, "" + Count.getCount(countChans[0]));
		if (Count.getCount(countChans[0]) < Count.getLimit(countChans[0])) {
			Spawner.trySpawn(myLoc.directionTo(enemyHQ).opposite(), RobotType.BEAVER, countChans[0]);
		}
	}

	protected static void initCounts() throws GameActionException {
		int towers = rc.senseTowerLocations().length;
		Comm.writeBlock(Comm.getHqId(), Count.COUNT_BUFFER_START, 1);
		Comm.writeBlock(Comm.getTowerId(), Count.COUNT_BUFFER_START, towers);
	}
	
	protected static void updateBuildStates() throws GameActionException {
		Count.setLimit(Comm.getMinerfactId(), 999);
	}
}
