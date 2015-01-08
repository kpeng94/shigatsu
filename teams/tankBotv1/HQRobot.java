package tankBotv1;

import java.util.Random;

import battlecode.common.*;

public class HQRobot extends Robot {
	public static RobotInfo[] enemies;
	public static MapLocation rallyPoint;
	public static RobotInfo[] myRobots;
	public static Direction directionToEnemyHQ;
	public static int distanceToEnemyHQ;
	private static int numberOfScouts = 0;
	
	
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
					if (rc.getTeamOre() >= RobotType.BEAVER.oreCost && Clock.getRoundNum() <= 100) {
						
						switch (numberOfScouts) {
							case 0:
								trySpawn(directions[(directionToInt(directionToEnemyHQ) + 4) % 8], RobotType.BEAVER);
								rc.broadcast(Clock.getRoundNum(), SCOUT_BEAVER_0);
								numberOfScouts++;
								break;
							case 1:
								trySpawn(directionToEnemyHQ, RobotType.BEAVER);
								rc.broadcast(Clock.getRoundNum(), SCOUT_BEAVER_1);
								numberOfScouts++;
								break;
							case 2:
								trySpawn(directionToEnemyHQ, RobotType.BEAVER);
								rc.broadcast(Clock.getRoundNum(), SCOUT_BEAVER_7);
								numberOfScouts++;
								break;
							default:
								trySpawn(directionToEnemyHQ, RobotType.BEAVER);
								break;
						}
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
		rc.setIndicatorString(0, "" + myRange);
		rc.setIndicatorString(1, "" + enemyTeam);
		enemies = rc.senseNearbyRobots(myRange, enemyTeam);
		if (enemies.length > 0) {
			return true;
		}
		return false;
	}

	public static void attack() throws GameActionException {
		rc.attackLocation(enemies[0].location);
	}

	public static void trySpawn(Direction d, RobotType type) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = { 0, 1, -1, 2, -2, 3, -3, 4 };
		int dirint = directionToInt(d);
		while (offsetIndex < 8 && !rc.canSpawn(directions[(dirint + offsets[offsetIndex] + 8) % 8], type)) {
			offsetIndex++;
		}
		if (offsetIndex < 8) {
			rc.spawn(directions[(dirint + offsets[offsetIndex] + 8) % 8], type);
		}
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
		int numBeavers = 0;
		int numTanks = 0;
		for (RobotInfo r : myRobots) {
			RobotType type = r.type;
			switch (type) {
				case BEAVER:
					numBeavers++;
					break;
				case TANK:
					numTanks++;
					break;
			}
		}

		rc.broadcast(BEAVER_COUNT_CHANNEL, numBeavers);
		rc.broadcast(TANK_COUNT_CHANNEL, numTanks);
	}
	
	static int directionToInt(Direction d) {
		switch (d) {
			case NORTH:
				return 0;
			case NORTH_EAST:
				return 1;
			case EAST:
				return 2;
			case SOUTH_EAST:
				return 3;
			case SOUTH:
				return 4;
			case SOUTH_WEST:
				return 5;
			case WEST:
				return 6;
			case NORTH_WEST:
				return 7;
			default:
				return -1;
		}
	}
}
