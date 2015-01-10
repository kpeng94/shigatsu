package tankBotv2;

import java.util.Random;

import battlecode.common.*;

public class HQRobot extends Robot {
	public static RobotInfo[] enemies;
	public static MapLocation rallyPoint;
	public static RobotInfo[] myRobots;
	public static Direction directionToEnemyHQ;
	public static int distanceToEnemyHQ;
	private static int numberOfScouts = 0;
	static final int STOP_SPAWN_BEAVER_TURN = 100;
	static int numBeavers = 0;
	static int numLaunchers = 0;
    static int numMiners = 0;
    static int numMinerFactories = 0;
    static int numTanks = 0;
    static int numSupplyDepots = 0;
    static int hqDistX;
    static int hqDistY;
    static int minTowerDistX;
    static int minTowerDistY;
    static int maxTowerDistX;
    static int maxTowerDistY;

	public static void init(RobotController rc) throws GameActionException {
        rand = new Random(rc.getID());
		myRange = rc.getType().attackRadiusSquared;
		myTeam = rc.getTeam();
		enemyTeam = myTeam.opponent();
		myTeamHQ = rc.senseHQLocation();
		enemyTeamHQ = rc.senseEnemyHQLocation();
		directionToEnemyHQ = myTeamHQ.directionTo(enemyTeamHQ);
		distanceToEnemyHQ = myTeamHQ.distanceSquaredTo(enemyTeamHQ);
	}

	/*
	 * Must be called after 
	 */
	public static void calculateLooseBoundsOnMap() {
		hqDistX = myTeamHQ.x > enemyTeamHQ.x ? (myTeamHQ.x - enemyTeamHQ.x) : (enemyTeamHQ.x - myTeamHQ.x);
		hqDistY = myTeamHQ.y > enemyTeamHQ.y ? (myTeamHQ.y - enemyTeamHQ.y) : (enemyTeamHQ.y - myTeamHQ.y);
		MapLocation[] towerLocations = rc.senseTowerLocations();
		MapLocation[] enemyTowerLocations = rc.senseEnemyTowerLocations();
		int minTowerX = Integer.MAX_VALUE;
		int minTowerY = Integer.MAX_VALUE;
		int maxTowerX = Integer.MIN_VALUE;
		int maxTowerY = Integer.MIN_VALUE;
		int minETowerX = Integer.MAX_VALUE;
		int minETowerY = Integer.MAX_VALUE;
		int maxETowerX = Integer.MIN_VALUE;
		int maxETowerY = Integer.MIN_VALUE;
		for (MapLocation towerLocation : towerLocations) {
			if (towerLocation.x < minTowerX) {
				minTowerX = towerLocation.x;				
			}
			if (towerLocation.x > maxTowerX) {
				maxTowerX = towerLocation.x;
			}
			if (towerLocation.y < minTowerY) {
				minTowerY = towerLocation.y;
			}
			if (towerLocation.y > maxTowerY) {
				maxTowerY = towerLocation.y;
			}
		}
		for (MapLocation towerLocation : enemyTowerLocations) {
			if (towerLocation.x < minETowerX) {
				minETowerX = towerLocation.x;				
			}
			if (towerLocation.x > maxETowerX) {
				maxETowerX = towerLocation.x;
			}
			if (towerLocation.y < minETowerY) {
				minETowerY = towerLocation.y;
			}
			if (towerLocation.y > maxETowerY) {
				maxETowerY = towerLocation.y;
			}
		}
//		The width of the map must be at least as big as the distance between the largest X and smallest X of opposing sides
        int dx1 = maxTowerX - minETowerX;
        int dx2 = maxETowerX - minETowerX;
        maxTowerDistX = (dx1 - dx2 > 0) ? dx1 : dx2;
        int dy1 = maxTowerY - minETowerY;
        int dy2 = maxETowerY - minETowerY;
        maxTowerDistY = (dy1 - dy2 > 0) ? dy1 : dy2;
	}
	
	public static void run(RobotController controller) throws Exception {
		rc = controller;
		init(rc);
//		rc.setIndicatorString(0, "" + rc.senseTerrainTile(new MapLocation(14095, 13465)));
//		rc.setIndicatorString(1, "" + rc.senseTerrainTile(new MapLocation(14093, 13465)));
				// 14093, 13465)));
		while (true) {
			try {
				broadcastLiveObjectsCount();
				// First priority: attack
				if (rc.isWeaponReady() && decideAttack()) {
					attack();
				}

				// Second priority: spawning
				if (rc.isCoreReady()) {
					if (Clock.getRoundNum() <= STOP_SPAWN_BEAVER_TURN && rc.getTeamOre() >= RobotType.BEAVER.oreCost &&
						numMinerFactories == 0 && numBeavers == 0) {
						Direction dirToSpawn = directions[(directionToInt(directionToEnemyHQ) + 4) % 8];
						MapLocation spawnLoc = myTeamHQ.add(dirToSpawn);
						trySpawn(dirToSpawn, RobotType.BEAVER);
						// 14095, 13465
						// 14093, 13465
//						switch (numberOfScouts) {
//							case 0:
//								trySpawn(dirToSpawn, RobotType.BEAVER);
//								rc.broadcast(Clock.getRoundNum(), SCOUT_BEAVER_0);
//								numberOfScouts++;
//								rc.setIndicatorString(0, "My location is: " + myTeamHQ);
//								rc.setIndicatorString(1, "Spawned location is: " + spawnLoc);
//								rc.setIndicatorString(2, "Direction is: " + dirToSpawn);
//								break;
//							case 1:
//								trySpawn(directionToEnemyHQ, RobotType.BEAVER);
//								rc.broadcast(Clock.getRoundNum(), SCOUT_BEAVER_1);
//								numberOfScouts++;
//								rc.transferSupplies(200, spawnLoc);
//								break;
//							case 2:
//								trySpawn(directionToEnemyHQ, RobotType.BEAVER);
//								rc.broadcast(Clock.getRoundNum(), SCOUT_BEAVER_7);
//								numberOfScouts++;
//								break;
//							default:
//								trySpawn(directionToEnemyHQ, RobotType.BEAVER);
//								break;
//						}
					}
				}
				
				// TODO: consider tradeoff spawning toward enemy and toward towers
			} catch (Exception e) {
				e.printStackTrace();
			}
			rc.yield();
		}
	}

	public static boolean decideAttack() {
		enemies = rc.senseNearbyRobots(myRange, enemyTeam);
		if (enemies.length > 0) {
			return true;
		}
		return false;
	}

	public static void attack() throws GameActionException {
		rc.attackLocation(enemies[0].location);
	}

	/**
	 * The way we're setting this up, this only needs to be done once. (or maybe we'll only
	 * broadcast a few things to the robots).
	 * @throws GameActionException
	 */
	public static void broadcastGG() throws GameActionException {
		// Broadcast rally point
		MapLocation rallyPoint = MapUtils.pointSection(myTeamHQ, enemyTeamHQ, 0.25);
		int xRallyPointDelta = rallyPoint.x - myTeamHQ.x;
		int yRallyPointDelta = rallyPoint.y - myTeamHQ.y;
		rc.broadcast(66, xRallyPointDelta * 1000 + yRallyPointDelta);
	}
	
	public static void broadcastLiveObjectsCount() throws GameActionException {
		myRobots = rc.senseNearbyRobots(999999, myTeam);
		numBeavers = numLaunchers = numMiners = numMinerFactories = numTanks = numSupplyDepots = 0;
		for (RobotInfo r : myRobots) {
			RobotType type = r.type;
			switch (type) {
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
				case TANK:
					numTanks++;
					break;
				case SUPPLYDEPOT:
					numSupplyDepots++;
					break;
			}
		}

		rc.broadcast(BEAVER_COUNT_CHANNEL, numBeavers);
		rc.broadcast(LAUNCHER_COUNT_CHANNEL, numLaunchers);
		rc.broadcast(MINERS_COUNT_CHANNEL, numMiners);
		rc.broadcast(TANK_COUNT_CHANNEL, numTanks);
		rc.broadcast(MINER_FACTORIES_COUNT_CHANNEL, numMinerFactories);
		rc.broadcast(SUPPLYDEPOT_COUNT_CHANNEL, numSupplyDepots);
	}
	
	public static void giveSupply() {
		
	}
	
}
