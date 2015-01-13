package kevintestrashbot;

import battlecode.common.*;

public class Spawner {
	public static final Direction[] NS = {Direction.NORTH, Direction.SOUTH};
	public static final Direction[] NOT_NS = {Direction.NORTH_WEST, Direction.NORTH_EAST, Direction.WEST, Direction.EAST, Direction.SOUTH_WEST, Direction.SOUTH_EAST};
	public static final Direction[] WE = {Direction.WEST, Direction.EAST};
	public static final Direction[] NOT_WE = {Direction.NORTH_WEST, Direction.NORTH_EAST, Direction.NORTH, Direction.SOUTH, Direction.SOUTH_WEST, Direction.SOUTH_EAST};
	public static final Direction[] DIAGS = {Direction.NORTH_WEST, Direction.NORTH_EAST, Direction.SOUTH_WEST, Direction.SOUTH_EAST};
	public static final Direction[] ADJS = {Direction.NORTH, Direction.WEST, Direction.EAST, Direction.SOUTH};
	
	public static int HQxMod;
	public static int HQyMod;

	public static void trySpawn(Direction dir, RobotType typ, double oreAmount) throws GameActionException {
		Direction[] dirs = MapUtils.dirsAroundRev(dir);
		for (int i = dirs.length; --i >= 0;) {
			if (Handler.rc.canSpawn(dirs[i], typ) && oreAmount >= typ.oreCost) {
				Handler.rc.spawn(dirs[i], typ);
				return;
			}
		}
	}
	
	public static Direction checkBuildArray(Direction[] dirs, RobotType typ) {
		for (int i = dirs.length; --i >= 0;) {
			if (Handler.rc.canBuild(dirs[i], typ)) return dirs[i];
		}
		return Direction.NONE;
	}
	
	// Tries to build on the HQ grid
	public static Direction getBuildDirection(RobotType typ, boolean tryNonGrid) {
		if (Handler.myLoc.x % 2 == HQxMod) { // Along same x grid
			if (Handler.myLoc.y % 2 != HQyMod) {
				Direction dir = checkBuildArray(NS, typ);
				if (dir == Direction.NONE && tryNonGrid) {
					dir = checkBuildArray(NOT_NS, typ);
				}
				return dir;
			}
		} else if (Handler.myLoc.y % 2 == HQyMod) { // Along same 
			Direction dir = checkBuildArray(WE, typ);
			if (dir == Direction.NONE && tryNonGrid) {
				dir = checkBuildArray(NOT_WE, typ);
			}
			return dir;
		} else {
			Direction dir = checkBuildArray(DIAGS, typ);
			if (dir == Direction.NONE && tryNonGrid) {
				dir = checkBuildArray(ADJS, typ);
			}
			return dir;
		}
		return Direction.NONE;
	}
	
}
