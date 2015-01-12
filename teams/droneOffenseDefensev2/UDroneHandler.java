package droneOffenseDefensev2;

import battlecode.common.*;

public class UDroneHandler extends UnitHandler {
	private static RobotInfo[] inRangeEnemies;
	private static Direction heading;
	
	// Subjobs
	private enum DroneJob {
		NONE, ATTACK, DEFEND;
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
	}

	protected static void execute() throws GameActionException {
		executeUnit();
		assignJob();
		if (rc.isWeaponReady()) {
			inRangeEnemies = rc.senseNearbyRobots(typ.attackRadiusSquared,
					otherTeam);
			tryAttack();
		}
		if (rc.isCoreReady()) {
			RobotInfo[] enemies = rc.senseNearbyRobots(typ.attackRadiusSquared / 2, otherTeam);
			if (enemies.length > 0) {
				MapLocation goalLoc = myLoc;
				for (RobotInfo enemy : enemies) {
					goalLoc.add(enemy.location.directionTo(myLoc));
				}
				heading = myLoc.directionTo(goalLoc);
			} else {
				if (myJob == DroneJob.DEFEND) {
					executeDefense();
				} else {
					executeOffense();
				}
			}
			NavSimple.walkTowards(heading);
		}
	}
	
	protected static void tryAttack() throws GameActionException {
		if (inRangeEnemies.length > 0) {
			MapLocation minLoc = inRangeEnemies[0].location;
			int minRange = myLoc.distanceSquaredTo(minLoc);
			for (int i = inRangeEnemies.length - 1; i > 0; i--) { // Get minimum
																	// in array
				RobotInfo enemy = inRangeEnemies[i];
				MapLocation enemyLoc = enemy.location;
				int enemyRange = myLoc.distanceSquaredTo(enemyLoc);
				if (enemyRange < minRange) {
					minRange = enemyRange;
					minLoc = enemyLoc;
				}
			}

			rc.attackLocation(minLoc);
		}
	}
	
	protected static void assignJob() throws GameActionException {
		if (Clock.getRoundNum() >= 1800) {
			myJob = DroneJob.ATTACK;
		} else {
			myJob = DroneJob.DEFEND;
		}
	}
	
	protected static void executeDefense() throws GameActionException {
		
		for (MapLocation tower: rc.senseTowerLocations()) {
			if (myLoc.distanceSquaredTo(tower) < RobotType.TOWER.attackRadiusSquared) {
				heading = myLoc.directionTo(enemyHQ);
				return;
			}
		}
		
		if (myLoc.distanceSquaredTo(myHQ) < RobotType.HQ.attackRadiusSquared) {
			heading = myLoc.directionTo(enemyHQ);
		} else {
			Direction approx = Direction.NONE;
			int closestDist = Integer.MAX_VALUE;
			for (Direction dir: MapUtils.dirs) {
				if (dir == heading.opposite())
					continue;
				MapLocation proj = myLoc.add(dir);
				if (rc.senseTerrainTile(proj) == TerrainTile.OFF_MAP) { 
					continue;
				}
				int dist = proj.distanceSquaredTo(myHQ);
				boolean remainsOutside = dist > RobotType.HQ.attackRadiusSquared;
				for (MapLocation tower: rc.senseTowerLocations()) {
					if (remainsOutside) {
						remainsOutside = proj.distanceSquaredTo(tower) > RobotType.TOWER.attackRadiusSquared;
					}
				}
				if (remainsOutside && dist < closestDist) {
					approx = dir;
					closestDist = dist;
				}
			}
			if (approx == Direction.NONE) 
				heading = heading.opposite();
			else {
				heading = approx;
			}
			rc.setIndicatorString(0, heading.toString());
		}
	}
	
	protected static void executeOffense() throws GameActionException {
		
		MapLocation targetPt = new MapLocation(
				Comm.readBlock(Comm.getDroneId(), SHQHandler.NEXT_TARGET_X),
				Comm.readBlock(Comm.getDroneId(), SHQHandler.NEXT_TARGET_Y));
		heading = myLoc.directionTo(targetPt);
	}
}
