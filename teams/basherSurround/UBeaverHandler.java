package basherSurround;

import battlecode.common.*;

public class UBeaverHandler extends UnitHandler {
//	public static int hqMapBaseBlock;
//	public static boolean smart;
//	public static MapLocation[] path;
//	public static int pathIndex;
	private static RobotType[] buildTyps = {RobotType.MINERFACTORY, RobotType.BARRACKS, RobotType.TANKFACTORY, RobotType.SUPPLYDEPOT};
	private static int[] buildChans;
	
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
		initUnit(rcon);
		Spawner.HQxMod = myHQ.x % 2;
		Spawner.HQyMod = myHQ.y % 2;
		
		buildChans = new int[]{Comm.getMinerfactId(), Comm.getBarrackId(), Comm.getTankfactId(), Comm.getSupplyId()};
	}

	protected static void execute() throws GameActionException {
		executeUnit();
		if (rc.isCoreReady()) {
			if (!tryBuild()) {
				if (myLoc.distanceSquaredTo(myHQ) > 10) {
					NavSimple.walkTowards(myLoc.directionTo(myHQ));
				} else {
					NavSimple.walkRandom();
				}
				// Random mining code
			}
//			Direction dir = NavSafeBug.dirToBugIn(enemyHQ);
//			rc.setIndicatorString(0, "" + dir);
//			if (dir != Direction.NONE) {
//				rc.move(dir);
//			}
		}
		Supply.spreadSupplies(Supply.DEFAULT_THRESHOLD);
		Distribution.spendBytecodesCalculating(Handler.rc.getSupplyLevel() > 50 ? 7500 : 2500);
	}
	
	protected static boolean tryBuild() throws GameActionException {
		Direction dir = Spawner.getBuildDirection(false);
		if (dir != Direction.NONE) {
			for (int i = 0; i < buildTyps.length; i++) {
				int num = Comm.readBlock(buildChans[i], 1);
				int limit = Comm.readBlock(buildChans[i], 2);
				if (num < limit && rc.getTeamOre() >= buildTyps[i].oreCost) {
					Spawner.build(dir, buildTyps[i], buildChans[i]);
					return true;
				}
			}
		}
		return false;
	}
	
}
