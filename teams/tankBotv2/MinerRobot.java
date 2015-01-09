package tankBotv2;

import java.util.Random;

import battlecode.common.*;

public class MinerRobot extends Robot {
	public static Direction directionToEnemyHQ;
	public static RobotInfo[] enemies;
	static int numberOfLiveBeavers = 1; // one for itself
	static MapLocation rallyPoint;
	static boolean rallied = false;
	static int job = 0;
	static int directionToEnemyHQInt;
	static int distanceToEnemyHQ;
	private static int numBarracksBuilt = 0;
	static int[] offsets = {0, 1, -1, 2, -2, 3, -3, 4};
	
	public static void init(RobotController rc) throws GameActionException {
        rand = new Random(rc.getID());
		myRange = rc.getType().attackRadiusSquared;
		myTeam = rc.getTeam();
		enemyTeam = myTeam.opponent();
		myTeamHQ = rc.senseHQLocation();
		enemyTeamHQ = rc.senseEnemyHQLocation();
		directionToEnemyHQ = myTeamHQ.directionTo(enemyTeamHQ);
		directionToEnemyHQInt = directionToInt(directionToEnemyHQ);
		distanceToEnemyHQ = myTeamHQ.distanceSquaredTo(enemyTeamHQ);
		rallyPoint = MapUtils.pointSection(myTeamHQ, enemyTeamHQ, 0.75);
	}
	
	public static void run(RobotController controller) throws Exception {
		rc = controller;
		init(rc);
		while(true) {
			myLocation = rc.getLocation();
			directionFromHQ = myTeamHQ.directionTo(myLocation);
			rc.yield();
			if (rc.isWeaponReady() && decideAttack()) {
				attack();
			} else if (rc.isCoreReady()) {
				rc.setIndicatorString(0, "core is ready");
				rc.setIndicatorString(1, "ores: " + rc.senseOre(myLocation) + " " + (rc.senseOre(myLocation) >= ORE_THRESHOLD_MINER));
				rc.setIndicatorString(2, "thresh: " + ORE_THRESHOLD_MINER + " " + rc.canMine());
				if (rc.senseOre(myLocation) >= ORE_THRESHOLD_MINER && rc.canMine()) {
					rc.mine();
				} else {
					double max = 0;
					Direction maxDirection = directionFromHQ;
					for (Direction d: directions) {
						double ore = rc.senseOre(rc.getLocation().add(d));
						if (ore > max) {
							max = ore;
							maxDirection = d;
						}					
					}
					tryMove(maxDirection);
				}
			}
		}
	}
	
}