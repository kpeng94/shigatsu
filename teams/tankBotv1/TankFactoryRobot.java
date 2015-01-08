package tankBotv1;

import java.util.Random;

import battlecode.common.*;

public class TankFactoryRobot extends Robot {
	public static Direction directionToEnemyHQ;
	public static int distanceToEnemyHQ;
	
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
			if (rc.isCoreReady() && rc.getTeamOre() >= RobotType.TANK.oreCost) {
				trySpawn(directionToEnemyHQ, RobotType.TANK);				
			}

			rc.yield();		
		}
		
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
