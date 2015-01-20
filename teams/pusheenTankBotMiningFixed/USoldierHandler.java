package pusheenTankBotMiningFixed;

import battlecode.common.*;

public class USoldierHandler extends UnitHandler {
	public static int spawnRound;
	public static boolean searching;
	public static MapLocation rallyDest;

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
		myLoc = rc.getLocation();
		spawnRound = Clock.getRoundNum();
		rallyDest = MapUtils.pointSection(myHQ, enemyHQ, 0.5);
		NavTangentBug.setDest(rallyDest);
		searching = (rc.readBroadcast(Comm.RALLY_MAP_CHAN) == 0);
	}

	protected static void execute() throws GameActionException {
		executeUnit();
		Count.incrementBuffer(Comm.getSoldierId());
		
		if (rc.isCoreReady()) {
			if (searching) {
				int roundsAlive = Clock.getRoundNum() - spawnRound;
				if (myLoc.distanceSquaredTo(rallyDest) < (3 + (roundsAlive / 100)) * (3 + (roundsAlive / 100))) {
					searching = false;
					rc.broadcast(Comm.RALLY_MAP_CHAN, NavBFS.newBFSTask(myLoc));
					rc.broadcast(Comm.RALLY_DEST_CHAN, MapUtils.encode(myLoc));
				} else {
					NavTangentBug.calculate(2500);
					Direction dir = NavTangentBug.getNextMove();
					if (dir != Direction.NONE) {
						NavSimple.walkTowards(dir);
					}
				}
			} else {
				int curPosInfo = NavBFS.readMapDataUncached(rc.readBroadcast(Comm.RALLY_MAP_CHAN), MapUtils.encode(myLoc));
				if (curPosInfo == 0) {
					NavTangentBug.calculate(2500);
					Direction dir = NavTangentBug.getNextMove();
					if (dir != Direction.NONE) {
						NavSimple.walkTowards(dir);
					}
				} else {
					NavSimple.walkTowardsDirected(MapUtils.dirs[curPosInfo & 0x00000007]);
				}
			}
		}
	}
	
}
