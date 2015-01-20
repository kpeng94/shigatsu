package pusheenLauncherBot;

import battlecode.common.*;

public class UMissileHandler extends UnitHandler {

//	private static int BASE_SCORE = 59;
	private static RobotInfo[] enemies;
	private static MapLocation destination = null;

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
	}

	// Currently completely ineffective against kiting.
	protected static void execute() throws GameActionException {
		myLoc = rc.getLocation();
		// Get information about enemies
		if (destination == null) {
			enemies = rc.senseNearbyRobots(24, otherTeam);
			if (enemies.length > 0) {
				destination = enemies[0].location;
			}
		}
		// About 270 bytecodes left here.
		if (destination != null) {
			if (destination.distanceSquaredTo(myLoc) <= 2) {
				rc.explode();
			} else {
				if (rc.isCoreReady()) {
					Direction d = myLoc.directionTo(destination);
					if (rc.canMove(d)) {
						rc.move(d);
					} else if (rc.canMove(d.rotateRight())) {
						rc.move(d.rotateRight());
					} else if (rc.canMove(d.rotateLeft())) {
						rc.move(d.rotateLeft());						
					} else if (rc.canMove(d.rotateRight().rotateRight())) {
						rc.move(d.rotateRight().rotateRight());
					} else if (rc.canMove(d.rotateLeft().rotateLeft())) {
						rc.move(d.rotateLeft().rotateLeft());
					}
				}
				if (destination.distanceSquaredTo(myLoc) <= 2) {
					rc.explode();
				}
			}
		} else {
			// TODO: Can't find enemies, move away from my teammates.
		}
	}


	
	public static void readBroadcasts() throws GameActionException {
		
	}
	
}
