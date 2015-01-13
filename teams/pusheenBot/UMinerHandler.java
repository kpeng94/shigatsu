package pusheenBot;

import battlecode.common.*;

public class UMinerHandler extends UnitHandler {
	public static final int MINER_PROXIMITY = 2;
	public static final int ADJ_THRESHOLD = 2;
	public static final int ORE_THRESHOLD = 2;

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
	}

	protected static void execute() throws GameActionException {
		executeUnit();
		if (rc.isCoreReady()) {
			RobotInfo[] adjBots = rc.senseNearbyRobots(MINER_PROXIMITY, myTeam);
			int numMiners = 0;
			for (int i = adjBots.length; --i >= 0;) {
				if (adjBots[i].type == RobotType.MINER) numMiners++;
			}
			if (numMiners > ADJ_THRESHOLD) {
				MapLocation closest = Mining.findClosestMinableOreWithRespectToHQ(ORE_THRESHOLD, 6);
				Direction dir = closest == null ? null : myLoc.directionTo(closest);
				Handler.rc.setIndicatorString(1, Clock.getRoundNum() + " " + closest + " " + dir);
				if (dir != null) {
					if (rc.canMove(dir)) rc.move(dir);
					else {
						NavTangentBug.setDest(closest);
						NavTangentBug.calculate(4000);
						dir = NavTangentBug.getNextMove();
						Handler.rc.setIndicatorString(0, "bugging to " + closest + " " + dir);
						if (dir != Direction.NONE) NavSimple.walkTowardsDirected(dir);
					}
				}
			} else {
				if (rc.senseOre(myLoc) > ORE_THRESHOLD) {
					rc.mine();
				} else {
					MapLocation closest = Mining.findClosestMinableOreWithRespectToHQ(ORE_THRESHOLD, 6);
					Direction dir = closest == null ? null : myLoc.directionTo(closest);
					Handler.rc.setIndicatorString(1, Clock.getRoundNum() + " " + closest + " " + dir);
					if (dir != null) {
						if (rc.canMove(dir)) rc.move(dir);
						else {
							NavTangentBug.setDest(closest);
							NavTangentBug.calculate(4000);
							dir = NavTangentBug.getNextMove();
							Handler.rc.setIndicatorString(0, "bugging to " + closest + " " + dir);
							if (dir != Direction.NONE) NavSimple.walkTowards(dir);
						}
					}
				}
			}
		}
		Supply.spreadSupplies(Supply.DEFAULT_THRESHOLD);
	}
	
	protected static Direction towardsNextMineableSquare() throws GameActionException {
//		boolean[][] occupied = Mining.getOccupiedTiles(8);
//		MapLocation closest = Mining.findRangeNOre(ORE_THRESHOLD, 2, occupied);
//		if (closest == null) {
//			closest = Mining.findRangeNOre(ORE_THRESHOLD, 8, occupied);
//		}
		MapLocation closest = Mining.findClosestMinableOreWithRespectToHQ(ORE_THRESHOLD, 6);
		if (closest != null) {
			return myLoc.directionTo(closest);
		}
		return null;
	}
	
}
