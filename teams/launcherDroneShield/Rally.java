package launcherDroneShield;

import battlecode.common.*;

public class Rally {
	
	private static final int SHOULD_ATTACK = 0;
	private static final int NEXT_TARGET_X = 1;
	private static final int NEXT_TARGET_Y = 2;
	private static final int SHOULD_GUARD = 10;
	private static final int NEXT_GUARD_X = 11;
	private static final int NEXT_GUARD_Y = 12;
	
	public static MapLocation getGuardPt() throws GameActionException {
		if (Comm.readBlock(getRallyId(), SHOULD_GUARD) == 1) {
			return new MapLocation(Comm.readBlock(
					getRallyId(), NEXT_GUARD_X), Comm.readBlock(
					getRallyId(), NEXT_GUARD_Y));
		} else {
			return null;
		}
	}
	
	public static MapLocation getTargetPt() throws GameActionException {
		if (Comm.readBlock(getRallyId(), SHOULD_ATTACK) == 1) {
			return new MapLocation(Comm.readBlock(
					getRallyId(), NEXT_TARGET_X), Comm.readBlock(
							getRallyId(), NEXT_TARGET_Y));
		} else {
			return null;
		}
	}
	
	public static void forceSetGuardPt(MapLocation loc) throws GameActionException {
		Comm.writeBlock(getRallyId(), SHOULD_GUARD, 1);
		Comm.writeBlock(getRallyId(), NEXT_GUARD_X, loc.x);
		Comm.writeBlock(getRallyId(), NEXT_GUARD_Y, loc.y);
	}
	
	public static void forceSetTargetPt(MapLocation loc) throws GameActionException {
		Comm.writeBlock(getRallyId(), SHOULD_ATTACK, 1);
		Comm.writeBlock(getRallyId(), NEXT_TARGET_X, loc.x);
		Comm.writeBlock(getRallyId(), NEXT_TARGET_Y, loc.y);
	}
	
	public static void setRallyPoints() throws GameActionException {
		
		// Determining the rally position for the defensive swarm
		// Position is determined as the weakest tower
		MapLocation nextGuardSite = null;
		int maxNumEnemies = 0;
		MapLocation[] towerLocs = Handler.rc.senseTowerLocations();
		
		int numNearbyEnemies = Handler.rc.senseNearbyRobots(RobotType.TOWER.sensorRadiusSquared, Handler.otherTeam).length;
		if (numNearbyEnemies > 0) {
			nextGuardSite = Handler.myLoc;
			maxNumEnemies = numNearbyEnemies;
		}
		
		for (MapLocation tower: towerLocs) {
			numNearbyEnemies = Handler.rc.senseNearbyRobots(tower, RobotType.TOWER.sensorRadiusSquared, Handler.otherTeam).length;
			if (numNearbyEnemies > maxNumEnemies) {
				nextGuardSite = tower;
				maxNumEnemies = numNearbyEnemies;
			}
		}
		
		if (nextGuardSite != null) {
			forceSetGuardPt(nextGuardSite);
		} else {
			Comm.writeBlock(getRallyId(), SHOULD_GUARD, 0);
		}
		
//		if (Clock.getRoundNum() <= 1500) {
//			Comm.writeBlock(getRallyId(), SHOULD_ATTACK, 0);
//			return;
//		}
//				
//		// Determining the rally and target positions for the offensive swarm
//		// Positions are determined as the closest pair of team/enemy towers
//		MapLocation nextTargetSite = Handler.enemyHQ;
//		
//		int minDistanceFromEnemy = Integer.MAX_VALUE;
//
//		MapLocation[] enemyTowers = Handler.rc.senseEnemyTowerLocations();
//		for (int j = 0; j < enemyTowers.length; j++) {
//			int distanceFromEnemy = Handler.myHQ
//					.distanceSquaredTo(enemyTowers[j]);
//			if (distanceFromEnemy < minDistanceFromEnemy) {
//				nextTargetSite = enemyTowers[j];
//				minDistanceFromEnemy = distanceFromEnemy;
//			}
//		}
//		for (int i = 0; i < towerLocs.length; i++) {
//			for (int j = 0; j < enemyTowers.length; j++) {
//				int distanceFromEnemy = towerLocs[i]
//						.distanceSquaredTo(enemyTowers[j]);
//				if (distanceFromEnemy < minDistanceFromEnemy) {
//					nextTargetSite = enemyTowers[j];
//					minDistanceFromEnemy = distanceFromEnemy;
//				}
//			}
//			if (enemyTowers.length == 0) {
//				int distanceFromEnemy = towerLocs[i]
//						.distanceSquaredTo(Handler.enemyHQ);
//				if (distanceFromEnemy < minDistanceFromEnemy) {
//					nextTargetSite = Handler.enemyHQ;
//					minDistanceFromEnemy = distanceFromEnemy;
//				}
//			}
//		}
//		
//		forceSetTargetPt(nextTargetSite);
	}

	/*-------------------------------- COMM FUNCTIONS --------------------------------*/
	
	public static final int RALLY_BLOCK = 199;
	public static int rallyBlockId = 0;

	/**
	 * Returns the block id of the dedicated scout block Creates a scout block
	 * if it was not previously
	 */
	public static int getRallyId() throws GameActionException {
		if (rallyBlockId == 0) {
			rallyBlockId = Handler.rc.readBroadcast(RALLY_BLOCK);
			if (rallyBlockId == 0) {
				rallyBlockId = Comm.requestBlock(true);
				Handler.rc.broadcast(RALLY_BLOCK, rallyBlockId);
			}
		}
		return rallyBlockId;
	}
}
