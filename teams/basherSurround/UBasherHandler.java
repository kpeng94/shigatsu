package basherSurround;

import battlecode.common.*;

public class UBasherHandler extends UnitHandler {

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
				 e.printStackTrace();
				System.out.println(typ + " Execution Exception");
			}
			rc.yield(); // Yields to save remaining bytecodes
		}
	}

	protected static void init(RobotController rcon) throws GameActionException {
		initUnit(rcon);
	}

	protected static void execute() throws GameActionException {
		executeUnit();
		
		MapLocation guardPt = Rally.getGuardPt();
		MapLocation atkPt = Rally.getTargetPt();
		
		if (guardPt != null) {
			rc.setIndicatorString(0, "Guarding");
			if (rc.isCoreReady()) {
//				if (shouldConverge(guardPt)) {
//					rc.move(myLoc.directionTo(guardPt));
//				} else {
					Direction dir = Orbit.orbit(guardPt, RobotType.TOWER.attackRadiusSquared);
					if (dir != Direction.NONE) {
						rc.move(dir);
					}
//				}
			}
		} else if (atkPt != null) {
			if (rc.isCoreReady()) {
				if (myLoc.distanceSquaredTo(atkPt) < 1.5 * RobotType.TOWER.attackRadiusSquared && shouldConverge(atkPt))
//					rc.setIndicatorString(0, myLoc.directionTo(atkPt).toString());
					NavSimple.walkTowards(myLoc.directionTo(atkPt));
//				} else {
				Direction dir = Orbit.orbit(atkPt, RobotType.TOWER.attackRadiusSquared);
				if (dir != Direction.NONE) {
					rc.move(dir);
				}
//				}
			}
		} else {
			rc.setIndicatorString(0, "Home");
			if (rc.isCoreReady()) {
				Direction dir = Orbit.orbit(rc.senseTowerLocations()[0], 24);
				if (dir != Direction.NONE) {
					rc.move(dir);
				}
			}
		}
	}
	
	protected static boolean shouldConverge(MapLocation loc) throws GameActionException {
		int numAllies = rc.senseNearbyRobots(myLoc, typ.sensorRadiusSquared, myTeam).length;
		int numEnemies = rc.senseNearbyRobots(loc, typ.sensorRadiusSquared, otherTeam).length; 
		return numAllies > 2 * numEnemies;
	}
	
}
