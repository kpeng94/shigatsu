package qualifyingBotv2;

import battlecode.common.*;

public class UMissileHandler extends UnitHandler {

//	private static int BASE_SCORE = 59;
	private static RobotInfo[] enemies;
	private static Direction nextDir = null;
	private static int timeSinceSpawned;

	public static void loop(RobotController rcon) {
		rc = rcon;
		otherTeam = rc.getTeam().opponent();
		enemyHQ = rc.senseEnemyHQLocation();
		nextDir = rc.senseHQLocation().directionTo(enemyHQ);
		timeSinceSpawned = 0;
		
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

	// Currently completely ineffective against kiting.
	protected static void execute() throws GameActionException {
		myLoc = rc.getLocation();
		
		MapLocation destination = null;
		MapLocation nearbyStructure = null;
		MapLocation nearbyUnit = null;
		
		enemies = rc.senseNearbyRobots(2 * (5 - timeSinceSpawned) * (5 - timeSinceSpawned), otherTeam);
		int max = enemies.length >= 5 ? 5 : enemies.length;
		for (int i = max; --i >= 0;) {
			RobotInfo enemy = enemies[i];
			if (enemy.type.isBuilding) {
				nearbyStructure = enemy.location;
				break;
			} else if (enemy.type != RobotType.MISSILE) {
				nearbyUnit = enemy.location;
			}
		}
		
		if (nearbyStructure != null) {
			destination = nearbyStructure;
		} else if (nearbyUnit != null) {
			destination = nearbyUnit;
		} else {
			destination = enemyHQ;
		}
		
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
		timeSinceSpawned++;
	}
}
