package miningBotTest;

import java.util.Random;

import battlecode.common.*;

public class MinerFactoryRobot extends Robot {
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
			if (rc.isCoreReady() && rc.getTeamOre() >= RobotType.MINER.oreCost && rc.readBroadcast(MINERS_COUNT_CHANNEL) < 1) {
				trySpawn(directionToEnemyHQ, RobotType.MINER);				
			}

			rc.yield();		
		}
		
	}


}
