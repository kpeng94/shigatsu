package tangentBugBot;

import battlecode.common.*;

public class NavTangentBug {
	private static final int BUFFERSIZE = 100;
	
	public static MapLocation dest;
	
	public static MapLocation[] open;
	public static MapLocation[] closed;
	public static int openSize;
	public static int closedSize;
	
	public static boolean done;
	public static boolean tracing;
	public static int[][] pathInfo;
	
	public static void setDest(MapLocation newdest) {
		if (dest == null || newdest.x != dest.x || newdest.y != dest.y) {
			dest = newdest;
			pathInfo = new int[GameConstants.MAP_MAX_WIDTH][GameConstants.MAP_MAX_HEIGHT];
			done = false;
			tracing = false;
			
			open = new MapLocation[BUFFERSIZE];
			closed = new MapLocation[BUFFERSIZE];
			openSize = 1;
			closedSize = 0;
			
			open[0] = Handler.rc.getLocation();
			
//			cur = rc.getLocation();
//			localNext = cur;
//			
//			if (dest.distanceSquaredTo(Navigation.enemyHQ) <= 25) {
//				nearHQ = true;
//			}
		}
	}
	
	public static void calculate(int bytecodeLimit) {
		if (done) return;
		while (Clock.getBytecodeNum() < bytecodeLimit) {
			if (openSize == 0 && closedSize == 0) { // done path finding
				done = true;
				return;
			}
			if (openSize == 0) { // Move all closed to open
				for (int i = closedSize; --i >= 0;) {
					open[openSize] = closed[i];
					openSize++;
				}
				closedSize = 0;
			}
			openSize--;
			MapLocation cur = open[openSize];
			Direction forwardDir = cur.directionTo(dest);
			MapLocation forwardPos = cur.add(forwardDir);
			if (checkTile(cur, forwardPos)) { // Can move forward
				open[openSize] = forwardPos;
				openSize++;
			} else { // Must start tracing
				
			}
		}
	}
	
	public static Direction getNextMove() {
		return null;
	}
	
	public static boolean checkTile(MapLocation cur, MapLocation next) {
		TerrainTile forwardTile = Handler.rc.senseTerrainTile(next);
		//if (forwardTile == )
		return true;
	}
	
}
