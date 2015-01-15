package pusheenBot;

import battlecode.common.*;

public class UBeaverHandler extends UnitHandler {
//	public static int hqMapBaseBlock;
//	public static boolean smart;
//	public static MapLocation[] path;
//	public static int pathIndex;
	
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
			if (!tryBuild()) {
				if (myLoc.distanceSquaredTo(myHQ) > 10) {
					NavSimple.walkTowards(myLoc.directionTo(myHQ));
				} else {
					NavSimple.walkRandom();
				}
				// Random mining code
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
	
	protected static boolean tryBuild() throws GameActionException {
		Direction dir = Spawner.getBuildDirection(false);
		if (dir != Direction.NONE) {
			int minerFactNum = Comm.readBlock(Comm.getMinerfactId(), 1);
			int minerFactLimit = Comm.readBlock(Comm.getMinerfactId(), 2);
			if (minerFactNum < minerFactLimit && rc.getTeamOre() >= RobotType.MINERFACTORY.oreCost) {
				rc.build(dir, RobotType.MINERFACTORY);
				return true;
			}
			int heliNum = Comm.readBlock(Comm.getHeliId(), 1);
			int heliLimit = Comm.readBlock(Comm.getHeliId(), 2);
			if (heliNum < heliLimit && rc.getTeamOre() >= RobotType.HELIPAD.oreCost) {
				rc.build(dir, RobotType.HELIPAD);
				return true;
			}
			int aeroNum = Comm.readBlock(Comm.getAeroId(), 1);
			int aeroLimit = Comm.readBlock(Comm.getAeroId(), 2);
			if (aeroNum < aeroLimit && rc.getTeamOre() >= RobotType.AEROSPACELAB.oreCost) {
				rc.build(dir, RobotType.AEROSPACELAB);
				return true;
			}
			int supplyNum = Comm.readBlock(Comm.getSupplyId(), 1);
			int supplyLimit = Comm.readBlock(Comm.getSupplyId(), 2);
			if (supplyNum < supplyLimit && rc.getTeamOre() >= RobotType.SUPPLYDEPOT.oreCost) {
				rc.build(dir, RobotType.SUPPLYDEPOT);
				return true;
			}
		}
		return false;
	}
	
}
