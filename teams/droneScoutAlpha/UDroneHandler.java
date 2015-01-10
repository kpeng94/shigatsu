package droneScoutAlpha;

import battlecode.common.*;

public class UDroneHandler extends UnitHandler {
	private static RobotInfo[] inRangeEnemies;
	private static Direction heading;
	private enum ScoutPhase {
		FINDING_LR_MARGIN, FINDING_TB_MARGIN, SWEEPING_LR, SWEEPING_TB, SWEEPING_LR_TO_TB, SWEEPING_TB_TO_LR;
	}
	
	private static ScoutPhase curPhase;
	private static int counter;
	
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
			case SWEEPING_LR:
				sweepLR();
				break;
			case SWEEPING_TB:
				sweepTB();
				break;
			case SWEEPING_LR_TO_TB:
				sweepLRToTB();
				break;
			case SWEEPING_TB_TO_LR:
				sweepTBToLR();
				break;
			}
			NavSimple.walkTowards(heading);
			
			int[] bounds = MapUtils.getDiscoveredBounds();
			MapUtils.updateBounds(myLoc);
			
			rc.setIndicatorString(0, "Bounds at [" + bounds[0] + "-" + bounds[1] + ", " + bounds[2] + "-" + bounds[3] + "]");
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
				MapUtils.writeBounds(bounds);
				curPhase = ScoutPhase.SWEEPING_LR;
				sweepLR();
			}
		}
	}
	
	protected static void sweepLR() throws GameActionException {
		if (myHQ.x < enemyHQ.x) {
			heading = Direction.EAST;
		} else {
			heading = Direction.WEST;
		}
		// If we moved 8, switch to sweeping diagonally
		if (counter == 8) {
			counter = 0;
			curPhase = ScoutPhase.SWEEPING_LR_TO_TB;
			sweepLRToTB();
		}
		else {
			counter++;
		}	
	}
	
	protected static void sweepLRToTB() throws GameActionException {
		if (myHQ.x < enemyHQ.x && myHQ.y < enemyHQ.y) {
			heading = Direction.SOUTH_WEST;
			MapLocation loc1 = myLoc.add(Direction.SOUTH, 4);
			MapLocation loc2 = myLoc.add(Direction.WEST, 4);
			if (rc.senseTerrainTile(loc1) == TerrainTile.OFF_MAP || rc.senseTerrainTile(loc2) == TerrainTile.OFF_MAP) {
				curPhase = ScoutPhase.SWEEPING_TB;
				sweepTB();
			}
		} else if (myHQ.x < enemyHQ.x && myHQ.y > enemyHQ.y) {
			heading = Direction.NORTH_WEST;
			MapLocation loc1 = myLoc.add(Direction.NORTH, 4);
			MapLocation loc2 = myLoc.add(Direction.WEST, 4);
			if (rc.senseTerrainTile(loc1) == TerrainTile.OFF_MAP || rc.senseTerrainTile(loc2) == TerrainTile.OFF_MAP) {
				curPhase = ScoutPhase.SWEEPING_TB;
				sweepTB();
			}
		} else if (myHQ.x > enemyHQ.x && myHQ.y < enemyHQ.y) {
			heading = Direction.SOUTH_EAST;
			MapLocation loc1 = myLoc.add(Direction.SOUTH, 4);
			MapLocation loc2 = myLoc.add(Direction.EAST, 4);
			if (rc.senseTerrainTile(loc1) == TerrainTile.OFF_MAP || rc.senseTerrainTile(loc2) == TerrainTile.OFF_MAP) {
				curPhase = ScoutPhase.SWEEPING_TB;
				sweepTB();
			}
		} else {
			heading = Direction.NORTH_EAST;
			MapLocation loc1 = myLoc.add(Direction.NORTH, 4);
			MapLocation loc2 = myLoc.add(Direction.EAST, 4);
			if (rc.senseTerrainTile(loc1) == TerrainTile.OFF_MAP || rc.senseTerrainTile(loc2) == TerrainTile.OFF_MAP) {
				curPhase = ScoutPhase.SWEEPING_TB;
				sweepTB();
			}
		}
	}
	
	protected static void sweepTB() throws GameActionException {
		if (myHQ.y < enemyHQ.y) {
			heading = Direction.SOUTH;
		} else {
			heading = Direction.NORTH;
		}
		// If we moved 8, switch to sweeping diagonally
		if (counter == 8) {
			counter = 0;
			curPhase = ScoutPhase.SWEEPING_TB_TO_LR;
			sweepTBToLR();
		}
		else {
			counter++;
		}
	}
	
	protected static void sweepTBToLR() throws GameActionException {
		if (myHQ.x < enemyHQ.x && myHQ.y < enemyHQ.y) {
			heading = Direction.NORTH_EAST;
			MapLocation loc1 = myLoc.add(Direction.NORTH, 4);
			MapLocation loc2 = myLoc.add(Direction.EAST, 4);
			if (rc.senseTerrainTile(loc1) == TerrainTile.OFF_MAP || rc.senseTerrainTile(loc2) == TerrainTile.OFF_MAP) {
				curPhase = ScoutPhase.SWEEPING_LR;
				sweepLR();
			}
		} else if (myHQ.x < enemyHQ.x && myHQ.y > enemyHQ.y) {
			heading = Direction.SOUTH_EAST;
			MapLocation loc1 = myLoc.add(Direction.SOUTH, 4);
			MapLocation loc2 = myLoc.add(Direction.EAST, 4);
			if (rc.senseTerrainTile(loc1) == TerrainTile.OFF_MAP || rc.senseTerrainTile(loc2) == TerrainTile.OFF_MAP) {
				curPhase = ScoutPhase.SWEEPING_LR;
				sweepLR();
			}
		} else if (myHQ.x > enemyHQ.x && myHQ.y < enemyHQ.y) {
			heading = Direction.NORTH_WEST;
			MapLocation loc1 = myLoc.add(Direction.NORTH, 4);
			MapLocation loc2 = myLoc.add(Direction.WEST, 4);
			if (rc.senseTerrainTile(loc1) == TerrainTile.OFF_MAP || rc.senseTerrainTile(loc2) == TerrainTile.OFF_MAP) {
				curPhase = ScoutPhase.SWEEPING_LR;
				sweepLR();
			}
		} else {
			heading = Direction.SOUTH_WEST;
			MapLocation loc1 = myLoc.add(Direction.SOUTH, 4);
			MapLocation loc2 = myLoc.add(Direction.WEST, 4);
			if (rc.senseTerrainTile(loc1) == TerrainTile.OFF_MAP || rc.senseTerrainTile(loc2) == TerrainTile.OFF_MAP) {
				curPhase = ScoutPhase.SWEEPING_LR;
				sweepLR();
			}
		}	
	}
}
