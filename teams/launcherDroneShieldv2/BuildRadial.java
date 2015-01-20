package launcherDroneShieldv2;

import battlecode.common.*;

public class BuildRadial {
	
	private static final int EXPAND_THRESHOLD = 30;
	
	private static int timeSinceBuild;
	private static Direction buildDir;
	private static int buildRadius;
	private static boolean clockwise;
	private static Direction heading;
	
	public static void init() throws GameActionException {
		timeSinceBuild = 0;
		clockwise = false;
		buildRadius = 1;
		heading = Handler.myLoc.directionTo(Handler.myHQ);
		
		int len = MapUtils.dirs.length;
		for (int i = 0; i < len; i++) {
			if (MapUtils.dirs[i] == heading) {
				if (i % 2 == 0) { // Cardinal direction
					heading = MapUtils.dirs[(i + 2) % len];
					break;
				} else { // Diagonal direction
					heading = MapUtils.dirs[(i + 1) % len];
					break;
				}
			}
		}
	}
	
	public static void build(RobotType robot) throws GameActionException {
		
		if (!Handler.rc.isBuildingSomething() && Handler.rc.getTeamOre() > robot.oreCost) {
			timeSinceBuild++;
		}
		if (timeSinceBuild > EXPAND_THRESHOLD) {
			timeSinceBuild = 0;
			buildRadius += 2;
		}
		
		Handler.rc.setIndicatorString(0, "build radius: " + buildRadius);
		
		buildDir = Direction.NONE;
		if (!inBuildRadius(Handler.myLoc)) { // Move to build radius
			walkToBuildRadius();
		} else {
			walkAlongBuildRadius(); 
			if (!canBuildWithGap() || Handler.rc.getTeamOre() < robot.oreCost) { // Move along build radius until we find a good site
				NavSimple.walkTowards(heading);
			} else if (Handler.rc.canBuild(buildDir, robot)){
				Spawner.build(buildDir, robot, StructureHandler.getRobotId(robot));
				timeSinceBuild = 0; // Reset the age so we can put off expanding
			}	
		}
	}
	
	private static boolean inBuildRadius(MapLocation loc) throws GameActionException {
		return loc.distanceSquaredTo(Handler.myHQ) > (buildRadius - 1) * (buildRadius - 1) * 2 &&
				loc.distanceSquaredTo(Handler.myHQ) < buildRadius * buildRadius * 2 + 1;
	}
	
	private static void walkToBuildRadius() throws GameActionException {
		if (Handler.myLoc.distanceSquaredTo(Handler.myHQ) <= (buildRadius - 1) * (buildRadius - 1) * 2) { // Inside radius
			NavSimple.walkTowards(Handler.myHQ.directionTo(Handler.myLoc));
		} else if (Handler.myLoc.distanceSquaredTo(Handler.myHQ) >= (buildRadius + 1) * (buildRadius + 1) * 2) { // Outside radius
			NavSimple.walkTowards(Handler.myLoc.directionTo(Handler.myHQ));
		}
	}
	
	private static void walkAlongBuildRadius() throws GameActionException {
		MapLocation next = Handler.myLoc.add(heading);
		if (!inBuildRadius(next)) {
			int len = MapUtils.dirsCardinal.length;
			if (clockwise) {
				for (int i = 0; i < len; i++) {
					if (MapUtils.dirsCardinal[i] == heading) {
						heading = MapUtils.dirsCardinal[(i + 1) % len];
						break;
					}
				}
			} else {
				for (int i = 0; i < len; i++) {
					if (MapUtils.dirsCardinal[i] == heading) {
						heading = MapUtils.dirsCardinal[((i - 1) % len + len) % len];
						break;
					}
				}
			}
			next = Handler.myLoc.add(heading);
			if (!Handler.rc.canMove(heading)) {
				clockwise = !clockwise;
				heading = Direction.NONE;
			}
		}
	}
	
	private static boolean canBuildWithGap() throws GameActionException {
		for (Direction d: MapUtils.dirs) {
			if (d == heading) {
				continue;
			}
			
			MapLocation site = Handler.myLoc.add(d);
			
			if (inBuildRadius(site)) {
				continue;
			}
			
			boolean canBuild = Handler.rc.senseRobotAtLocation(site) == null;
			for (Direction d2: MapUtils.dirs) {
				RobotInfo info = Handler.rc.senseRobotAtLocation(site.add(d2));
				if (info != null && info.type.isBuilding) {
					canBuild = false;
				}
			}
			
			if (canBuild) {
				buildDir = d;
				return true;
			}
		}
		return false;
	}
}
