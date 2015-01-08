package tangentBugBot;

import battlecode.common.*;

public class NavDumbTangent {
	public static MapLocation dest;
	public static boolean tracing = false;
	public static boolean isLeft = false;
	public static Direction prevMove = null;
	public static int bugStartDist = 0;
	
	public static void dumbTangentReset(MapLocation newdest) {
		if (dest == null || newdest.x != dest.x || newdest.y != dest.y) {
			dest = newdest;
			tracing = false;
			prevMove = null;
		}
	}
	
	public static Direction dumbTangentDir(boolean avoidTower) {
		if (dest == null) return Direction.NONE;
		Direction forwardDir = Handler.myLoc.directionTo(dest);
		if (tracing) {
			int curDist = Handler.myLoc.distanceSquaredTo(dest);
			if (isLeft) {
				MapLocation wall = Handler.myLoc.add(prevMove.rotateRight().rotateRight());
				if (curDist < bugStartDist && curDist < wall.distanceSquaredTo(dest)) {
					tracing = false;
				} else {
					Direction testDir = prevMove.rotateRight().rotateRight();
					for (int i = 8; --i >= 0;) {
						if (NavSimple.canMove(testDir, true, avoidTower, false)) {
							return testDir;
						}
						testDir = testDir.rotateLeft();
					}
				}
			} else {
				MapLocation wall = Handler.myLoc.add(prevMove.rotateLeft().rotateLeft());
				if (curDist < bugStartDist && curDist < wall.distanceSquaredTo(dest)) {
					tracing = false;
				} else {
					Direction testDir = prevMove.rotateLeft().rotateLeft();
					for (int i = 8; --i >= 0;) {
						if (NavSimple.canMove(testDir, true, avoidTower, false)) {
							return testDir;
						}
						testDir = testDir.rotateRight();
					}
				}
			}
		}
		if (!tracing) {
			if (NavSimple.canMove(forwardDir, true, avoidTower, false)) {
				return forwardDir;
			}
			Direction testDir = forwardDir.rotateLeft();
			for (int i = 3; --i >= 0;) {
				if (NavSimple.canMove(testDir, true, avoidTower, false)) {
					tracing = true;
					isLeft = true;
					bugStartDist = Handler.myLoc.add(testDir).distanceSquaredTo(dest);
					return testDir;
				}
				testDir = testDir.rotateLeft();
			}
			testDir = forwardDir.rotateRight();
			for (int i = 4; --i >= 0;) {
				if (NavSimple.canMove(testDir, true, avoidTower, false)) {
					tracing = true;
					isLeft = false;
					bugStartDist = Handler.myLoc.add(testDir).distanceSquaredTo(dest);
					return testDir;
				}
				testDir = testDir.rotateRight();
			}
		}
		return Direction.NONE;
	}
	
	// call this pls
	public static void executeDumbTangent(Direction dir) throws GameActionException {
		if (Handler.rc.canMove(dir)) {
			prevMove = dir;
			Handler.rc.move(dir);
		}
	}

}
