package droneOffenseDefensev2;

import battlecode.common.*;

public class SHQHandler extends StructureHandler {
	public static final int splashRange = 52;
	public static final int NEXT_TARGET_X = 1;
	public static final int NEXT_TARGET_Y = 2;
	public static final int SHOULD_GUARD = 3;
	public static final int NEXT_GUARD_X = 4;
	public static final int NEXT_GUARD_Y = 5;
	
	public static MapLocation[] towerLocs;
	public static int towerNum;

	public static int range;
	public static boolean splash;
	public static RobotInfo[] inRangeEnemies;
	
	private static int numBeavers;
	private static int numDrones;
	private static int numHelipads;
	private static int numFactories;
	private static int numMiners;
	
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
		numFactories = 0;
		numMiners = 0;
		for (RobotInfo r : myRobots) {
			RobotType type = r.type;
			if (type == RobotType.DRONE) {
				numDrones++;
			} else if (type == RobotType.BEAVER) {
				numBeavers++;
			} else if (type == RobotType.HELIPAD) {
				numHelipads++;
			} else if (type == RobotType.MINERFACTORY) {
				numFactories++;
			} else if (type == RobotType.MINER) {
				numMiners++;
			}
		}
		
		Comm.writeBlock(Comm.getBeaverId(), 0, numBeavers);
		Comm.writeBlock(Comm.getDroneId(), 0, numDrones);
		Comm.writeBlock(Comm.getHeliId(), 0, numHelipads);
		Comm.writeBlock(Comm.getMinerfactId(), 0, numFactories);
		Comm.writeBlock(Comm.getMinerId(), 0, numMiners);
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
	
	protected static void setRallyPoints() throws GameActionException {
		
		// Determining the rally position for the defensive swarm
		// Position is determined as the weakest tower
		MapLocation nextGuardSite = null;
		int maxNumEnemies = 0;
		
		int numNearbyEnemies = rc.senseNearbyRobots(RobotType.TOWER.sensorRadiusSquared, otherTeam).length;
		if (numNearbyEnemies > 0) {
			nextGuardSite = myLoc;
			maxNumEnemies = numNearbyEnemies;
		}
		
		for (MapLocation tower: towerLocs) {
			numNearbyEnemies = rc.senseNearbyRobots(tower, RobotType.TOWER.sensorRadiusSquared, otherTeam).length;
			if (numNearbyEnemies > maxNumEnemies) {
				nextGuardSite = tower;
				maxNumEnemies = numNearbyEnemies;
			}
		}
		
		if (nextGuardSite != null) {
			Comm.writeBlock(Comm.getDroneId(), SHOULD_GUARD, 1);
			Comm.writeBlock(Comm.getDroneId(), NEXT_GUARD_X, nextGuardSite.x);
			Comm.writeBlock(Comm.getDroneId(), NEXT_GUARD_Y, nextGuardSite.y);
		} else {
			Comm.writeBlock(Comm.getDroneId(), SHOULD_GUARD, 0);
		}
				
		// Determining the rally and target positions for the offensive swarm
		// Positions are determined as the closest pair of team/enemy towers
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
					nextTargetSite = enemyTowers[j];
					minDistanceFromEnemy = distanceFromEnemy;
				}
			}
			if (enemyTowers.length == 0) {
				int distanceFromEnemy = towerLocs[i]
						.distanceSquaredTo(enemyHQ);
				if (distanceFromEnemy < minDistanceFromEnemy) {
					nextTargetSite = enemyHQ;
					minDistanceFromEnemy = distanceFromEnemy;
				}
			}
		}
		
		Comm.writeBlock(Comm.getDroneId(), NEXT_TARGET_X, nextTargetSite.x);
		Comm.writeBlock(Comm.getDroneId(), NEXT_TARGET_Y, nextTargetSite.y);
	}
	
	protected static void spawnBeaver() throws GameActionException {
		if (rc.getTeamOre() >= RobotType.BEAVER.oreCost &&
				numFactories == 0 && numBeavers == 0 && numHelipads == 0) {
			Spawner.trySpawn(MapUtils.dirs[(MapUtils.directionToInt(myLoc.directionTo(enemyHQ)) + 4) % 8], RobotType.BEAVER);
		}
	}
}
