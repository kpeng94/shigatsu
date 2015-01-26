package missileWave;

import battlecode.common.*;

public class UMissileHandler extends UnitHandler {

//	private static int BASE_SCORE = 59;
	private static RobotInfo[] enemies;
	private static MapLocation destination = null;
	private static MapLocation nearbyStructure = null;
	private static MapLocation nearbyUnit = null;
	private static Direction nextDir = null;
	private static int timeSinceSpawned;

	public static void loop(RobotController rcon) {
		try {
			init(rcon);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(typ + " Initialization Exception");
		}

		while (true) {
			try {
				execute();
			} catch (Exception e) {
				 e.printStackTrace();
				System.out.println(typ + " Execution Exception");
			}
			rc.yield(); // Yields to save remaining bytecodes
		}
	}

	protected static void init(RobotController rcon) throws GameActionException {
		rc = rcon;
		otherTeam = rc.getTeam().opponent();
		enemyHQ = rc.senseEnemyHQLocation();
		nextDir = rc.senseHQLocation().directionTo(enemyHQ);
		timeSinceSpawned = 0;
	}

	// Currently completely ineffective against kiting.
	protected static void execute() throws GameActionException {
		myLoc = rc.getLocation();
		
		enemies = rc.senseNearbyRobots((5 - timeSinceSpawned) * (5 - timeSinceSpawned), otherTeam);
		for (int i = 0; i < enemies.length && i < 5; i++) {
			if (enemies[i].type.isBuilding) {
				nearbyStructure = enemies[i].location;
				break;
			} else if (enemies[i].type != RobotType.MISSILE) {
				nearbyUnit = enemies[i].location;
			}
		}
		
		// Get information about enemies
//		if (destination == null) {
//			enemies = rc.senseNearbyRobots(24, otherTeam);
//			for (int i = 0; i < enemies.length && i < 8; i++) {
//				if (enemies[i].type.isBuilding) {
//					nearbyStructure = enemies[i].location;
//					break;
//				} else if (enemies[i].type != RobotType.MISSILE) {
//					nearbyUnit = enemies[i].location;
//				}
//			}
//			if (enemies.length > 0) {
//				destination = enemies[0].location;
//			}
//		}
		
		if (nearbyStructure != null) {
			destination = nearbyStructure;
		} else if (nearbyUnit != null) {
			destination = nearbyUnit;
		} else {
			destination = enemyHQ;
		}
		
		// About 270 bytecodes left here.
		if (destination.distanceSquaredTo(myLoc) <= 2) {
			rc.explode();
		}
		if (rc.isCoreReady()) {
			nextDir = myLoc.directionTo(destination);
			if (rc.canMove(nextDir)) {
				rc.move(nextDir);
			} else if (rc.canMove(nextDir.rotateRight())) {
				rc.move(nextDir.rotateRight());
			} else if (rc.canMove(nextDir.rotateLeft())) {
				rc.move(nextDir.rotateLeft());						
			} else if (rc.canMove(nextDir.rotateRight().rotateRight())) {
				rc.move(nextDir.rotateRight().rotateRight());
			} else if (rc.canMove(nextDir.rotateLeft().rotateLeft())) {
				rc.move(nextDir.rotateLeft().rotateLeft());
			}
		}
		
		// Reset locations and update the counter
		destination = null;
		nearbyStructure = null;
		nearbyUnit = null;
		timeSinceSpawned++;
	}
}
