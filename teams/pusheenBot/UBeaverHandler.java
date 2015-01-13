package pusheenBot;

import battlecode.common.*;

public class UBeaverHandler extends UnitHandler {
//	public static int hqMapBaseBlock;
//	public static boolean smart;
//	public static MapLocation[] path;
//	public static int pathIndex;
	public static int beaverId;
	
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
		if (Clock.getRoundNum() == 0) {
			beaverId = 1;
		} else if (Clock.getRoundNum() == 40) {
			beaverId = 2;
		}
		Spawner.HQxMod = myHQ.x % 2;
		Spawner.HQyMod = myHQ.y % 2;
//		NavTangentBug.setDest(rc.senseEnemyTowerLocations()[2]);
//		hqMapBaseBlock = rc.readBroadcast(Comm.HQ_MAP_CHAN);
//		MapLocation dest = rc.senseEnemyTowerLocations()[2];
//		smart = !(NavBFS.readMapDataUncached(hqMapBaseBlock, MapUtils.encode(dest)) == 0);
//		if (smart) {
//			rc.setIndicatorString(0, "I AM SMART");
//			path = NavBFS.backtrace(hqMapBaseBlock, dest);
//			pathIndex = 0;
//		} else {
//			rc.setIndicatorString(0, "I AM DUMB");
//		}

	}

	protected static void execute() throws GameActionException {
		executeUnit();
		if (rc.isCoreReady()) {
			if (beaverId == 1 && Clock.getRoundNum() == 20) {
				Direction dir = Spawner.getBuildDirection(RobotType.MINERFACTORY, true);
				if (dir != Direction.NONE) {
					rc.build(dir, RobotType.MINERFACTORY);
				}
			} else if (beaverId == 2 && Clock.getRoundNum() == 100) {
				Direction dir = Spawner.getBuildDirection(RobotType.HELIPAD, true);
				if (dir != Direction.NONE) {
					rc.build(dir, RobotType.HELIPAD);
				}
			}
		}
//		if (!smart) NavTangentBug.calculate(2500);
//		if (rc.isCoreReady()) {
//			if (smart) {
//				if (pathIndex < path.length - 1) {
//					while (myLoc.distanceSquaredTo(path[pathIndex]) <= 2) {
//						pathIndex++;
//					}
//				}
//				NavSimple.walkTowardsDirected(myLoc.directionTo(path[pathIndex]));
//			} else {
//				Direction nextMove = NavTangentBug.getNextMove();
//				if (nextMove != Direction.NONE) {
//					NavSimple.walkTowardsDirected(nextMove);
//				}
//			}
//		}
		Supply.spreadSupplies(Supply.DEFAULT_THRESHOLD);
		Distribution.spendBytecodesCalculating(Handler.rc.getSupplyLevel() > 50 ? 7500 : 2500);
	}
	
}
