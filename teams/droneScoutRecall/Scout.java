package droneScoutRecall;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class Scout {
	
	private static RobotController rc;
	private static MapLocation myLoc;
	private static MapLocation myHQ;
	private static MapLocation enemyHQ;
	
	/**
	 * Returns if a scout drone is needed
	 */
	public static boolean needed() throws GameActionException {
		return assignType() != null;
	}
	
	/**
	 * Executes Scout behavior
	 */
	public static void init() throws GameActionException {
		initNav();
		initRecall();
	}
	
	/**
	 * Executes Scout behavior
	 */
	public static void execute() throws GameActionException {
		
		rc = Handler.rc;
		myLoc = rc.getLocation();
		myHQ = rc.senseHQLocation();
		enemyHQ = rc.senseEnemyHQLocation();
		
		updateRecall();
		updateBounds(myLoc);
		if (rc.isCoreReady()) {
			executeNav();
		}
	}
	
	/*-------------------------------- NAV FUNCTIONS --------------------------------*/
	
	private static Direction heading;
	
	private static double mainAxisSlope;
	private static double symmAxisSlope;
	
	private static MapLocation refIntercept;
	
	private static int counter;
	private static Direction[] validLRTBDirs;
	private static Direction curLRTBDir;
	
	private static void initNav() {
		
		MapLocation myHQ = Handler.rc.senseHQLocation();
		MapLocation enemyHQ = Handler.rc.senseEnemyHQLocation();
		
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
		
		counter = 0;
		curLRTBDir = Direction.NONE;
		
		validLRTBDirs = new Direction[4];
		for (int i = 0; i < 4; i++) {
			validLRTBDirs[i] = Direction.NONE;
		}
	}
	
	private static final void executeNav() throws GameActionException {
		if (sPhase == ScoutPhase.NONE)
			sPhase = ScoutPhase.TO_MID;
		switch (sPhase) {
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
		case RECALLING:
			heading = myLoc.directionTo(recallLoc);
		default:
			break;
		}
		NavSimple.walkTowards(heading);
	}
	
	protected static void toMid() throws GameActionException {
		if (myLoc.distanceSquaredTo(refIntercept) < 2) {
			sPhase = ScoutPhase.SWEEP_AXIS_TO;
		} else {
			heading = myLoc.directionTo(refIntercept);
		}
	}
	
	protected static void sweepAxisTo() throws GameActionException {
		int nextX = myLoc.x;
		if (sType == ScoutType.POS) {
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
				sPhase = ScoutPhase.SWEEP_LRTB;
				sweepLRTB();
				break;
			}
		}
		heading = myLoc.directionTo(new MapLocation(nextX, (int) (nextX * symmAxisSlope + (refIntercept.y - symmAxisSlope * refIntercept.x))));
	}
	
	protected static void sweepLRTB() throws GameActionException {
		
		if (counter == 9) {
			counter = 0;
			sPhase = ScoutPhase.SWEEP_AXIS_BACK;
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
		if (sType == ScoutType.POS) {
			nextX--;
		} else {
			nextX++;
		}
		
		heading = myLoc.directionTo(new MapLocation(nextX, (int) (nextX * symmAxisSlope + (refIntercept.y - symmAxisSlope * refIntercept.x))));
		MapLocation test = myLoc.add(heading);
		int projection = (int) (test.x * mainAxisSlope + (myHQ.y - mainAxisSlope * myHQ.x));
		if (Math.abs(test.y - projection) <= 1) {
			sPhase = ScoutPhase.TO_HQ;
			toHQ();
		}
	}
	
	protected static void toHQ() throws GameActionException {
		
		if (counter == 7) {
			counter = 0;
			sPhase = ScoutPhase.SWEEP_AXIS_TO;
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

	/*-------------------------------- RECALL FUNCTIONS --------------------------------*/
	
	// The recall info channels hold the time since the last update, the phase
	// and map location
	private static final int RECALL_INFO_OFFSET = 10;
	private static final int RECALL_INFO_SIZE = 4;
	private static final int EXPIRED_THRESHOLD = 5;
	
	private static ScoutType sType;
	private static ScoutPhase sPhase;
	private static MapLocation recallLoc;
	
	private static int baseOff;
	
	public enum ScoutType {
		POS, NEG;
	}

	private enum ScoutPhase {
		NONE, RECALLING, TO_MID, SWEEP_AXIS_TO, SWEEP_AXIS_BACK, TO_HQ, SWEEP_LRTB;
	}

	/**
	 * Increments the ages for each ScoutType
	 */
	public static void incRecallAges() throws GameActionException {
		for (ScoutType type : ScoutType.values()) {
			int baseOff = typeToOffset(type);
			int age = Comm.readBlock(getScoutId(), baseOff);
			Comm.writeBlock(getScoutId(), baseOff, age + 1);
		}
	}
	
	/**
	 * Initializes Scout behavior
	 */
	private static void initRecall() throws GameActionException {
		sType = assignType();
		baseOff = typeToOffset(sType);
		recallLoc = new MapLocation(
				Comm.readBlock(getScoutId(), baseOff + 2),
				Comm.readBlock(getScoutId(), baseOff + 3));
		if (Comm.readBlock(getScoutId(), baseOff + 1) > 0)
			sPhase = ScoutPhase.RECALLING;
		else
			sPhase = ScoutPhase.NONE;
	}
	
	/**
	 * Converts a ScoutType to the base offset  
	 */
	private static int typeToOffset(ScoutType type) throws GameActionException {
		return RECALL_INFO_OFFSET + RECALL_INFO_SIZE * type.ordinal();
	}
	
	/**
	 * Returns the first ScoutType that is assumed to be inactive (last updated
	 * more than EXPIRED_THRESHOLD rounds ago) Returns null if all ScoutTypes
	 * are active
	 */
	private static ScoutType assignType() throws GameActionException {
		ScoutType inactive = null;
		for (ScoutType type : ScoutType.values()) {
			int baseOffset = typeToOffset(type);
			if (Comm.readBlock(getScoutId(), baseOffset) >= 0) {
				inactive = type;
				break;
			}
		}
		return inactive;
	}

	/**
	 * Resets the ages for the given ScoutType 
	 * Updates the values
	 */
	private static void updateRecall() throws GameActionException {
		
		MapLocation myLoc = rc.getLocation();
		
		// Reset the age 
		Comm.writeBlock(getScoutId(), baseOff, -EXPIRED_THRESHOLD);
		
		// Update the values if we've made progress
		if (sPhase != ScoutPhase.RECALLING) { 
			Comm.writeBlock(getScoutId(), baseOff + 1, sPhase.ordinal());
			Comm.writeBlock(getScoutId(), baseOff + 2, myLoc.x);
			Comm.writeBlock(getScoutId(), baseOff + 3, myLoc.y);
		}
		// Finish remaining initialization if we're recalling and we've reached the recall loc
		else if (myLoc.equals(recallLoc)) {
			sPhase = ScoutPhase.values()[Comm.readBlock(getScoutId(), baseOff + 1)];
		}
	}

	/*-------------------------------- BOUNDS FUNCTIONS --------------------------------*/
	
	private static final int[] BOUND_OFFSETS = { 0, 1, 2, 3 };

	// Bounds are ordered minX, maxX, minY, maxY
	public static void initBounds() throws GameActionException {
		Comm.writeBlock(getScoutId(), BOUND_OFFSETS[0], Integer.MAX_VALUE);
		Comm.writeBlock(getScoutId(), BOUND_OFFSETS[1], 0);
		Comm.writeBlock(getScoutId(), BOUND_OFFSETS[2], Integer.MAX_VALUE);
		Comm.writeBlock(getScoutId(), BOUND_OFFSETS[3], 0);
	}

	public static int[] getDiscoveredBounds() throws GameActionException {
		int[] bounds = new int[4];
		for (int i = 0; i < 4; i++) {
			bounds[i] = Comm.readBlock(getScoutId(), BOUND_OFFSETS[i]);
		}
		return bounds;
	}

	public static void updateBounds(MapLocation loc) throws GameActionException {
		int[] bounds = getDiscoveredBounds();
		if (loc.x < bounds[0]) {
			bounds[0] = loc.x;
		}
		if (loc.x > bounds[1]) {
			bounds[1] = loc.x;
		}
		if (loc.y < bounds[2]) {
			bounds[2] = loc.y;
		}
		if (loc.y > bounds[3]) {
			bounds[3] = loc.y;
		}
		writeBounds(bounds);
	}

	public static void writeBounds(int[] bounds) throws GameActionException {
		for (int i = 0; i < 4; i++) {
			Comm.writeBlock(getScoutId(), BOUND_OFFSETS[i], bounds[i]);
		}
	}

	/*-------------------------------- COMM FUNCTIONS --------------------------------*/
	
	public static final int SCOUT_BLOCK = 200;
	public static int scoutBlockId = 0;

	/**
	 * Returns the block id of the dedicated scout block Creates a scout block
	 * if it was not previously
	 */
	public static int getScoutId() throws GameActionException {
		if (scoutBlockId == 0) {
			scoutBlockId = Handler.rc.readBroadcast(SCOUT_BLOCK);
			if (scoutBlockId == 0) {
				scoutBlockId = Comm.requestBlock(true);
				Handler.rc.broadcast(SCOUT_BLOCK, scoutBlockId);
			}
		}
		return scoutBlockId;
	}
}
