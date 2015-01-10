package tankBotv2;

import java.util.Random;

import battlecode.common.*;

public class MinerFactoryRobot extends Robot {
	public static Direction directionToEnemyHQ;
	public static int distanceToEnemyHQ;
	private static int numberOfLiveMiners = 0;
	
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
			readBroadcasts();
			if (rc.isCoreReady() && rc.getTeamOre() >= RobotType.MINER.oreCost && numberOfLiveMiners <= MINER_THRESHOLD) {
				trySpawn(directionToEnemyHQ, RobotType.MINER);				
			}

			rc.yield();
		}
		
	}
	
	public static void readBroadcasts() throws GameActionException {
		numberOfLiveMiners  = rc.readBroadcast(MINERS_COUNT_CHANNEL);
	}


}
