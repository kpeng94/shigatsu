package kevintesttrashbot;

import java.util.Random;

import battlecode.common.*;

public class BarracksRobot extends Robot {
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
			rc.yield();		
		}
		
	}

}