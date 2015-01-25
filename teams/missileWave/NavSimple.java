package missileWave;

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
	
	public static boolean canMove(Direction dir, boolean avoidHQ, boolean avoidTower, boolean avoidUnits) {
		if (avoidUnits) {
			if (!Handler.rc.canMove(dir)) return false;
		} else {
			if (Handler.rc.senseTerrainTile(Handler.myLoc.add(dir)) != TerrainTile.NORMAL) return false;
		}
		if (avoidHQ) {
			int distToHQ = Handler.enemyHQ.distanceSquaredTo(Handler.myLoc.add(dir));
			if (Handler.enemyTowers.length < 5) {
				if (distToHQ <= RobotType.HQ.attackRadiusSquared) return false;
			} else {
				if (distToHQ <= SHQHandler.SPLASH_RANGE && distToHQ != 49) return false;
			}
		}
		if (avoidTower) {
			for (int i = Handler.enemyTowers.length; --i >= 0;) {
				if (Handler.enemyTowers[i].distanceSquaredTo(Handler.myLoc.add(dir)) <= RobotType.TOWER.attackRadiusSquared) return false;
			}
		}
		return true;
	}
	
}
