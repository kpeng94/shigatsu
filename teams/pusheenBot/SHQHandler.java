package pusheenBot;

import battlecode.common.*;

public class SHQHandler extends StructureHandler {
	public static final int SPLASH_RANGE = 52;

	public static MapLocation[] towerLocs;
	public static int towerNum;

	public static int range;
	public static boolean splash;
	public static RobotInfo[] inRangeEnemies;

	private static RobotInfo[] myRobots;
	private static int numBeavers = 0;
	private static int numMiners = 0;
	private static int numDrones = 0;
	private static int numLaunchers = 0;
	private static int numMinerFactories = 0;
	private static int numHelipads = 0;
	private static int numAerospaceLabs = 0;
	private static int numSupplyDepots = 0;

	private static int prevOre = 0;

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
		Comm.writeBlock(Comm.getBeaverId(), 2, 1); // Maintain 1 beaver
		prevOre = GameConstants.ORE_INITIAL_AMOUNT;
	}

	protected static void execute() throws GameActionException {
		executeStructure();
		updateTowers();
		updateUnitCounts();
		updateOre();

		if (rc.isWeaponReady()) { // Try to attack
			calculateAttackable();
			tryAttack();
		}

		if (rc.isCoreReady()) { // Try to spawn
			trySpawn();
		}
		updateBuildStates();

		Supply.spreadSupplies(Supply.DEFAULT_THRESHOLD);
		Distribution.spendBytecodesCalculating(7500);
	}

	protected static void calculateAttackable() {
		if (towerNum >= 5) {
			range = GameConstants.HQ_BUFFED_ATTACK_RADIUS_SQUARED;
			splash = true;
			inRangeEnemies = rc.senseNearbyRobots(SPLASH_RANGE, otherTeam);
		} else if (towerNum >= 2) {
			range = GameConstants.HQ_BUFFED_ATTACK_RADIUS_SQUARED;
			splash = false;
			inRangeEnemies = rc.senseNearbyRobots(range, otherTeam);
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
				MapLocation newLoc = minLoc.add(minLoc.directionTo(myLoc));
				if (myLoc.distanceSquaredTo(newLoc) < range) {
					rc.attackLocation(newLoc);
				}
			}
		}
	}

	protected static void trySpawn() throws GameActionException {
		int beaverLimit = Comm.readBlock(Comm.getBeaverId(), 2);
		if (numBeavers < beaverLimit) {
			if (Spawner.trySpawn(myLoc.directionTo(enemyHQ).opposite(), RobotType.BEAVER)) {
				numBeavers++;
				Comm.writeBlock(Comm.getBeaverId(), 1, numBeavers);
			}
		}
	}

	protected static void updateTowers() {
		towerLocs = rc.senseTowerLocations();
		towerNum = towerLocs.length;
	}

	protected static void updateUnitCounts() throws GameActionException {
		myRobots = rc.senseNearbyRobots(999999, myTeam);
		numBeavers = numMiners = numDrones = numLaunchers = 0;
		numMinerFactories = numHelipads = numAerospaceLabs = numSupplyDepots = 0;
		for (int i = myRobots.length; --i >= 0;) {
			RobotType typ = myRobots[i].type;
			if (typ == RobotType.BEAVER) {
				numBeavers++;
			} else if (typ == RobotType.MINER) {
				numMiners++;
			} else if (typ == RobotType.DRONE) {
				numDrones++;
			} else if (typ == RobotType.LAUNCHER) {
				numLaunchers++;
			} else if (typ == RobotType.MINERFACTORY) {
				numMinerFactories++;
			} else if (typ == RobotType.HELIPAD) {
				numHelipads++;
			} else if (typ == RobotType.AEROSPACELAB) {
				numAerospaceLabs++;
			} else if (typ == RobotType.SUPPLYDEPOT) {
				numSupplyDepots++;
			}
		}

		Comm.writeBlock(Comm.getBeaverId(), 1, numBeavers);
		Comm.writeBlock(Comm.getMinerId(), 1, numMiners);
		Comm.writeBlock(Comm.getDroneId(), 1, numDrones);
		Comm.writeBlock(Comm.getLauncherId(), 1,numLaunchers);
		Comm.writeBlock(Comm.getMinerfactId(), 1, numMinerFactories);
		Comm.writeBlock(Comm.getHeliId(), 1, numHelipads);
		Comm.writeBlock(Comm.getAeroId(), 1, numAerospaceLabs);
		Comm.writeBlock(Comm.getSupplyId(), 1, numSupplyDepots);
	}

	protected static void updateOre() throws GameActionException {
		int spent = rc.readBroadcast(Comm.SPENT_ORE_BUFFER_CHAN);
		int gained = (int) (rc.getTeamOre() - prevOre + spent);
		rc.broadcast(Comm.SPENT_ORE_BUFFER_CHAN, 0);
		rc.broadcast(Comm.PREV_ORE_CHAN, (int) rc.getTeamOre());
		rc.broadcast(Comm.PREV_ORE_CHAN, prevOre);
		rc.broadcast(Comm.GAINED_ORE_CHAN, gained);
	}

	protected static void updateBuildStates() throws GameActionException {
		if (numBeavers == 1) {
			Comm.writeBlock(Comm.getMinerfactId(), 2, 1);
			Comm.writeBlock(Comm.getMinerId(), 2, 30);
		}
	}

}
