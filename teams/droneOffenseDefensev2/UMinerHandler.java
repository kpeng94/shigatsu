package droneOffenseDefensev2;

import battlecode.common.*;

public class UMinerHandler extends UnitHandler {
	private static RobotInfo[] inRangeEnemies;

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

	protected static void init(RobotController rcon) {
		initUnit(rcon);
	}

	protected static void execute() throws GameActionException {
		executeUnit();
		if (rc.isWeaponReady()) {
			inRangeEnemies = rc.senseNearbyRobots(typ.attackRadiusSquared,
					otherTeam);
			tryAttack();
		}
		if (rc.isCoreReady()) {
			MapLocation ml = findClosestMinableOre(rc, ORE_THRESHOLD_MINER, 6);
			RobotInfo[] nearbyRobots = rc.senseNearbyRobots(myLoc, 2, myTeam);
			if (nearbyRobots.length > 2) {
				if (ml != null) {
					NavSimple.walkTowards(myLoc.directionTo(ml));
				}
			} else if (rc.senseOre(myLoc) >= ORE_THRESHOLD_MINER
					&& rc.canMine()) {
				rc.mine();
			} else {
				if (ml != null) {
					rc.setIndicatorString(0, "" + nearbyRobots.length);
					rc.setIndicatorString(1, "" + myLoc.directionTo(ml));
					NavSimple.walkTowards(myLoc.directionTo(ml));
				}
			}
		}
	}
	
	protected static void tryAttack() throws GameActionException {
		if (inRangeEnemies.length > 0) {
			MapLocation minLoc = inRangeEnemies[0].location;
			int minRange = myLoc.distanceSquaredTo(minLoc);
			for (int i = inRangeEnemies.length - 1; i > 0; i--) { // Get minimum
																	// in array
				RobotInfo enemy = inRangeEnemies[i];
				MapLocation enemyLoc = enemy.location;
				int enemyRange = myLoc.distanceSquaredTo(enemyLoc);
				if (enemyRange < minRange) {
					minRange = enemyRange;
					minLoc = enemyLoc;
				}
			}

			rc.attackLocation(minLoc);
		}
	}
	
	/**
	 * Calculates the closest square with at least the threshold amount of 
	 * ore. The distance is calculated in terms of Manhattan distance and NOT
	 * Euclidean distance. This does NOT factor in the square the robot is currently on.
	 * Ignores squares with other robots on them already
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
						.add(MapUtils.dirs[currentDirection]);
				if (rc.senseOre(currentLocation) > threshold && rc.canMove(MapUtils.dirs[currentDirection]))
					return currentLocation;
			}
			currentDirection = (currentDirection + 2) % 8;
			for (int i = 0; i < step; i++) {
				currentLocation = currentLocation.add(MapUtils.dirs[currentDirection]);
				if (rc.senseOre(currentLocation) > threshold && rc.canMove(MapUtils.dirs[currentDirection]))
					return currentLocation;
			}
			currentDirection = (currentDirection + 2) % 8;
			
			step++;
		}

		return null;
	}
	
}
