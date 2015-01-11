package droneScoutOmega;

import java.util.Arrays;

import battlecode.common.*;

public class UDroneHandler extends UnitHandler {
	private static RobotInfo[] inRangeEnemies;
	private static Direction heading;
	
	private static double mainAxisSlope;
	private static double symmAxisSlope;
	
	private static MapLocation refIntercept;
	
	private enum ScoutDir {
		POS, NEG;
	}
	private enum ScoutPhase {
		TO_MID,
		SWEEP_AXIS_TO,
		SWEEP_AXIS_BACK,
		TO_HQ,
		SWEEP_LRTB;
	}
	
	private static ScoutPhase curPhase;
	private static int counter;
	private static Direction[] validLRTBDirs;
	private static Direction curLRTBDir;
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
		refIntercept = new MapLocation((myHQ.x + enemyHQ.x) / 2, (myHQ.y + enemyHQ.y) / 2);
		
		// Calculating the slope of the main axis that both HQs lie on
		if (myHQ.x == enemyHQ.x) {
			mainAxisSlope = Double.POSITIVE_INFINITY;
		} else {
			mainAxisSlope = 1.0 * (myHQ.y - enemyHQ.y) / (myHQ.x - enemyHQ.x);
		}
		
		// Calculating the slope of the symmetry axis
		if (myHQ.y == enemyHQ.y) {
			symmAxisSlope = Double.POSITIVE_INFINITY;
		} else {
			symmAxisSlope = 1.0 * (myHQ.x - enemyHQ.x) / (enemyHQ.y - myHQ.y);
		}
		
		if (Comm.readBlock(Comm.getDroneId(), 0) == 1) {
			scoutDir = ScoutDir.POS;
		} else {
			scoutDir = ScoutDir.NEG;
		}
		
		counter = 0;
		curLRTBDir = Direction.NONE;
		
		validLRTBDirs = new Direction[4];
		for (int i = 0; i < 4; i++) {
			validLRTBDirs[i] = Direction.NONE;
		}
	}

	protected static void execute() throws GameActionException {
		executeUnit();
		MapUtils.updateBounds(myLoc);
		if (rc.isCoreReady()) {
			rc.setIndicatorString(1, "" + Arrays.toString(validLRTBDirs));
			switch (curPhase) {
			case TO_MID:
				toMid();
				break;
			case SWEEP_AXIS_TO:
				sweepAxisTo();
				break;
			case SWEEP_LRTB:
				sweepLRTB();
				break;
			case SWEEP_AXIS_BACK:
				sweepAxisBack();
				break;
			case TO_HQ:
				toHQ();
				break;
			}
			NavSimple.walkTowards(heading);
		}
	}
	
	protected static void toMid() throws GameActionException {
		if (myLoc.distanceSquaredTo(refIntercept) < 2) {
			curPhase = ScoutPhase.SWEEP_AXIS_TO;
			sweepAxisTo();
		} else {
			heading = myLoc.directionTo(refIntercept);
		}
	}
	
	protected static void sweepAxisTo() throws GameActionException {
		int nextX = myLoc.x;
		if (scoutDir == ScoutDir.POS) {
			nextX++;
		} else {
			nextX--;
		}
		
		Direction[] dirs = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
		for (Direction dir: dirs) {
			MapLocation test = myLoc.add(dir, 4);
			if (rc.senseTerrainTile(test) == TerrainTile.OFF_MAP) {
				// Find valid LRTB directions, discard if results in enemy territory, to off map, or back towards midpoint
				if (curLRTBDir == Direction.NONE) {
					int numValidDir = 0;
					for (Direction dir2: dirs) {
						MapLocation test2 = myLoc.add(dir2, 4);
						if (rc.senseTerrainTile(test2) == TerrainTile.OFF_MAP) {
							continue;
						} else if (test2.distanceSquaredTo(enemyHQ) < test2.distanceSquaredTo(myHQ)) {
							continue;
						} else if (dir2 == test2.directionTo(refIntercept)) {
							continue;
						} else {
							validLRTBDirs[numValidDir] = dir2;
							numValidDir++;
						}
					}
				}
				curLRTBDir = tangentDir(dir, validLRTBDirs);
				curPhase = ScoutPhase.SWEEP_LRTB;
				sweepLRTB();
				break;
			}
		}
		heading = myLoc.directionTo(new MapLocation(nextX, (int) (nextX * symmAxisSlope + (refIntercept.y - symmAxisSlope * refIntercept.x))));
	}
	
	protected static void sweepLRTB() throws GameActionException {
		
		if (counter == 9) {
			counter = 0;
			curPhase = ScoutPhase.SWEEP_AXIS_BACK;
			sweepAxisBack();
		} else {
			// change the direction if we reached an edge
			MapLocation test = myLoc.add(curLRTBDir, 4);
			if (rc.senseTerrainTile(test) == TerrainTile.OFF_MAP) {
				curLRTBDir = tangentDir(curLRTBDir,validLRTBDirs);
			}
			heading = curLRTBDir;
			refIntercept = refIntercept.add(heading);
			counter++;
		}
	}

	protected static void sweepAxisBack() throws GameActionException {
		int nextX = myLoc.x;
		if (scoutDir == ScoutDir.POS) {
			nextX--;
		} else {
			nextX++;
		}
		
		heading = myLoc.directionTo(new MapLocation(nextX, (int) (nextX * symmAxisSlope + (refIntercept.y - symmAxisSlope * refIntercept.x))));
		MapLocation test = myLoc.add(heading);
		int projection = (int) (test.x * mainAxisSlope + (myHQ.y - mainAxisSlope * myHQ.x));
		if (Math.abs(test.y - projection) <= 1) {
			curPhase = ScoutPhase.TO_HQ;
			toHQ();
		}
	}
	
	protected static void toHQ() throws GameActionException {
		
		if (counter == 7) {
			counter = 0;
			curPhase = ScoutPhase.SWEEP_AXIS_TO;
			sweepAxisTo();
		} else {
			heading = myLoc.directionTo(myHQ);
			refIntercept = refIntercept.add(heading);
			counter++;
		}
	}
	
	protected static Direction tangentDir(Direction ray, Direction[] validDirs) {
		for (Direction dir: validDirs) {
			if ((ray == Direction.NORTH || ray == Direction.SOUTH) && (dir == Direction.WEST || dir == Direction.EAST))
				return dir;
			if ((ray == Direction.WEST || ray == Direction.EAST) && (dir == Direction.NORTH || dir == Direction.SOUTH))
				return dir;
		}
		return validDirs[0];
	}
}
