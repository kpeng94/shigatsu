package pusheenBot;

import battlecode.common.*;

public class SHQHandler extends StructureHandler {
	public static final int SPLASH_RANGE = 52;

	public static MapLocation[] towerLocs;
	public static int towerNum;

	public static int range;
	public static boolean splash;
	public static RobotInfo[] inRangeEnemies;
	
	private static int prevOre = 0;

	private static RobotInfo[] myRobots;
	
	private static RobotType[] countTyps = {RobotType.BEAVER, RobotType.MINER, RobotType.DRONE, RobotType.LAUNCHER,
		RobotType.MINERFACTORY, RobotType.HELIPAD, RobotType.AEROSPACELAB, RobotType.SUPPLYDEPOT};
	private static int[] countChans;
	private static int[] counts;

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
		initStructure(rcon);
		rc.broadcast(Comm.HQ_MAP_CHAN, NavBFS.newBFSTask(myHQ));
		
		prevOre = GameConstants.ORE_INITIAL_AMOUNT;
		
		countChans = new int[]{Comm.getBeaverId(), Comm.getMinerId(), Comm.getDroneId(), Comm.getLauncherId(),
			Comm.getMinerfactId(), Comm.getHeliId(), Comm.getAeroId(), Comm.getSupplyId()};
		counts = new int[countTyps.length];
		
		Comm.writeBlock(countChans[0], 2, 1); // Maintain 1 beaver
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
		int beaverLimit = Comm.readBlock(countChans[0], 2);
		if (counts[0] < beaverLimit) {
			Spawner.trySpawn(myLoc.directionTo(enemyHQ).opposite(), RobotType.BEAVER, countChans[0]);
		}
	}

	protected static void updateTowers() {
		towerLocs = rc.senseTowerLocations();
		towerNum = towerLocs.length;
	}

	protected static void updateUnitCounts() throws GameActionException {
		myRobots = rc.senseNearbyRobots(999999, myTeam);
		counts = new int[countTyps.length];
		for (int i = myRobots.length; --i >= 0;) {
			RobotType typ = myRobots[i].type;
			for (int j = 0; j < countTyps.length; j++) {
				if (typ == countTyps[j]) {
					counts[j]++;
					break;
				}
			}
		}

		for (int j = 0; j < countTyps.length; j++) {
			Comm.writeBlock(countChans[j], 1, counts[j]);
		}
	}

	protected static void updateOre() throws GameActionException {
		int spent = rc.readBroadcast(Comm.SPENT_ORE_BUFFER_CHAN);
		int gained = (int) (rc.getTeamOre() - prevOre + spent);
		prevOre = (int) rc.getTeamOre();
		rc.broadcast(Comm.SPENT_ORE_BUFFER_CHAN, 0);
		rc.broadcast(Comm.PREV_ORE_CHAN, prevOre);
		rc.broadcast(Comm.SPENT_ORE_CHAN, spent);
		rc.broadcast(Comm.GAINED_ORE_CHAN, gained);
	}

	protected static void updateBuildStates() throws GameActionException {
		if (counts[2] >= 20 || Clock.getRoundNum() >= 500) {
			Comm.writeBlock(Comm.getMinerfactId(), 2, 1);
			Comm.writeBlock(Comm.getMinerId(), 2, 40);
			Comm.writeBlock(Comm.getBeaverId(), 2, 2);
			Comm.writeBlock(Comm.getHeliId(), 2, 5);
			Comm.writeBlock(Comm.getDroneId(), 2, 999);
			Comm.writeBlock(Comm.getSupplyId(), 2, 30);
		} else if (counts[4] == 1) {
			Comm.writeBlock(Comm.getBeaverId(), 2, 2);
			Comm.writeBlock(Comm.getHeliId(), 2, 1);
			Comm.writeBlock(Comm.getDroneId(), 2, 999);
		} else if (counts[0] == 1) {
			Comm.writeBlock(Comm.getMinerfactId(), 2, 1);
			Comm.writeBlock(Comm.getMinerId(), 2, 25);
		}
		
	}

}
