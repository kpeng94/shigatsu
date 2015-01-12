package kevintestrashbot;

import battlecode.common.*;

public class UMissileHandler extends UnitHandler {

//	private static int BASE_SCORE = 59;
	private static RobotInfo[] enemies;
	private static MapLocation destination = null;

	public static void loop(RobotController rcon) {
		try {
			init(rcon);
		} catch (Exception e) {
			// e.printStackTrace();
			System.out.println(typ + " Initialization Exception");
		}

		while (true) {
			try {
				execute();
			} catch (Exception e) {
				// e.printStackTrace();
				System.out.println(typ + " Execution Exception");
			}
			rc.yield(); // Yields to save remaining bytecodes
		}
	}

	protected static void init(RobotController rcon) throws GameActionException {
		initUnit(rcon);
	}

	// Currently completely ineffective against kiting.
	protected static void execute() throws GameActionException {
		executeUnit();
		enemies = rc.senseNearbyRobots(15, otherTeam);
		if (enemies.length > 0 && destination == null) {
			destination = enemies[0].location;
			NavTangentBug.setDest(destination);
		}
		if (destination.distanceSquaredTo(myLoc) <= 2) {
			rc.explode();
		} else {
			NavSimple.walkTowards(myLoc.directionTo(destination));
		}
		rc.yield();
	}

	/**
	 * Calculates the best target square among the potential targets
	 * @param potentialTargets
	 * @return
	 * @throws GameActionException
	 */
//	public MapLocation calculateBestTarget(RobotInfo[] robot) throws GameActionException {
//		rc.senseNearbyRobots
//		int highestScore = BASE_SCORE;
//		MapLocation bestLocation = null;
//		
//		for (RobotInfo potentialTarget : potentialTargets) {
//			int currentScore = 40;
//			MapLocation location = rc.senseLocationOf(potentialTarget);
//			Robot[] splashRobots = rc.senseNearbyGameObjects(Robot.class, location, GameConstants.ARTILLERY_SPLASH_RADIUS_SQUARED, null);
//			for (Robot splashRobot : splashRobots) {
//				if (splashRobot.getTeam() == rc.getTeam()) {
//					currentScore -= 20;
//				} else {
//					currentScore += 20;
//				}
//			}
//			if (currentScore > highestScore) {
//				bestLocation = location;
//				highestScore = currentScore;
//			}
//		}
//		return bestLocation; // will return null if there were no positive score enemy targets
//	}	
	
	
}
