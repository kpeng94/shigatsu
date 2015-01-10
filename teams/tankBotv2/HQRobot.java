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

	public static void run(RobotController controller) throws Exception {
		rc = controller;
		init(rc);
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
