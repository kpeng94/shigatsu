package droneSurroundBot;

import battlecode.common.*;

public class NavTangentBug {
	public static int DIST_MIN = 5;
	public static int PROXIMITY = 2;
	public static int SMART_FUTURE_SENSING = 2;
	
	public static MapLocation dest;
	public static MapLocation convertedDest;
	
	public static MapLocation open;
	public static boolean closed;
	
	public static MapLocation curFollowing;
	
	public static boolean done;
	public static boolean tracing;
	public static int distTraveled;
	public static Direction[][] nextDir;
	public static Direction[][] prevDir;
	public static int[][] minDistToDest;
	
	public static void setDest(MapLocation newdest) {
		if (dest == null || newdest.x != dest.x || newdest.y != dest.y) {
			dest = newdest;
			convertedDest = MapUtils.encodeMapLocation(dest);
			nextDir = new Direction[GameConstants.MAP_MAX_WIDTH][GameConstants.MAP_MAX_HEIGHT];
			prevDir = new Direction[GameConstants.MAP_MAX_WIDTH][GameConstants.MAP_MAX_HEIGHT];
			minDistToDest = new int[GameConstants.MAP_MAX_WIDTH][GameConstants.MAP_MAX_HEIGHT];
			done = false;
			tracing = false;
			distTraveled = 0;
			
			curFollowing = Handler.rc.getLocation();
			open = curFollowing;
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
			MapLocation convertedCur = MapUtils.encodeMapLocation(open);
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
					nextDir[convertedCur.x][convertedCur.y] = forwardDir;
				} else if (forwardTile == TerrainTile.UNKNOWN) { // Unable to sense yet
					closed = true;
					return;
				} else { // Must start tracing
					Direction testDir = prevDir[convertedCur.x][convertedCur.y];
					if (testDir != null) forwardDir = testDir.opposite();
					
					Direction leftDir = forwardDir.rotateLeft(); // Check left side first
					for (int i = 8; --i >= 0;) {
						MapLocation leftPos = cur.add(leftDir);
						TerrainTile leftTile = Handler.rc.senseTerrainTile(leftPos);
						if (leftTile == TerrainTile.UNKNOWN) { // Unable to sense yet
							closed = true;
							return;
						} else if (leftTile == TerrainTile.NORMAL) {
							setNextPosInfo(leftPos, leftDir.opposite());
							MapLocation convertedLeftPos = MapUtils.encodeMapLocation(leftPos);
							nextDir[convertedCur.x][convertedCur.y] = leftDir;
							minDistToDest[convertedLeftPos.x][convertedLeftPos.y] = cur.distanceSquaredTo(dest);
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
				} else if (distTraveled >= DIST_MIN && forwardTile == TerrainTile.NORMAL && minDistToDest[convertedCur.x][convertedCur.y] > forwardPos.distanceSquaredTo(dest)) { // Discovered shorter distance
					setNextPosInfo(forwardPos, forwardDir.opposite());
					nextDir[convertedCur.x][convertedCur.y] = forwardDir;
					tracing = false;
				} else { // try tracing
					Direction testDir = prevDir[convertedCur.x][convertedCur.y].rotateLeft().rotateLeft();
					for (int i = 8; --i >= 0;) {
						MapLocation testPos = cur.add(testDir);
						TerrainTile testTile = Handler.rc.senseTerrainTile(testPos);
						if (testTile == TerrainTile.UNKNOWN) { // Unable to sense yet
							closed = true;
							return;
						} else if (testTile == TerrainTile.NORMAL) {
							setNextPosInfo(testPos, testDir.opposite());
							MapLocation convertedTest = MapUtils.encodeMapLocation(testPos);
							minDistToDest[convertedTest.x][convertedTest.y] = minDistToDest[convertedCur.x][convertedCur.y];
							nextDir[convertedCur.x][convertedCur.y] = testDir;
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
		while (Handler.myLoc.distanceSquaredTo(curFollowing) <= SMART_FUTURE_SENSING) {
			if (curFollowing.equals(dest)) return Direction.NONE; // reached destination
			MapLocation convertedPos = MapUtils.encodeMapLocation(curFollowing);
			Direction next = nextDir[convertedPos.x][convertedPos.y];
			curFollowing = curFollowing.add(next);
		}
		return Handler.myLoc.directionTo(curFollowing);
	}
	
	// Checks a position and replaces with cheaper direction and dist vector
	private static void setNextPosInfo(MapLocation pos, Direction pathVector) {
		MapLocation convertedPos = MapUtils.encodeMapLocation(pos);
		prevDir[convertedPos.x][convertedPos.y] = pathVector;
		open = pos;
	}
	
}
