package miningBotTest;

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
	static int[] offsets = { 0, 1, -1, 2, -2, 3, -3, 4 };

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
		while (true) {
			myLocation = rc.getLocation();
			directionFromHQ = myTeamHQ.directionTo(myLocation);
			if (rc.isWeaponReady() && decideAttack()) {
				attack();
			} else if (rc.isCoreReady()) {
//				rc.setIndicatorString(0, "core is ready");
//				rc.setIndicatorString(1, "ores: " + rc.senseOre(myLocation)
//						+ " "
//						+ (rc.senseOre(myLocation) >= ORE_THRESHOLD_MINER));
//				rc.setIndicatorString(2, "thresh: " + ORE_THRESHOLD_MINER + " "
//						+ rc.canMine());
				if (rc.senseOre(myLocation) >= ORE_THRESHOLD_MINER
						&& rc.canMine()) {
					rc.mine();
				} else {
					MapLocation ml = findClosestMinableOre(rc, ORE_THRESHOLD_MINER, 6);
					if (ml != null) {
						RobotInfo[] nearbyRobots = rc.senseNearbyRobots(myLocation, 2, myTeam);
						rc.setIndicatorString(0, "" + nearbyRobots.length);
						if (nearbyRobots.length >= 2) {
//							Direction dsum = 
//							for (RobotInfo nearbyRobot : nearbyRobots) {
//								nearbyRobot.location
//							}
							tryMove(myLocation.directionTo(ml).rotateRight().rotateRight());
						} else {
							tryMove(myLocation.directionTo(ml));							
						}
					}
				}
//					double max = 0;
//					Direction maxDirection = directionFromHQ;
//					for (Direction d : directions) {
//						double ore = rc.senseOre(rc.getLocation().add(d));
//						if (ore > max) {
//							max = ore;
//							maxDirection = d;
//						}
//					}
//					tryMove(maxDirection);
//				}
//			}
					
//			if(Clock.getRoundNum() == 1000){
//			}
			}
			rc.yield();
		}
	}

	/**
	 * Calculates the closest square with at least the threshold amount of 
	 * ore. The distance is calculated in terms of Manhattan distance and NOT
	 * Euclidean distance. This does NOT factor in the square the robot is currently on.
	 * 
	 * @param rc - RobotController for the robot
	 * @param threshold - the minimum amount of ore for the function to return
	 * @param stepLimit - the size of the search outwards (a step limit of n will search in
	 * 					  a [n by n] square, centered about the robot's current location
	 * @return - MapLocation of closest square with ore greater than the threshold, 
	 *           or null if there is none
	 */
	public static MapLocation findClosestMinableOre(RobotController rc,
			double threshold, int stepLimit) {
		int step = 1;
		int currentDirection = 0;
		MapLocation currentLocation = rc.getLocation();

		while (step < stepLimit) {
			for (int i = 0; i < step; i++) {
				currentLocation = currentLocation
						.add(directions[currentDirection]);
				if (rc.senseOre(currentLocation) > threshold)
					return currentLocation;
			}
			currentDirection = (currentDirection + 2) % 8;
			for (int i = 0; i < step; i++) {
				currentLocation = currentLocation.add(directions[currentDirection]);
				if (rc.senseOre(currentLocation) > threshold)
					return currentLocation;
			}
			currentDirection = (currentDirection + 2) % 8;
			
			step++;
		}

		return null;
	}
}
