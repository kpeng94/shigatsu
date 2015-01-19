package launcherDroneShield;

import battlecode.common.*;

public class UMinerHandler extends UnitHandler {
	public static boolean movingToFrontier = false;
	public static boolean usingBFS = false;
	public static int pathIndex = 0;
	public static MapLocation[] path;
	
	public static int lastTangentReset;
	public static MapLocation lastTangentStart;
	
	public static Direction checkDir;

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
		checkDir = MapUtils.dirs[rand.nextAnd(7)];
	}

	protected static void execute() throws GameActionException {
		executeUnit();
		if (rc.isWeaponReady()) {
			Attack.tryAttackClosestButKillIfPossible();
		}
		
		if (rc.isCoreReady()) {
			// Moving to frontier
			miningOverride: if (movingToFrontier) {
				if(rc.senseOre(myLoc) >= Mining.ORE_THRESHOLD && rc.canMine()){
					movingToFrontier = false;
					rc.mine();
					break miningOverride;
				}
				if (usingBFS) {
					while (pathIndex < path.length - 1 && myLoc.distanceSquaredTo(path[pathIndex]) <= 2) {
						pathIndex++;
					}
					if (pathIndex == path.length - 1) {
						movingToFrontier = false;
					}
					NavSimple.walkTowardsDirected(myLoc.directionTo(path[pathIndex]));
				} else {
					if (myLoc.distanceSquaredTo(lastTangentStart) < Clock.getRoundNum() - lastTangentReset - 5) {
						int frontier = Comm.readBlock(Comm.getMinerId(), Mining.FRONTIER_OFFSET);
						lastTangentReset = Clock.getRoundNum();
						lastTangentStart = myLoc;
						NavTangentBug.setDest(MapUtils.decode(frontier & 0xFFFF));
					}
					NavTangentBug.calculate(2500);
					Direction nextMove = NavTangentBug.getNextMove();
					if (nextMove != Direction.NONE) {
						NavSimple.walkTowards(nextMove);
					}
					if (myLoc.distanceSquaredTo(NavTangentBug.dest) <= 2) {
						movingToFrontier = false;
					}
				}
			} else {
				// Check ore tiles around you
				MapLocation ml;
				ml = Mining.findClosestMinableOreWithRespectToHQ(Mining.ORE_THRESHOLD, 6);
				RobotInfo[] nearbyRobots = rc.senseNearbyRobots(myLoc, 1, myTeam);
				int numMiners = 0;
				for (int i = nearbyRobots.length; --i >= 0;) {
					if (nearbyRobots[i].type == RobotType.MINER) numMiners++;
				}
				if (numMiners > Mining.ADJ_THRESHOLD) {
					if (ml != null) {
						NavSimple.walkTowards(myLoc.directionTo(ml));
					}
				} else if (rc.senseOre(myLoc) >= Mining.ORE_THRESHOLD && rc.canMine()) {
					// System.out.println("Still mining");
					rc.mine();
				} else {
					if (ml != null) {
						NavSimple.walkTowards(myLoc.directionTo(ml));
					} else {
						int frontier = Comm.readBlock(Comm.getMinerId(), Mining.FRONTIER_OFFSET);
						if (frontier != 0) {
							movingToFrontier = true;
							int hqMapBaseBlock = rc.readBroadcast(Comm.HQ_MAP_CHAN);
							if (myLoc.distanceSquaredTo(myHQ) < 8 && !(NavBFS.readMapDataUncached(hqMapBaseBlock, frontier & 0xFFFF) == 0)) {
								usingBFS = true;
								path = NavBFS.backtrace(hqMapBaseBlock, MapUtils.decode(frontier & 0xFFFF));
								pathIndex = 0;
							} else {
								usingBFS = false;
								lastTangentReset = Clock.getRoundNum();
								lastTangentStart = myLoc;
								NavTangentBug.setDest(MapUtils.decode(frontier & 0xFFFF));
							}
						} else {
							if (rc.canMove(checkDir)) {
								rc.move(checkDir);
							} else {
								checkDir = MapUtils.dirs[rand.nextAnd(7)];
								NavSimple.walkTowards(checkDir);
							}
						}
					}
				}
			}
		}

		Supply.spreadSupplies(Supply.DEFAULT_THRESHOLD);
		Distribution.spendBytecodesCalculating(Handler.rc.getSupplyLevel() > 50 ? 7500 : 2500);

	}

}
