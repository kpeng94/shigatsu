package launcherDroneShield;

import battlecode.common.*;

public class SHQHandler extends StructureHandler {
	public static final int SPLASH_RANGE = 52;

	public static MapLocation[] towerLocs;
	public static int towerNum;

	public static int range;
	public static boolean splash;
	public static RobotInfo[] inRangeEnemies;
	
	private static int prevOre = 0;
	
	private static int[] countChans;

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
		
		initCounts();
		
		// For initializing launcher rally position
		Rally.set(ULauncherHandler.LAUNCHER_RALLY_NUM, MapUtils.pointSection(myHQ, enemyHQ, 0.75));

		Count.setLimit(Comm.getBeaverId(), 1); // Maintain 1 beaver
	}

	protected static void execute() throws GameActionException {
		for (int channel : countChans) {
			Count.resetBuffer(channel);
		}
		
		executeStructure();
		updateTowers();
		updateOre();

		if (rc.isWeaponReady()) { // Try to attack
			calculateAttackable();
			tryAttack();
		}

		if (rc.isCoreReady()) { // Try to spawn
			trySpawn();
		}
		updateBuildStates();
		
		checkForGuard();
		
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
		if (Count.getCount(countChans[0]) < Count.getLimit(countChans[0])) {
			Spawner.trySpawn(myLoc.directionTo(enemyHQ).opposite(), RobotType.BEAVER, countChans[0]);
		}
	}

	protected static void updateTowers() {
		towerLocs = rc.senseTowerLocations();
		towerNum = towerLocs.length;
	}

	protected static void initCounts() throws GameActionException {
		int towers = rc.senseTowerLocations().length;
		Comm.writeBlock(Comm.getHqId(), Count.COUNT_BUFFER_START, 1);
		Comm.writeBlock(Comm.getTowerId(), Count.COUNT_BUFFER_START, towers);
	}

	protected static void checkForGuard() throws GameActionException {
		// Determining the rally position for the defensive swarm
		// Position is determined as the weakest tower
		MapLocation nextGuardSite = null;
		int maxNumEnemies = 0;
		MapLocation[] towerLocs = Handler.rc.senseTowerLocations();

		int numNearbyEnemies = Handler.rc.senseNearbyRobots(RobotType.TOWER.sensorRadiusSquared, Handler.otherTeam).length;
		if (numNearbyEnemies > 0) {
			nextGuardSite = Handler.myLoc;
			maxNumEnemies = numNearbyEnemies;
		}

		for (MapLocation tower: towerLocs) {
			numNearbyEnemies = Handler.rc.senseNearbyRobots(tower, RobotType.TOWER.sensorRadiusSquared, Handler.otherTeam).length;
			if (numNearbyEnemies > maxNumEnemies) {
				nextGuardSite = tower;
				maxNumEnemies = numNearbyEnemies;
			}
		}

		if (nextGuardSite != null) {
			Rally.set(UDroneHandler.DRONE_GUARD_RALLY_NUM, nextGuardSite);
		} else {
			Rally.deactivate(UDroneHandler.DRONE_GUARD_RALLY_NUM);
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
		if (Count.getCount(Comm.getLauncherId()) >= 5) {
			Count.setLimit(Comm.getMinerId(), 0);
			Count.setLimit(Comm.getHeliId(), 5);
			Count.setLimit(Comm.getDroneId(), 999);
			Count.setLimit(Comm.getLauncherId(), 8);
			Count.setLimit(Comm.getSupplyId(), 30);
		} else if (Count.getCount(Comm.getAeroId()) > 0) {
			Count.setLimit(Comm.getMinerfactId(), 0);
			Count.setLimit(Comm.getDroneId(), 30);
			Count.setLimit(Comm.getLauncherId(), 8);
			Count.setLimit(Comm.getSupplyId(), 10);
		} else {
			Count.setLimit(Comm.getBeaverId(), 1);
			Count.setLimit(Comm.getMinerfactId(), 1);
			Count.setLimit(Comm.getMinerId(), 40);
			Count.setLimit(Comm.getHeliId(), 2);
			Count.setLimit(Comm.getDroneId(), 999);
			Count.setLimit(Comm.getAeroId(), 4);
			Count.setLimit(Comm.getLauncherId(), 8);
			Count.setLimit(Comm.getSupplyId(), 10);
		}
	}

}
