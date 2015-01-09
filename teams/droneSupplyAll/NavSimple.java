package droneSupplyAll;

import battlecode.common.*;

public class NavSimple {
	
	public static void walkTowards(Direction dir) throws GameActionException {
		walkPriority(MapUtils.directionsTowards(dir));
	}
	
	public static void walkRandom() throws GameActionException {
		walkPriority(MapUtils.directionsTowards(MapUtils.dirs[Handler.rand.nextAnd(7)]));
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
