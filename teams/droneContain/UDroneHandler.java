package droneContain;

import battlecode.common.*;

public class UDroneHandler extends UnitHandler {

	private static Direction heading;
	private static boolean orbitClockwise;
	private static MapLocation circling;

	// Subjobs
	private enum DroneJob {
		NONE, ATTACK, ORBIT, COURIER;
	}

	private static DroneJob myJob;

	public static void loop(RobotController rcon) {
		try {
			init(rcon);
		} catch (Exception e) {
			// e.printStackTr1ace();
			System.out.println(typ + " Initialization Exception");
		}

		while (true) {
			try {
				execute();
			} catch (Exception e) {
				// e.printStackTrace();
				System.out.println(typ + " Execution Exception");
			}
			rc.yield(); // Yields to save remaining bytecodes
		}
	}

	protected static void init(RobotController rcon) throws GameActionException {
		initUnit(rcon);
		myJob = DroneJob.NONE;
		heading = Direction.NONE;
		if (rand.nextInt(2) == 1)
			orbitClockwise = true;
		else 
			orbitClockwise = false;
	}

	protected static void execute() throws GameActionException {
		executeUnit();
		assignJob();
		Supply.spreadSupplies(Supply.DEFAULT_THRESHOLD);
		if (rc.isCoreReady()) {
//			if (!evade()) {
				switch (myJob) {
				case ORBIT:
					executeOrbit();
					break;
				case ATTACK:
					executeOffense();
					break;
				default:
					break;
				}
				NavSimple.walkTowards(heading);
//			}
		}
	}

	protected static void assignJob() throws GameActionException {
		if (Clock.getRoundNum() >= 1800) {
			myJob = DroneJob.ATTACK;
		} else {
			myJob = DroneJob.ORBIT;
		}
	}

	protected static boolean evade() throws GameActionException {

		RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(rc.getLocation(),
				rc.getType().sensorRadiusSquared, rc.getTeam().opponent());
		boolean shouldRun = false;
		MapLocation target = null;
		double lowestHP = Double.POSITIVE_INFINITY;
		boolean[] safe = new boolean[MapUtils.dirs.length];
		for (int i = 0; i < safe.length; i++) {
			safe[i] = true;
		}

		for (RobotInfo enemy : nearbyEnemies) {
			int distToEnemy = rc.getLocation()
					.distanceSquaredTo(enemy.location);
			boolean inEnemyRange = distToEnemy <= enemy.type.attackRadiusSquared;
			boolean enemyInRange = rc.canAttackLocation(enemy.location);
			if (inEnemyRange && enemy.type != RobotType.TOWER
					&& enemy.type != RobotType.HQ) {// run away!
				shouldRun = true;
				for (int i = 0; i < MapUtils.dirs.length; i++) {
					if (rc.canMove(MapUtils.dirs[i])) {
						int testDist = myLoc.add(MapUtils.dirs[i])
								.distanceSquaredTo(enemy.location);
						if (testDist <= enemy.type.attackRadiusSquared) {
							safe[i] = false;
						}
					} else {
						safe[i] = false;
					}
				}
			}
			if (enemyInRange) {// this robot can be attacked
				if (target == null || enemy.health < lowestHP) {
					target = enemy.location;
					lowestHP = enemy.health;
				}
			}
		}

		if (target != null && rc.canAttackLocation(target)
				&& rc.isWeaponReady()) {
			rc.attackLocation(target);
		}
		if (shouldRun) {
			for (int i = 0; i < safe.length; i++) {
				if (safe[i]) {
					NavSimple.walkTowards(MapUtils.dirs[i]);
					return true;
				}
			}
		}
		return false;
	}

	protected static void executeOrbit() throws GameActionException {
		
		heading = myLoc.directionTo(enemyHQ);
		
		if (avoidsTowersAndHQ(myLoc.add(heading))) {
			return;
		} else {
			updateCircleCenter(myLoc.add(heading));
			heading = perpendicularTo(myLoc.directionTo(circling));
			if (rc.senseTerrainTile(myLoc.add(heading)) == TerrainTile.OFF_MAP) {
				orbitClockwise = !orbitClockwise;
				//executeOrbit();
			}
		}
	}

	protected static boolean avoidsTowersAndHQ(MapLocation loc) {
		int dist = loc.distanceSquaredTo(enemyHQ);
		boolean avoids = dist > SHQHandler.splashRange;
		for (MapLocation tower : rc.senseEnemyTowerLocations()) {
			avoids = avoids
					&& loc.distanceSquaredTo(tower) > RobotType.TOWER.attackRadiusSquared;
		}
		return avoids;
	}
	
	protected static void updateCircleCenter(MapLocation loc) {
		
		int dist = loc.distanceSquaredTo(enemyHQ);
		if (dist <= SHQHandler.splashRange && (circling == null || !circling.equals(enemyHQ))) {
			circling = enemyHQ;
			return;
		}
		
		for (MapLocation tower : rc.senseEnemyTowerLocations()) {
			dist = loc.distanceSquaredTo(tower);
			if (dist <= RobotType.TOWER.attackRadiusSquared && (circling == null || !circling.equals(tower))) {
				circling = tower;
				return;
			}
		}
		
	}

	protected static void executeOffense() throws GameActionException {

		MapLocation targetPt = new MapLocation(Comm.readBlock(
				Comm.getDroneId(), SHQHandler.NEXT_TARGET_X), Comm.readBlock(
				Comm.getDroneId(), SHQHandler.NEXT_TARGET_Y));

		Direction approx = Direction.NONE;
		int closestDist = Integer.MAX_VALUE;
		for (Direction dir : MapUtils.dirs) {
			if (dir == heading.opposite() || !rc.canMove(dir))
				continue;
			MapLocation proj = myLoc.add(dir);
			if (rc.senseTerrainTile(proj) == TerrainTile.OFF_MAP) {
				continue;
			}
			int dist = proj.distanceSquaredTo(targetPt);
			if (dist < closestDist) {
				approx = dir;
				closestDist = dist;
			}
		}
		if (approx == Direction.NONE)
			heading = heading.opposite();
		else {
			heading = approx;
		}
	}

	protected static Direction perpendicularTo(Direction d) {
		int len = MapUtils.dirs.length;
		if (!orbitClockwise) {
			for (int i = 0; i < len; i++) {
				if (MapUtils.dirs[i] == d) {
					return MapUtils.dirs[(i + 2) % len];
				}
			}
			return Direction.NONE;
		} else {
			for (int i = 0; i < len; i++) {
				if (MapUtils.dirs[i] == d) {
					return MapUtils.dirs[((i - 2) % len + len) % len];
				}
			}
			return Direction.NONE;
		}
	}
}
