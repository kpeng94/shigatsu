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
	}

	// Currently completely ineffective against kiting.
	protected static void execute() throws GameActionException {
		executeUnit();
		enemies = rc.senseNearbyRobots(15, otherTeam);
		if (enemies.length > 0 && destination == null) {
			destination = enemies[0].location;
		}
		if (destination.distanceSquaredTo(myLoc) <= 2) {
			rc.explode();
		} else {
			tryMove(myLoc.directionTo(destination));
		}
		rc.yield();
	}

	
	public static void readBroadcasts() throws GameActionException {
		
	}
	
}
