package navBot;

import battlecode.common.*;

public class UBeaverHandler extends UnitHandler {
	public static int hqMapBaseBlock;
	public static boolean smart;
	
	public static int tempBlock;
	
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
				e.printStackTrace();
				System.out.println(typ + " Execution Exception");
			}
			rc.yield(); // Yields to save remaining bytecodes
		}
	}

	protected static void init(RobotController rcon) throws GameActionException {
		initUnit(rcon);
		NavTangentBug.setDest(rc.senseEnemyTowerLocations()[2]);
		hqMapBaseBlock = rc.readBroadcast(Comm.HQ_MAP_CHAN);
		smart = !(NavBFS.readMapDataUncached(hqMapBaseBlock, MapUtils.encode(rc.senseEnemyTowerLocations()[2])) == 0);
		tempBlock = rc.readBroadcast(Comm.TEMP);
	}

	protected static void execute() throws GameActionException {
		executeUnit();
		if (!smart) NavTangentBug.calculate(2500);
		if (rc.isCoreReady()) {
//			if (smart) {
//				int[] cache = new int[NavBFS.MAP_SIZE];
//				NavBFS.backTrace();
//			} else {
				int distDir = NavBFS.readMapDataUncached(tempBlock, MapUtils.encode(myLoc));
				Direction nextMove = (distDir == 0) ? nextMove = NavTangentBug.getNextMove() : MapUtils.dirs[distDir & 0x00000007];
				Handler.rc.setIndicatorString(0, nextMove.toString());
//				Direction nextMove = NavTangentBug.getNextMove();
				if (nextMove != Direction.NONE) {
					NavSimple.walkTowardsDirected(nextMove);
				}
//			}
				
		}
		Distribution.spendBytecodesCalculating(Handler.rc.getSupplyLevel() > 50 ? 7500 : 2500);
	}
	
}
