package tankBotv2;

import java.util.Random;

import battlecode.common.*;

public class Robot {
	static RobotInfo[] enemies;
	static RobotController rc;
	static Team myTeam, enemyTeam;
	static MapLocation myTeamHQ, enemyTeamHQ;
	static int mapWidth, mapHeight;
	static int myRange;
	static Random rand;
	static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	static MapLocation myLocation;
	static Direction directionFromHQ;
	static int distanceToEnemyHQ;
	static int distanceFromHQ;
	
	// Beaver scout IDs
	static final int SCOUT_BEAVER_0 = 100;
	static final int SCOUT_BEAVER_1 = 101;
	static final int SCOUT_BEAVER_7 = 102;
	
	// Communication channels
	static final int BEAVER_COUNT_CHANNEL = 2138;
	static final int TANK_COUNT_CHANNEL = 2139;
	static final int SUPPLYDEPOT_COUNT_CHANNEL = 2140;
	static final int MINERS_BEING_BUILT_CHANNEL = 4232;
	static final int MINERS_COUNT_CHANNEL = 4233;
	static final int LAUNCHER_COUNT_CHANNEL = 4234;
	static final int MINER_FACTORIES_COUNT_CHANNEL = 4235;
	static final int MINER_FACTORIES_BEING_BUILT_CHANNEL = 4236;
	
	static final int MINER_BASE_DELTA = 10000;
	
	// Thresholds
	static final int MINERFACTORY_THRESHOLD = 8;
	static final int MINER_THRESHOLD = 20;
	static final int ORE_THRESHOLD = 10;
	static final int ORE_THRESHOLD_MINER = 2;
	static final int SUPPLYDEPOT_THRESHOLD = 8;
	
	// Consider making the spawn method return a direction that actually spawned in.
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

    // This method will attempt to move in Direction d (or as close to it as possible)
	static void tryMove(Direction d) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = {0,1,-1,2,-2};
		int dirint = directionToInt(d);
		while (offsetIndex < 5 && !rc.canMove(directions[(dirint+offsets[offsetIndex]+8)%8])) {
			offsetIndex++;
		}
		if (offsetIndex < 5) {
			rc.move(directions[(dirint+offsets[offsetIndex]+8)%8]);
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
	
	static void claimMineMapLocation(int deltaX, int deltaY) throws GameActionException {
		rc.broadcast(MINER_BASE_DELTA + GameConstants.MAP_MAX_WIDTH * ((120 + deltaX) % 120) + ((120 + deltaY) % 120), 1);
	}
	
	static boolean isMineMapLocationClaimed(int deltaX, int deltaY) throws GameActionException {
		return rc.readBroadcast(MINER_BASE_DELTA + GameConstants.MAP_MAX_WIDTH * ((120 + deltaX) % 120) + ((120 + deltaY) % 120)) == 1;
	}
}
