package qualifyingBotv3;

import battlecode.common.*;

public class UMissileHandler extends UnitHandler {
	private static RobotInfo[] enemies;
	private static Direction nextDir = null;
	private static int timeSinceSpawned;
	private static int lastTargetId;
	private static Direction prevDir;
	
	public static void loop(RobotController rcon) {
		rc = rcon;
		otherTeam = rc.getTeam().opponent();
		myTeam = rc.getTeam();
		timeSinceSpawned = 0;
		lastTargetId = 0;
		
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

		nextDir = null;
		if (rc.canSenseRobot(lastTargetId)) {
			MapLocation loc = rc.senseRobot(lastTargetId).location;
			if (loc.distanceSquaredTo(myLoc) <= 2) {
				rc.explode();
			}
			nextDir = myLoc.directionTo(loc);
		} else {
			RobotInfo nearbyStructure = null;
			RobotInfo nearbyUnit = null;
			
			int radius = 2 * (6 - timeSinceSpawned) * (6 - timeSinceSpawned);
			
			enemies = rc.senseNearbyRobots(radius, otherTeam);
			int numEnemies = enemies.length;
			int max = numEnemies >= 5 ? 5 : numEnemies;
			for (int i = max; --i >= 0;) {
				RobotInfo enemy = enemies[i];
				if (enemy.type.isBuilding) {
					nearbyStructure = enemy;
					break;
				} else if (enemy.type != RobotType.MISSILE) {
					nearbyUnit = enemy;
				}
			}
			
			if (nearbyStructure != null) {
				MapLocation loc = nearbyStructure.location;
				if (loc.distanceSquaredTo(myLoc) <= 2) {
					rc.explode();
				}
				lastTargetId = nearbyStructure.ID;
				nextDir = myLoc.directionTo(loc);
			} else if (nearbyUnit != null) {
				MapLocation loc = nearbyUnit.location;
				if (loc.distanceSquaredTo(myLoc) <= 2) {
					rc.explode();
				}
				lastTargetId = nearbyUnit.ID;
				nextDir = myLoc.directionTo(loc);
			} else if (numEnemies > 0) {
				RobotInfo enemy = enemies[0];
				MapLocation loc = enemy.location;
				if (loc.distanceSquaredTo(myLoc) <= 2) {
					rc.explode();
				}
				lastTargetId = enemy.ID;
				nextDir = myLoc.directionTo(loc);
			} else if (nextDir == null) {
				rc.setIndicatorString(0, "nextDir is none");
				RobotInfo[] allies = rc.senseNearbyRobots(2, myTeam);
				if (allies.length > 0) {
					nextDir = allies[0].location.directionTo(myLoc);
				} else if (prevDir != null) {
					nextDir = prevDir;
				}
			}
		}
		
		prevDir = nextDir;
		
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
