package tankBotv1;

import java.util.Random;

import battlecode.common.*;

public class Robot {
	static RobotController rc;
	static Team myTeam, enemyTeam;
	static MapLocation myTeamHQ, enemyTeamHQ;
	static int mapWidth, mapHeight;
	static int myRange;
	static Random rand;
	static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	static MapLocation myLocation;
	static final int SCOUT_BEAVER_0 = 100;
	static final int SCOUT_BEAVER_1 = 101;
	static final int SCOUT_BEAVER_7 = 102;
	
	// Communication channels
	static final int BEAVER_COUNT_CHANNEL = 67;
	static final int TANK_COUNT_CHANNEL = 21390;
}
