package launcherDroneShield;

import battlecode.common.*;

public class Spawner {
	public static final Direction[] NSWE = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
	public static final Direction[] WENS = {Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH};
	public static final Direction[] DIAGS = {Direction.NORTH_WEST, Direction.NORTH_EAST, Direction.SOUTH_WEST, Direction.SOUTH_EAST};
	
	public static int HQxMod;
	public static int HQyMod;

	// Spawns a unit in a direction, does not check if can spawn
	// Wrapper for updating spent ore and unit count
	public static void spawn(Direction dir, RobotType typ, int countBlock) throws GameActionException {
		Handler.rc.spawn(dir, typ);
		Handler.rc.broadcast(Comm.SPENT_ORE_BUFFER_CHAN, Handler.rc.readBroadcast(Comm.SPENT_ORE_BUFFER_CHAN) + typ.oreCost);
		Count.incrementBoth(countBlock);
	}
	
	public static void build(Direction dir, RobotType typ, int countBlock) throws GameActionException {
		Handler.rc.build(dir, typ);
		Handler.rc.broadcast(Comm.SPENT_ORE_BUFFER_CHAN, Handler.rc.readBroadcast(Comm.SPENT_ORE_BUFFER_CHAN) + typ.oreCost);
		Count.incrementBoth(countBlock);
	}
	
	public static void trySpawn(Direction dir, RobotType typ, int countBlock) throws GameActionException {
		Direction[] dirs = MapUtils.dirsAroundRev(dir);
		for (int i = dirs.length; --i >= 0;) {
			if (Handler.rc.canSpawn(dirs[i], typ)) {
				spawn(dirs[i], typ, countBlock);
				return;
			}
		}
	}
	
	// Checks build array in reverse order
	public static Direction checkBuildArray(Direction[] dirs, RobotType typ) {
		for (int i = dirs.length; --i >= 0;) {
			if (Handler.rc.canBuild(dirs[i], typ)) return dirs[i];
		}
		return Direction.NONE;
	}
	
	// Tries to build on the HQ grid
	// Uses a hacky method of trying the cheapest tower: supply depots
	public static Direction getBuildDirection(boolean tryNonGrid) {
		if (Handler.myLoc.x % 2 == HQxMod) { // Along same x grid
			if (Handler.myLoc.y % 2 == HQyMod) { // Along the same y grid
				Direction dir = checkBuildArray(DIAGS, RobotType.SUPPLYDEPOT);
				if (dir == Direction.NONE && tryNonGrid) {
					dir = checkBuildArray(NSWE, RobotType.SUPPLYDEPOT);
				}
				return dir;
			} else {
				Direction dir = checkBuildArray(WENS, RobotType.SUPPLYDEPOT);
				if (dir == Direction.NONE && tryNonGrid) {
					dir = checkBuildArray(DIAGS, RobotType.SUPPLYDEPOT);
				}
				return dir;
			}
		} else if (Handler.myLoc.y % 2 == HQyMod) { // Along same y grid
			Direction dir = checkBuildArray(NSWE, RobotType.SUPPLYDEPOT);
			if (dir == Direction.NONE && tryNonGrid) {
				dir = checkBuildArray(DIAGS, RobotType.SUPPLYDEPOT);
			}
			return dir;
		} else {
			Direction dir = checkBuildArray(DIAGS, RobotType.SUPPLYDEPOT);
			if (dir == Direction.NONE && tryNonGrid) {
				dir = checkBuildArray(NSWE, RobotType.SUPPLYDEPOT);
			}
			return dir;
		}
	}
	
}
