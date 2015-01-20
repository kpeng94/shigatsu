package launcherDroneShieldv2;

import battlecode.common.*;

public class NavSafeBug {
	private static enum traceDir {
		LEFT,
		RIGHT,
		NONE
	}
	
	public static traceDir tracing = traceDir.NONE;
	public static Direction prevDir;
	public static int startingDist = 0;
	public static Direction startingDir;
	public static MapLocation startingDest;
	public static traceDir prevTrace = traceDir.NONE;
	
	public static Direction dirToBugIn(MapLocation dest) {
		if (Handler.myLoc.equals(dest)) return Direction.NONE;
		Direction forwardDir = Handler.myLoc.directionTo(dest);
		MapLocation forwardLoc = Handler.myLoc.add(forwardDir);

		if (tracing == traceDir.NONE || (tracing != traceDir.NONE && !dest.equals(startingDest))) {
			if (Handler.rc.canMove(forwardDir) && safeTile(forwardLoc)) { // can move forward
				return forwardDir;
			} else { // start tracing
				if (tracing == traceDir.NONE) {
					if (prevTrace == traceDir.NONE) {
						tracing = traceDir.LEFT;
					} else {
						tracing = prevTrace;
					}
					prevTrace = tracing;
				}
				startingDist = Handler.myLoc.distanceSquaredTo(dest);
				startingDir = forwardDir;
				startingDest = dest;
				Direction testDir = forwardDir;
				for (int i = 7; --i >= 0;) {
					if (tracing == traceDir.LEFT) {
						testDir = testDir.rotateLeft();
					} else if (tracing == traceDir.RIGHT) {
						testDir = testDir.rotateRight();
					}
					MapLocation testLoc = Handler.myLoc.add(testDir);
					if (Handler.rc.canMove(testDir) && safeTile(testLoc)) {
						prevDir = testDir;
						return testDir;
					}
				}
			}
		}
		
		else { // Already tracing
			if (forwardDir.equals(startingDir) && Handler.myLoc.distanceSquaredTo(dest) < startingDist &&
					Handler.rc.canMove(forwardDir) && safeTile(forwardLoc)) { // stop tracing
				tracing = traceDir.NONE;
				return forwardDir;
			} else { // Calculate next tracing
				if (tracing == traceDir.LEFT) {
					Direction dir = tryTraceLeft();
					if (dir == Direction.NONE && tracing == traceDir.RIGHT) {
						prevDir = prevDir.opposite();
						dir = tryTraceRight();
					}
					if (dir != Direction.NONE) {
						prevDir = dir;
					}
					return dir;
				} else if (tracing == traceDir.RIGHT) {
					Direction dir = tryTraceRight();
					if (dir == Direction.NONE && tracing == traceDir.LEFT) {
						prevDir = prevDir.opposite();
						dir = tryTraceLeft();
					}
					if (dir != Direction.NONE) {
						prevDir = dir;
					}
					return dir;
				}
			}
		}
		RobotInfo[] nearbyEnemies = Handler.rc.senseNearbyRobots(24, Handler.otherTeam);
		if (nearbyEnemies.length > 0) {
			Direction away = nearbyEnemies[0].location.directionTo(Handler.myLoc);
			Direction[] dirs = MapUtils.dirsAround(away);
			for (int i = dirs.length; --i >= 0;) {
				if (Handler.rc.canMove(dirs[i])) {
					return dirs[i];
				}
			}
		}
		prevDir = forwardDir;
		return Direction.NONE;
	}
	
	private static Direction tryTraceLeft() {
		Direction wallDir = prevDir.opposite().rotateLeft();
		MapLocation wallLoc = Handler.myLoc.add(wallDir);
		if (Handler.rc.canMove(wallDir) && safeTile(wallLoc)) {
			tracing = traceDir.NONE;
			return wallDir;
		}
		Direction testDir = wallDir;
		for (int i = 7; --i >= 0;) {
			testDir = testDir.rotateLeft();
			if (Handler.rc.senseTerrainTile(Handler.myLoc.add(testDir)) == TerrainTile.OFF_MAP) {
				tracing = traceDir.RIGHT;
				prevTrace = tracing;
				return Direction.NONE;
			}
			MapLocation testLoc = Handler.myLoc.add(testDir);
			if (Handler.rc.canMove(testDir) && safeTile(testLoc)) {
				return testDir;
			}
		}
		return Direction.NONE;
	}
	
	private static Direction tryTraceRight() {
		Direction wallDir = prevDir.opposite().rotateRight();
		MapLocation wallLoc = Handler.myLoc.add(wallDir);
		if (Handler.rc.canMove(wallDir) && safeTile(wallLoc)) {
			tracing = traceDir.NONE;
			return wallDir;
		}
		Direction testDir = wallDir;
		for (int i = 7; --i >= 0;) {
			testDir = testDir.rotateRight();
			if (Handler.rc.senseTerrainTile(Handler.myLoc.add(testDir)) == TerrainTile.OFF_MAP) {
				tracing = traceDir.LEFT;
				prevTrace = tracing;
				return Direction.NONE;
			}
			MapLocation testLoc = Handler.myLoc.add(testDir);
			if (Handler.rc.canMove(testDir) && safeTile(testLoc)) {
				return testDir;
			}
		}
		return Direction.NONE;
	}
	
	private static boolean safeTile(MapLocation loc) {
		if (Handler.enemyTowers.length >= 5) { // Enemy HQ has splash
			if (loc.add(loc.directionTo(Handler.enemyHQ)).distanceSquaredTo(Handler.enemyHQ) <= GameConstants.HQ_BUFFED_ATTACK_RADIUS_SQUARED) {
				return false;
			}
		} else if (Handler.enemyTowers.length >= 2) { // Enemy HQ has larger range
			if (loc.distanceSquaredTo(Handler.enemyHQ) <= GameConstants.HQ_BUFFED_ATTACK_RADIUS_SQUARED) {
				return false;
			}
		} else {
			if (loc.distanceSquaredTo(Handler.enemyHQ) <= RobotType.HQ.attackRadiusSquared) {
				return false;
			}
		}
		for (int i = Handler.enemyTowers.length; --i >= 0;) {
			if (loc.distanceSquaredTo(Handler.enemyTowers[i]) <= RobotType.TOWER.attackRadiusSquared) {
				return false;
			}
		}
		
		return true;
	}
	
}
