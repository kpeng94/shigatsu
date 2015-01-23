package genghisPostSeedRework;

import battlecode.common.*;

public class NavTangentBug {
	public static final int DIST_MIN = 5;
	public static final int PROXIMITY = 2;
	public static final int SMART_FUTURE_SENSING = 2;
	public static final int CHECK_LIMIT = 5;
	
	public static MapLocation dest;
	public static MapLocation convertedDest;
	
	public static MapLocation open;
	public static boolean closed;
	
	public static MapLocation curFollowing;
	
	public static boolean done;
	public static boolean tracing;
	public static int distTraveled;
	public static Direction[] nextDir;
	public static Direction[] prevDir;
	public static int[] minDistToDest;
	
	public static void setDestForced(MapLocation newdest) {
		dest = newdest;
		convertedDest = MapUtils.encodeMapLocation(dest);
		nextDir = new Direction[1 << 16];
		prevDir = new Direction[1 << 16];
		minDistToDest = new int[1 << 16];
		done = false;
		tracing = false;
		distTraveled = 0;
		
		curFollowing = Handler.myLoc;
		open = curFollowing;
	}
	
	public static void setDest(MapLocation newdest) {
		if (dest == null || newdest.x != dest.x || newdest.y != dest.y) {
			setDestForced(newdest);
		}
	}
	
	public static void calculate(int bytecodeLimit) {
		if (done) return;
		boolean recheckClosed = false;
		while (Clock.getBytecodeNum() < bytecodeLimit) {
			if (closed) { // Move closed to open
				if (recheckClosed) return;
				closed = false;
				recheckClosed = true;
			}
			MapLocation cur = open;
			int encodedCur = MapUtils.encode(open);
			if (cur.equals(dest)) {
				done = true;
				return;
			}
			
			if (!tracing) { // not tracing
				Direction forwardDir = cur.directionTo(dest);
				MapLocation forwardPos = cur.add(forwardDir);
				TerrainTile forwardTile = Handler.rc.senseTerrainTile(forwardPos); // Checks forward
				
				if (forwardTile == TerrainTile.NORMAL) { // Can move forward
					setNextPosInfo(forwardPos, forwardDir.opposite());
					nextDir[encodedCur] = forwardDir;
				} else if (forwardTile == TerrainTile.UNKNOWN) { // Unable to sense yet
					closed = true;
					return;
				} else { // Must start tracing
					Direction testDir = prevDir[encodedCur];
					if (testDir != null) forwardDir = testDir.opposite();
					
					Direction leftDir = forwardDir.rotateLeft(); // Check left side first
					for (int i = 7; --i >= 0;) {
						MapLocation leftPos = cur.add(leftDir);
						TerrainTile leftTile = Handler.rc.senseTerrainTile(leftPos);
						if (leftTile == TerrainTile.UNKNOWN) { // Unable to sense yet
							closed = true;
							return;
						} else if (leftTile == TerrainTile.NORMAL) {
							setNextPosInfo(leftPos, leftDir.opposite());
							int encodedLeftPos = MapUtils.encode(leftPos);
							nextDir[encodedCur] = leftDir;
							minDistToDest[encodedLeftPos] = cur.distanceSquaredTo(dest);
							distTraveled = 0;
							tracing = true;
							break;
						}
						leftDir = leftDir.rotateLeft();
					}
					// This should never happen???
				}
			} else { // Tracing
				Direction forwardDir = cur.directionTo(dest);
				MapLocation forwardPos = cur.add(forwardDir);
				TerrainTile forwardTile = Handler.rc.senseTerrainTile(forwardPos);
				if (forwardTile == TerrainTile.UNKNOWN) {
					closed = true;
					return;
				} else if (distTraveled >= DIST_MIN && forwardTile == TerrainTile.NORMAL && minDistToDest[encodedCur] > forwardPos.distanceSquaredTo(dest)) { // Discovered shorter distance
					setNextPosInfo(forwardPos, forwardDir.opposite());
					nextDir[encodedCur] = forwardDir;
					tracing = false;
				} else { // try tracing
					Direction testDir = prevDir[encodedCur].rotateLeft().rotateLeft();
					for (int i = 8; --i >= 0;) {
						MapLocation testPos = cur.add(testDir);
						TerrainTile testTile = Handler.rc.senseTerrainTile(testPos);
						if (testTile == TerrainTile.UNKNOWN) { // Unable to sense yet
							closed = true;
							return;
						} else if (testTile == TerrainTile.NORMAL) {
							setNextPosInfo(testPos, testDir.opposite());
							int encodedTest = MapUtils.encode(testPos);
							minDistToDest[encodedTest] = minDistToDest[encodedCur];
							nextDir[encodedCur] = testDir;
							distTraveled++;
							break;
						}
						testDir = testDir.rotateLeft();
					}
				}
			}
		}
	}
	
	public static Direction getNextMove() {
		if (Handler.myLoc.distanceSquaredTo(dest) < PROXIMITY) return Direction.NONE;
		int checkAmount = 0;
		while (Handler.myLoc.distanceSquaredTo(curFollowing) <= SMART_FUTURE_SENSING && checkAmount < CHECK_LIMIT) {
			if (curFollowing.equals(dest)) return Direction.NONE; // reached destination
			int encodedPos = MapUtils.encode(curFollowing);
			Direction next = nextDir[encodedPos];
			if (next == null) return Handler.myLoc.directionTo(curFollowing);
			curFollowing = curFollowing.add(next);
			checkAmount++;
		}
		return Handler.myLoc.directionTo(curFollowing);
	}
	
	// Checks a position and replaces with cheaper direction and dist vector
	private static void setNextPosInfo(MapLocation pos, Direction pathVector) {
		int encodedPos = MapUtils.encode(pos);
		prevDir[encodedPos] = pathVector;
		open = pos;
	}
	
}
