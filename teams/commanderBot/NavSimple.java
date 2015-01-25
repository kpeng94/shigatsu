package commanderBot;

import battlecode.common.*;

public class NavSimple {
	
	public static void walkTowards(Direction dir) throws GameActionException {
		walkPriority(MapUtils.dirsAround(dir));
	}
	
	public static void walkTowardsDirected(Direction dir) throws GameActionException {
		walkPriority(MapUtils.dirsTowards(dir));
	}
	
	public static void walkTowardsSafe(Direction dir) throws GameActionException {
		Direction[] dirs = MapUtils.dirsSemiCircleRev(dir);
		for (int i = dirs.length; --i >= 0;) {
			Direction d = dirs[i];
			if (Handler.rc.canMove(d) && NavSafeBug.safeTile(Handler.myLoc.add(d))) {
				Handler.rc.move(d);
				return;
			}
		}
	}
	
	public static void walkRandom() throws GameActionException {
		walkPriority(MapUtils.dirsAround(MapUtils.dirs[Handler.rand.nextAnd(7)]));
	}
	
	public static void walkPriority(Direction[] dirs) throws GameActionException {
		for (int i = 0; i < dirs.length; i++) {
			if (Handler.rc.canMove(dirs[i])) {
				Handler.rc.move(dirs[i]);
				return;
			}
		}
	}
	
	public static Direction dirTowards(Direction dir) {
		return dirPriority(MapUtils.dirsAround(dir));
	}
	
	public static Direction dirTowardsDirection(Direction dir) {
		return dirPriority(MapUtils.dirsTowards(dir));
	}
	
	public static Direction dirTowardsSafe(Direction dir) {
		Direction[] dirs = MapUtils.dirsSemiCircleRev(dir);
		for (int i = dirs.length; --i >= 0;) {
			Direction d = dirs[i];
			if (Handler.rc.canMove(d) && NavSafeBug.safeTile(Handler.myLoc.add(d))) {
				return d;
			}
		}
		return Direction.NONE;
	}
	
	public static Direction dirRandom() {
		return dirPriority(MapUtils.dirsAround(MapUtils.dirs[Handler.rand.nextAnd(7)]));
	}
	
	public static Direction dirPriority(Direction[] dirs) {
		for (int i = 0; i < dirs.length; i++) {
			if (Handler.rc.canMove(dirs[i])) {
				return dirs[i];
			}
		}
		return Direction.NONE;
	}
	
}
