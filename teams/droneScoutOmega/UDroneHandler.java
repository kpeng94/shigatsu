package droneScoutOmega;

import battlecode.common.*;

public class UDroneHandler extends UnitHandler {
	private static RobotInfo[] inRangeEnemies;
	private static Direction heading;
	
	private static double mainAxisSlope;
	private static double mainAxisIntercept;
	private static double symmAxisSlope;
	private static double symmAxisIntercept;
	
	private enum ScoutDir {
		POS, NEG;
	}
	private enum ScoutPhase {
		TO_MID,
		SWEEP_AXIS_POS, SWEEP_AXIS_NEG,
		SWEEP_AXIS_POS_BACK, SWEEP_AXIS_NEG_BACK,
		TO_HQ,
		SWEEP_LRTB;
	}
	
	private static ScoutPhase curPhase;
	private static int counter;
	private static Direction lrtbDir;
	private static ScoutDir scoutDir;
	
	public static void loop(RobotController rcon) {
		try {
			init(rcon);
		} catch (Exception e) {
			// e.printStackTr1ace();
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

	protected static void init(RobotController rcon) throws GameActionException {
		initUnit(rcon);
		curPhase = ScoutPhase.TO_MID;
		
		// Calculating the slope of the main axis that both HQs lie on
		if (myHQ.x == enemyHQ.x) {
			mainAxisSlope = Double.POSITIVE_INFINITY;
		} else {
			mainAxisSlope = (myHQ.y - enemyHQ.y) / (myHQ.x - enemyHQ.x);
		}
		
		// Calculating the intercept of the symmetry axis using my hq
		mainAxisIntercept = myHQ.y - mainAxisSlope * myHQ.x;
		
		// Calculating the slope of the symmetry axis
		if (myHQ.y == enemyHQ.y) {
			symmAxisSlope = Double.POSITIVE_INFINITY;
		} else {
			symmAxisSlope = (myHQ.x - enemyHQ.x) / (enemyHQ.y - myHQ.y);
		}
		
		// Calculating the intercept of the symmetry axis using the midpoint of the hqs
		symmAxisIntercept = ((myHQ.y + enemyHQ.y) / 2) - symmAxisSlope * ((myHQ.x + enemyHQ.x) / 2);
		
		if (Comm.readBlock(Comm.getDroneId(), 0) == 1) {
			scoutDir = ScoutDir.POS;
		} else {
			scoutDir = ScoutDir.NEG;
		}
		
		counter = 0;
		lrtbDir = Direction.NONE;
	}

	protected static void execute() throws GameActionException {
		executeUnit();
		rc.setIndicatorString(2, curPhase.toString());
		MapUtils.updateBounds(myLoc);
		if (rc.isWeaponReady()) {
			inRangeEnemies = rc.senseNearbyRobots(typ.attackRadiusSquared,
					otherTeam);
			tryAttack();
		}
		if (rc.isCoreReady()) {
			switch (curPhase) {
			case TO_MID:
				toMid();
				break;
			case SWEEP_AXIS_POS:
				sweepAxisPos();
				break;
			case SWEEP_AXIS_NEG:
				sweepAxisNeg();
				break;
			case SWEEP_LRTB:
				sweepLRTB();
				break;
			case SWEEP_AXIS_POS_BACK:
				sweepAxisPosBack();
				break;
			case SWEEP_AXIS_NEG_BACK:
				sweepAxisNegBack();
				break;
			case TO_HQ:
				toHQ();
				break;
			}
			NavSimple.walkTowards(heading);
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
	
	protected static void toMid() throws GameActionException {
		MapLocation midPoint = new MapLocation((myHQ.x + enemyHQ.x) / 2, (myHQ.y + enemyHQ.y) / 2);
		if (myLoc.distanceSquaredTo(midPoint) < 2) {
			if (scoutDir == ScoutDir.POS) {
				curPhase = ScoutPhase.SWEEP_AXIS_POS;
				sweepAxisPos();
			} else {
				curPhase = ScoutPhase.SWEEP_AXIS_NEG;
				sweepAxisNeg();
			}
		} else {
			heading = myLoc.directionTo(midPoint);
		}
	}
	
	protected static void sweepAxisPos() throws GameActionException {
		int nextX = myLoc.x + 1;
		Direction[] dirs = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
		for (Direction dir: dirs) {
			MapLocation test = myLoc.add(dir, 4);
			if (rc.senseTerrainTile(test) == TerrainTile.OFF_MAP) {
				curPhase = ScoutPhase.SWEEP_LRTB;
				lrtbDir = myLoc.directionTo(myHQ); // this isn't accurate
				sweepLRTB();
				break;
			}
		}
		double tempIntercept = myLoc.y - symmAxisSlope * myLoc.x;
		heading = myLoc.directionTo(new MapLocation(nextX, (int) (nextX * symmAxisSlope + tempIntercept)));
	}
	
	protected static void sweepAxisNeg() throws GameActionException {
		int nextX = myLoc.x - 1;
		Direction[] dirs = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
		for (Direction dir: dirs) {
			MapLocation test = myLoc.add(dir, 4);
			if (rc.senseTerrainTile(test) == TerrainTile.OFF_MAP) {
				curPhase = ScoutPhase.SWEEP_LRTB;
				lrtbDir = myLoc.directionTo(myHQ); // this isn't accurate
				sweepLRTB();
				break;
			}
		}
		double tempIntercept = myLoc.y - symmAxisSlope * myLoc.x;
		heading = myLoc.directionTo(new MapLocation(nextX, (int) (nextX * symmAxisSlope + tempIntercept)));
	}
	
	protected static void sweepLRTB() throws GameActionException {
		
		if (counter == 9) {
			counter = 0;
			if (scoutDir == ScoutDir.POS) {
				curPhase = ScoutPhase.SWEEP_AXIS_POS_BACK;
				sweepAxisPosBack();
			} else {
				curPhase = ScoutPhase.SWEEP_AXIS_NEG_BACK;
				sweepAxisNegBack();
			}
		} else {
			heading = lrtbDir;
			counter++;
		}
	}

	protected static void sweepAxisPosBack() throws GameActionException {
		int nextX = myLoc.x - 1;
		double tempIntercept = myLoc.y - symmAxisSlope * myLoc.x;
		heading = myLoc.directionTo(new MapLocation(nextX, (int) (nextX * symmAxisSlope + tempIntercept)));
		MapLocation test = myLoc.add(heading);
		int projection = (int) (test.x * mainAxisSlope + mainAxisIntercept);
		if (Math.abs(test.y - projection) <= 1) {
			curPhase = ScoutPhase.TO_HQ;
			toHQ();
		}
	}
	
	protected static void sweepAxisNegBack() throws GameActionException {
		int nextX = myLoc.x + 1;
		double tempIntercept = myLoc.y - symmAxisSlope * myLoc.x;
		heading = myLoc.directionTo(new MapLocation(nextX, (int) (nextX * symmAxisSlope + tempIntercept)));
		MapLocation test = myLoc.add(heading);
		int projection = (int) (test.x * mainAxisSlope + mainAxisIntercept);
		if (Math.abs(test.y - projection) <= 1) {
			curPhase = ScoutPhase.TO_HQ;
			toHQ();
		}
	}
	
	protected static void toHQ() throws GameActionException {
		
		if (counter == 7) {
			counter = 0;
			if (scoutDir == ScoutDir.POS) {
				curPhase = ScoutPhase.SWEEP_AXIS_POS;
				sweepAxisPos();
			} else {
				curPhase = ScoutPhase.SWEEP_AXIS_NEG;
				sweepAxisNeg();
			}
		} else {
			heading = myLoc.directionTo(myHQ);
			counter++;
		}
	}
}
