package droneScoutZeta;

import battlecode.common.*;

public class UDroneHandler extends UnitHandler {
	private static RobotInfo[] inRangeEnemies;
	private static Direction heading;
	private enum ScoutPhase {
		FINDING_LR_MARGIN, FINDING_TB_MARGIN, SWEEPING_LONG_TO, SWEEPING_LONG_BACK, SWEEPING_SHORT, SWEEPING_DIAG;
	}
	
	private static ScoutPhase curPhase;
	private static int counter;
	private static int width;
	private static int minX;
	private static int minY;
	
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
		curPhase = ScoutPhase.FINDING_LR_MARGIN;
		counter = 0;
	}

	protected static void execute() throws GameActionException {
		executeUnit();
		if (rc.isWeaponReady()) {
			inRangeEnemies = rc.senseNearbyRobots(typ.attackRadiusSquared,
					otherTeam);
			tryAttack();
		}
		if (rc.isCoreReady()) {
			switch (curPhase) {
			case FINDING_LR_MARGIN:
				findLRMargin();
				break;
			case FINDING_TB_MARGIN:
				findTBMargin();
				break;
			case SWEEPING_LONG_TO:
				sweepLongTo();
				break;
			case SWEEPING_LONG_BACK:
				sweepLongBack();
				break;
			case SWEEPING_SHORT:
				sweepShort();
				break;
			case SWEEPING_DIAG:
				sweepDiag();
				break;
			}
			NavSimple.walkTowards(heading);
			
			int[] bounds = MapUtils.getDiscoveredBounds();
			MapUtils.updateBounds(myLoc);
			
			rc.setIndicatorString(0, "Bounds at [" + bounds[0] + "-" + bounds[1] + ", " + bounds[2] + "-" + bounds[3] + "]");
			rc.setIndicatorString(1, "width is " + width);
			rc.setIndicatorString(2, "mins are " + minX + "," + minY);
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
	
	protected static void findLRMargin() throws GameActionException {
		if (myHQ.x < enemyHQ.x) {
			heading = Direction.WEST;
		} else {
			heading = Direction.EAST;
		}
		// If we found the margin, switch to finding TB
		for (int i = 1; i < 5; i++) {
			MapLocation loc = myLoc.add(heading, i);
			if (rc.senseTerrainTile(loc) == TerrainTile.OFF_MAP) {
				int margin = Math.abs(myHQ.x - myLoc.x) + i - 1;
				int[] bounds = MapUtils.getDiscoveredBounds();
				bounds[0] -= margin;
				bounds[1] += margin;
				minX = bounds[0];
				width = bounds[1] - bounds[0];
				MapUtils.writeBounds(bounds);
				curPhase = ScoutPhase.FINDING_TB_MARGIN;
				findTBMargin();
			}
		}
	}
	
	protected static void findTBMargin() throws GameActionException {
		if (myHQ.y < enemyHQ.y) {
			heading = Direction.NORTH;
		} else {
			heading = Direction.SOUTH;
		}
		// If we found the margin, switch to finding TB
		for (int i = 1; i < 5; i++) {
			MapLocation loc = myLoc.add(heading, i);
			if (rc.senseTerrainTile(loc) == TerrainTile.OFF_MAP) {
				int margin = Math.abs(myHQ.y - myLoc.y) + i - 1;
				int[] bounds = MapUtils.getDiscoveredBounds();
				bounds[2] -= margin;
				bounds[3] += margin;
				minY = bounds[2];
				MapUtils.writeBounds(bounds);
				curPhase = ScoutPhase.SWEEPING_LONG_TO;
				sweepLongTo();
			}
		}
	}
	
	protected static void sweepLongTo() throws GameActionException {
		if (myHQ.x < enemyHQ.x && myHQ.y < enemyHQ.y) {
			heading = Direction.EAST;
		} else if (myHQ.x < enemyHQ.x && myHQ.y > enemyHQ.y) {
			heading = Direction.NORTH;
		} else if (myHQ.x > enemyHQ.x && myHQ.y < enemyHQ.y) {
			heading = Direction.SOUTH;
		} else {
			heading = Direction.WEST;
		}
		// If we moved 9, switch to sweeping diagonally
		if (myLoc.x - minX + myLoc.y - minY == width) {
			curPhase = ScoutPhase.SWEEPING_DIAG;
			sweepDiag();
		}
	}
	
	protected static void sweepDiag() throws GameActionException {
		if (myHQ.x < enemyHQ.x && myHQ.y < enemyHQ.y) {
			heading = Direction.SOUTH_WEST;
		} else if (myHQ.x < enemyHQ.x && myHQ.y > enemyHQ.y) {
			heading = Direction.SOUTH_EAST;
		} else if (myHQ.x > enemyHQ.x && myHQ.y < enemyHQ.y) {
			heading = Direction.NORTH_WEST;
		} else {
			heading = Direction.NORTH_EAST;
		}
		// If we moved 7, switch to sweeping diagonally
		if (counter == 7) {
			counter = 0;
			curPhase = ScoutPhase.SWEEPING_LONG_BACK;
			sweepLongBack();
		}
		else {
			counter++;
		}	
	}
	
	protected static void sweepLongBack() throws GameActionException {
		if (myHQ.x < enemyHQ.x && myHQ.y < enemyHQ.y) {
			heading = Direction.WEST;
		} else if (myHQ.x < enemyHQ.x && myHQ.y > enemyHQ.y) {
			heading = Direction.SOUTH;
		} else if (myHQ.x > enemyHQ.x && myHQ.y < enemyHQ.y) {
			heading = Direction.NORTH;
		} else {
			heading = Direction.EAST;
		}
		if (rc.senseTerrainTile(myLoc.add(heading, 4)) == TerrainTile.OFF_MAP) {
			curPhase = ScoutPhase.SWEEPING_SHORT;
			sweepShort();
		}
	}
	
	protected static void sweepShort() throws GameActionException {
		if (myHQ.x < enemyHQ.x && myHQ.y < enemyHQ.y) {
			heading = Direction.SOUTH;
		} else if (myHQ.x < enemyHQ.x && myHQ.y > enemyHQ.y) {
			heading = Direction.EAST;
		} else if (myHQ.x > enemyHQ.x && myHQ.y < enemyHQ.y) {
			heading = Direction.WEST;
		} else {
			heading = Direction.NORTH;
		}
		// If we moved 9, switch to sweeping diagonally
		if (counter == 9) {
			counter = 0;
			curPhase = ScoutPhase.SWEEPING_LONG_TO;
			sweepLongTo();
		}
		else {
			counter++;
		}
	}
}
