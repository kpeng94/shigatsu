package pusheenBot;

import battlecode.common.*;

public class SHQHandler extends StructureHandler {
	public static final int SPLASH_RANGE = 52;
	public static final int UNIT_COUNT_UPDATE_FREQ = 10;
	
	public static MapLocation[] towerLocs;
	public static int towerNum;

	public static int range;
	public static boolean splash;
	public static RobotInfo[] inRangeEnemies;
	
	public static RobotInfo[] myRobots;
	static int numBeavers = 0;
	static int numLaunchers = 0;
    static int numMiners = 0;
    static int numMinerFactories = 0;
    static int numTanks = 0;
    static int numSupplyDepots = 0;
    static int numHelipads = 0;
    static int numAerospaceLabs = 0;
	
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
		initStructure(rcon);
		rc.broadcast(Comm.HQ_MAP_CHAN, NavBFS.newBFSTask(myHQ));
	}

	protected static void execute() throws GameActionException {
		executeStructure();
		if (Clock.getRoundNum() % UNIT_COUNT_UPDATE_FREQ == 0) {
			updateUnitCounts();
		}
		updateTowers();
		updateUnitCounts();
		if (rc.isWeaponReady()) { // Try to attack
			calculateAttackable();
			tryAttack();
		}
		if (Clock.getRoundNum() == 0) {
			rc.broadcast(Comm.STATE_CHAN, 1);
		} else if (Clock.getRoundNum() == 40) {
			rc.broadcast(Comm.STATE_CHAN, 2);
		} else if (Clock.getRoundNum() == 150) {
			rc.broadcast(Comm.STATE_CHAN, 3);
		}
		
		if (rc.isCoreReady()) { // Try to spawn
			if (rc.readBroadcast(Comm.STATE_CHAN) == 1 && Clock.getRoundNum() == 0) {
				Spawner.trySpawn(myLoc.directionTo(enemyHQ).opposite(), RobotType.BEAVER);
			} else if (rc.readBroadcast(Comm.STATE_CHAN) == 2 && Clock.getRoundNum() == 40) {
				Spawner.trySpawn(myLoc.directionTo(enemyHQ).rotateLeft().rotateLeft(), RobotType.BEAVER);
			} else if (Comm.readBlock(Comm.getBeaverId(), 1) < 2 && rc.readBroadcast(Comm.STATE_CHAN) >= 3) {
				Spawner.trySpawn(myLoc.directionTo(enemyHQ).opposite(), RobotType.BEAVER);
			}
		}
		Supply.spreadSupplies(Supply.DEFAULT_THRESHOLD);
		
		Distribution.spendBytecodesCalculating(7500);
	}
	
	protected static void updateTowers() {
		towerLocs = rc.senseTowerLocations();
		towerNum = towerLocs.length;
	}

	protected static void calculateAttackable() {
		if (towerNum >= 5) {
			range = GameConstants.HQ_BUFFED_ATTACK_RADIUS_SQUARED;
			splash = true;
			inRangeEnemies = rc.senseNearbyRobots(SPLASH_RANGE, otherTeam);
		} else {
			range = typ.attackRadiusSquared;
			splash = false;
			inRangeEnemies = rc.senseNearbyRobots(range, otherTeam);
		}
	}
	
	protected static void tryAttack() throws GameActionException {
		if (inRangeEnemies.length > 0) {
			MapLocation minLoc = inRangeEnemies[0].location;
			int minRange = myLoc.distanceSquaredTo(minLoc);
			for (int i = inRangeEnemies.length; --i >= 0;) { // Get minimum in array
				RobotInfo enemy = inRangeEnemies[i];
				MapLocation enemyLoc = enemy.location;
				int enemyRange = myLoc.distanceSquaredTo(enemyLoc);
				if (enemyRange < minRange) {
					minRange = enemyRange;
					minLoc = enemyLoc;
				}
			}
			
			if (minRange < range) { // Splash damage calculations
				rc.attackLocation(minLoc);
			} else {
				int dx = minLoc.x - myLoc.x;
				int dy = minLoc.y - myLoc.y;
				MapLocation newLoc = new MapLocation(minLoc.x - ((dx == 0) ? 0 : ((dx > 0) ? 1 : -1)), minLoc.y - ((dy == 0) ? 0 : ((dy > 0) ? 1 : -1)));
				if (myLoc.distanceSquaredTo(newLoc) < range) {
					rc.attackLocation(newLoc);
				}
			}
		}
	}

	protected static void updateUnitCounts() throws GameActionException {
		myRobots = rc.senseNearbyRobots(999999, myTeam);
		numAerospaceLabs = numBeavers = numLaunchers = numMiners = numMinerFactories = numTanks = numSupplyDepots = numHelipads = 0;
		for (int i = myRobots.length; --i >= 0;) {
			switch (myRobots[i].type) {
			case BEAVER:
				numBeavers++;
				break;
			case LAUNCHER:
				numLaunchers++;
				break;
			case MINER:
				numMiners++;
				break;
			case MINERFACTORY:
				numMinerFactories++;
				break;
			case SUPPLYDEPOT:
				numSupplyDepots++;
				break;
			case HELIPAD:
				numHelipads++;
				break;
			case AEROSPACELAB:
				numAerospaceLabs++;
				break;
			}
		}
		Comm.writeBlock(Comm.getHeliId(), 1, numHelipads);
		Comm.writeBlock(Comm.getBeaverId(), 1, numBeavers);
		Comm.writeBlock(Comm.getLauncherId(), 1,numLaunchers);
		Comm.writeBlock(Comm.getMinerId(), 1, numMiners);
		Comm.writeBlock(Comm.getMinerfactId(), 1, numMinerFactories);
		Comm.writeBlock(Comm.getSupplyId(), 1, numSupplyDepots);
		Comm.writeBlock(Comm.getAeroId(), 1, numAerospaceLabs);
	}	
	
}
