package droneScoutOmega;

import battlecode.common.*;

public class SHQHandler extends StructureHandler {
	public static final int splashRange = 52;
	
	public static MapLocation[] towerLocs;
	public static int towerNum;

	public static int range;
	public static boolean splash;
	public static RobotInfo[] inRangeEnemies;
	
	private static int numBeavers;
	private static int numDrones;
	private static int numHelipads;
	
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

	protected static void init(RobotController rcon) throws GameActionException {
		initStructure(rcon);
		setupBounds();
	}

	protected static void execute() throws GameActionException {
		executeStructure();
		updateTowers();
		updateCounts();
		supplyDrones();
		if (rc.isWeaponReady()) {
			calculateAttackable();
			tryAttack();
		}
		if (rc.isCoreReady()) {
			spawnBeaver();
		}
	}
	
	protected static void setupBounds() throws GameActionException {
		MapUtils.initBounds();
		
		MapUtils.updateBounds(myHQ);
		MapUtils.updateBounds(enemyHQ);
		
		for (MapLocation loc: rc.senseTowerLocations())
			MapUtils.updateBounds(loc);
		for (MapLocation loc: rc.senseEnemyTowerLocations())
			MapUtils.updateBounds(loc);
	}
	
	protected static void updateTowers() {
		towerLocs = rc.senseTowerLocations();
		towerNum = towerLocs.length;
	}
	
	protected static void calculateAttackable() {
		if (towerNum >= 5) {
			range = GameConstants.HQ_BUFFED_ATTACK_RADIUS_SQUARED;
			splash = true;
			inRangeEnemies = rc.senseNearbyRobots(splashRange, otherTeam);
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
			for (int i = inRangeEnemies.length - 1; i > 0; i--) { // Get minimum in array
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
	
	protected static void updateCounts() throws GameActionException {
		RobotInfo[] myRobots = rc.senseNearbyRobots(999999, myTeam);
		numBeavers = 0;
		numDrones = 0;
		numHelipads = 0;
		for (RobotInfo r : myRobots) {
			RobotType type = r.type;
			if (type == RobotType.DRONE) {
				numDrones++;
			} else if (type == RobotType.BEAVER) {
				numBeavers++;
			} else if (type == RobotType.HELIPAD) {
				numHelipads++;
			}
		}
		
		Comm.writeBlock(Comm.getBeaverId(), 0, numBeavers);
		Comm.writeBlock(Comm.getDroneId(), 0, numDrones);
		Comm.writeBlock(Comm.getHeliId(), 0, numHelipads);
	}
	
	protected static void supplyDrones() throws GameActionException {
		// Give supply to every drone in the vicinity if they need
		// some
		RobotInfo[] allies = rc.senseNearbyRobots(rc.getLocation(),
				15, myTeam);
		for (int i = 0; i < allies.length; i++) {
			if (allies[i].type == RobotType.DRONE
					&& allies[i].supplyLevel == 0) {
				rc.transferSupplies(
						Math.max((int) rc.getSupplyLevel(), 1000),
						allies[i].location);
			}
		}
	}
	
	protected static void spawnBeaver() throws GameActionException {
		if (rc.getTeamOre() >= 100
				&& rand.nextInt(10000) < Math.pow(1.2, 12 - numBeavers) * 10000
				&& Clock.getRoundNum() < 1000) {
			Spawner.trySpawn(MapUtils.dirs[rand.nextInt(8)], RobotType.BEAVER);
		}
	}
}
