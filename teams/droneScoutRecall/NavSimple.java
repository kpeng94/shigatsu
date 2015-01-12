package droneScoutRecall;

import battlecode.common.*;

public class NavSimple {
	
	public static void walkTowards(Direction dir) throws GameActionException {
		walkPriority(MapUtils.dirsTowards(dir));
	}
	
	public static void walkRandom() throws GameActionException {
		walkPriority(MapUtils.dirsTowards(MapUtils.dirs[Handler.rand.nextAnd(7)]));
	}
	
	public static void walkPriority(Direction[] dirs) throws GameActionException {
		for (int i = 0; i < dirs.length; i++) {
			if (Handler.rc.canMove(dirs[i])) {
				Handler.rc.move(dirs[i]);
				return;
			}
		}
	}
	
}
