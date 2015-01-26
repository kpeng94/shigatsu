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
		myTeam = rc.getTeam();
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

		MapLocation nearbyStructure = null;
		MapLocation nearbyUnit = null;
		
		int radius = 2 * (5 - timeSinceSpawned) * (5 - timeSinceSpawned);
		
		enemies = rc.senseNearbyRobots(radius, otherTeam);
		int numEnemies = enemies.length;
		int max = numEnemies >= 5 ? 5 : numEnemies;
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
			nextDir = myLoc.directionTo(nearbyStructure);
			if (nearbyStructure.distanceSquaredTo(myLoc) <= 2) {
				rc.explode();
			}
		} else if (nearbyUnit != null) {
			nextDir = myLoc.directionTo(nearbyUnit);
			if (nearbyUnit.distanceSquaredTo(myLoc) <= 2) {
				rc.explode();
			}
		} else if (numEnemies > 0) {
			MapLocation loc = enemies[0].location;
			nextDir = myLoc.directionTo(loc);
			if (loc.distanceSquaredTo(myLoc) <= 2) {
				rc.explode();
			}
		} else if (nextDir == null) {
			RobotInfo[] allies = rc.senseNearbyRobots(radius, myTeam);
			if (allies.length > 0) {
				nextDir = allies[0].location.directionTo(myLoc);
			} else {
				rc.explode();
			}
		}
		
		if (rc.isCoreReady()) {
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
