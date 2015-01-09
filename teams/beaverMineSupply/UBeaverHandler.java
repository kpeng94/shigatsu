package beaverMineSupply;

import battlecode.common.*;

public class UBeaverHandler extends UnitHandler {
	
	// Subjobs
	private enum BeaverJob {
		NONE, COURIER, MINER;
	}

	private static BeaverJob myJob;

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

	protected static void init(RobotController rcon) {
		initUnit(rcon);
		myJob = BeaverJob.NONE;
	}

	protected static void execute() throws GameActionException {
		executeUnit();
		assignJob();
		if (rc.isCoreReady()) {
			switch (myJob) {
			case COURIER:
				executeCourier();
				break;
			case MINER:
				executeMiner();
				break;
			default:
				break;
			}
		}
	}
	
	protected static void assignJob() throws GameActionException {
		if (myJob == BeaverJob.NONE) {
			int numCouriers = rc.readBroadcast(NUM_COURIER_CHANNEL);
			if (numCouriers < Clock.getRoundNum() / 400) {
				myJob = BeaverJob.COURIER;
				rc.broadcast(NUM_COURIER_CHANNEL, numCouriers + 1);
			}
			else
				myJob = BeaverJob.MINER;
		}
		else if (Clock.getRoundNum() > 1500) {
			myJob = BeaverJob.MINER;
		}
	}
	
	protected static void executeCourier() throws GameActionException {
		if (rc.getSupplyLevel() > 0) {
			MapLocation dest = myHQ;
			RobotInfo[] all = rc.senseNearbyRobots();
			double maxOre = 0;
			for (int i = 0; i < all.length; i++) {
				double oreAt = rc.senseOre(all[i].location);
				if (all[i].team == myTeam && all[i].type == RobotType.BEAVER && all[i].supplyLevel == 0 && oreAt > maxOre) {
					dest = all[i].location;
					maxOre = oreAt;
				}
			}
			if (myLoc.distanceSquaredTo(dest) < GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED) {
				rc.transferSupplies((int) rc.getSupplyLevel(), dest);
			} else {
				NavSimple.walkTowards(myLoc.directionTo(dest));
			}
		} else {
			NavSimple.walkTowards(myLoc.directionTo(myHQ));
		}
	}
	
	protected static void executeMiner() throws GameActionException {
		int fate = rand.nextInt(1000);
		if (fate < 600) {
			rc.mine();
		} else if (fate < 900) {
		 	NavSimple.walkRandom();
		} else {
			NavSimple.walkTowards(myHQ.directionTo(myLoc));
		}
		/*
		// Move away from enemies
		RobotInfo[] enemies = rc.senseNearbyRobots(25, otherTeam);
		if (enemies.length > 0) {
			MapLocation goalLoc = myLoc;
			for (int i = 0; i < enemies.length; i++) {
				if (enemies[i].type != RobotType.BEAVER)
					goalLoc.add(enemies[i].location.directionTo(myLoc));
			}
			NavSimple.walkTowards(myLoc.directionTo(goalLoc));
		} else { // If ore is depleted, move to another location
			double curOre = rc.senseOre(myLoc);
			if (curOre < 30) {
				Direction bestDir = null;
				double maxOre = curOre;
				for (int i = 0; i < MapUtils.dirs.length; i++) {
					double candOre = rc.senseOre(myLoc
							.add(MapUtils.dirs[i]));
					if (candOre > maxOre) {
						bestDir = MapUtils.dirs[i];
						maxOre = candOre;
					}
				}
				if (bestDir != null)
					NavSimple
							.walkTowards(myHQ.directionTo(rc.getLocation()));
				else
					rc.mine();
			} else {
				rc.mine();
			}
		}*/
	}
}
