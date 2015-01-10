package droneKiteOD;

import battlecode.common.*;

public class SHQHandler extends StructureHandler {
	public static final int splashRange = 52;
	
	public static MapLocation[] towerLocs;
	public static int towerNum;

	public static int range;
	public static boolean splash;
	public static RobotInfo[] inRangeEnemies;
	
	private static int nextTriggerRound;
	private static int waveNumber;
	
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

	protected static void init(RobotController rcon) {
		initStructure(rcon);
		rc.setIndicatorString(0, "DOD");
		nextTriggerRound = 0;
		waveNumber = 1;
	}

	protected static void execute() throws GameActionException {
		executeStructure();
		updateTowers();
		updateCounts();
		supplyDrones();
		setRallyPoints();
		if (rc.isWeaponReady()) {
			calculateAttackable();
			tryAttack();
		}
		if (rc.isCoreReady()) {
			spawnBeaver();
		}
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
		
		rc.broadcast(NUM_BEAVER_CHANNEL, numBeavers);
		rc.broadcast(NUM_DRONE_CHANNEL, numDrones);
		rc.broadcast(NUM_HELIPAD_CHANNEL, numHelipads);
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
						Math.max((int) rc.getSupplyLevel(), 300),
						allies[i].location);
			}
		}

		// Supply drones if about to attack
		if (Clock.getRoundNum() % INTERWAVE_TIME == INTERWAVE_TIME - 1) {
			for (int i = 0; i < allies.length; i++) {
				if (allies[i].type == RobotType.DRONE
						&& allies[i].supplyLevel == 0) {
					rc.transferSupplies(Math.min(
							(int) rc.getSupplyLevel(), 500),
							allies[i].location);
				}
			}
		}
	}
	
	protected static void setRallyPoints() throws GameActionException {
		
		// Determining the rally position for the defensive swarm
		// Position is determined as the weakest tower
		MapLocation nextGuardSite = myHQ;
		double minTowerHealth = Double.MAX_VALUE;
		int maxNumEnemies = 0;
		for (int i = 0; i < towerLocs.length; i++) {
			double towerHealth = rc.senseRobotAtLocation(towerLocs[i]).health;
			int numNearbyEnemies = rc.senseNearbyRobots(myLoc, 35, otherTeam).length;
			if (towerHealth < minTowerHealth) {
				nextGuardSite = towerLocs[i];
				minTowerHealth = towerHealth;
			}
			else if (towerHealth == minTowerHealth && numNearbyEnemies > maxNumEnemies) {
				nextGuardSite = towerLocs[i];
				maxNumEnemies = numNearbyEnemies;
			}
		}
		
		// Determining the rally and target positions for the offensive swarm
		// Positions are determined as the closest pair of team/enemy towers
		MapLocation nextRallySite = myHQ;
		MapLocation nextTargetSite = enemyHQ;
		
		int minDistanceFromEnemy = Integer.MAX_VALUE;

		MapLocation[] enemyTowers = rc.senseEnemyTowerLocations();
		for (int j = 0; j < enemyTowers.length; j++) {
			int distanceFromEnemy = myHQ
					.distanceSquaredTo(enemyTowers[j]);
			if (distanceFromEnemy < minDistanceFromEnemy) {
				nextTargetSite = enemyTowers[j];
				minDistanceFromEnemy = distanceFromEnemy;
			}
		}
		for (int i = 0; i < towerLocs.length; i++) {
			for (int j = 0; j < enemyTowers.length; j++) {
				int distanceFromEnemy = towerLocs[i]
						.distanceSquaredTo(enemyTowers[j]);
				if (distanceFromEnemy < minDistanceFromEnemy) {
					nextRallySite = towerLocs[i];
					nextTargetSite = enemyTowers[j];
					minDistanceFromEnemy = distanceFromEnemy;
				}
			}
			if (enemyTowers.length == 0) {
				int distanceFromEnemy = towerLocs[i]
						.distanceSquaredTo(enemyHQ);
				if (distanceFromEnemy < minDistanceFromEnemy) {
					nextRallySite = towerLocs[i];
					nextTargetSite = enemyHQ;
					minDistanceFromEnemy = distanceFromEnemy;
				}
			}
		}
		rc.broadcast(WAVE_NUMBER_CHANNEL, waveNumber);

		rc.broadcast(NEXT_RALLY_X_CHANNEL, nextRallySite.x);
		rc.broadcast(NEXT_RALLY_Y_CHANNEL, nextRallySite.y);
		rc.broadcast(NEXT_TARGET_X_CHANNEL, nextTargetSite.x);
		rc.broadcast(NEXT_TARGET_Y_CHANNEL, nextTargetSite.y);
		rc.broadcast(NEXT_GUARD_X_CHANNEL, nextGuardSite.x);
		rc.broadcast(NEXT_GUARD_Y_CHANNEL, nextGuardSite.y);

		if (Clock.getRoundNum() == nextTriggerRound) {
			rc.broadcast(DRONE_PROMOTION_CHANNEL, 1);
			nextTriggerRound += INTERWAVE_TIME;
			waveNumber++;
		} else {
			rc.broadcast(DRONE_PROMOTION_CHANNEL, 0);
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
