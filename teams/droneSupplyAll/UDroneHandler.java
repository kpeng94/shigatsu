package droneSupplyAll;

import battlecode.common.*;

public class UDroneHandler extends UnitHandler {
	private static RobotInfo[] inRangeEnemies;

	// Subjobs
	private enum DroneJob {
		NONE, REPORT, ATTACK, COURIER, DEFEND;
	}

	private static DroneJob myJob;

	public static void loop(RobotController rcon) {
		try {
			init(rcon);
		} catch (Exception e) {
			// e.printStackTrace();
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

	protected static void init(RobotController rcon) {
		initUnit(rcon);
		myJob = DroneJob.NONE;
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
			walkDrone();
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

		if (myJob == DroneJob.NONE) {
			int numCouriers = rc.readBroadcast(NUM_COURIER_CHANNEL);
			if (numCouriers < rc.readBroadcast(WAVE_NUMBER_CHANNEL) / 2) {
				myJob = DroneJob.COURIER;
				rc.broadcast(NUM_COURIER_CHANNEL, numCouriers + 1);
			} else {
				if (rand.nextInt(100) < 75)
					myJob = DroneJob.REPORT;
				else
					myJob = DroneJob.DEFEND;
			}
		} else if (rc.readBroadcast(DRONE_PROMOTION_CHANNEL) == 1
				&& myJob == DroneJob.REPORT) {
			myJob = DroneJob.ATTACK;
		}
	}

	protected static void walkDrone() throws GameActionException {

		MapLocation goalLoc;
		
		MapLocation guardPt = new MapLocation(
				rc.readBroadcast(NEXT_GUARD_X_CHANNEL),
				rc.readBroadcast(NEXT_GUARD_Y_CHANNEL));
		MapLocation rallyPt = new MapLocation(
				rc.readBroadcast(NEXT_RALLY_X_CHANNEL),
				rc.readBroadcast(NEXT_RALLY_Y_CHANNEL));
		MapLocation targetPt = new MapLocation(
				rc.readBroadcast(NEXT_TARGET_X_CHANNEL),
				rc.readBroadcast(NEXT_TARGET_Y_CHANNEL));
		
		int myRange = typ.attackRadiusSquared;
		RobotInfo[] allies = rc.senseNearbyRobots(myRange / 2, myTeam);
		RobotInfo[] enemies = rc.senseNearbyRobots(myRange / 2, otherTeam);

		// Couriers retrieve supply from hq and drop it off at the rally point
		if (myJob == DroneJob.COURIER) {
			
			if (Clock.getRoundNum() % INTERWAVE_TIME == INTERWAVE_TIME - 1) { // Supply drones if about to attack
				
//				RobotInfo[] nearby = rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, myTeam);
//				int numNearbyDrones = 0;
//				for (RobotInfo info: nearby) { 
//					if (info.type == RobotType.DRONE && info.supplyLevel == 0 && numNearbyDrones < 15) {
//						numNearbyDrones++;
//					}
//				}
				
				int numNearbyDrones = Math.min(15, rc.readBroadcast(NUM_DRONE_CHANNEL));
				
				if (numNearbyDrones > 0) {
					int toDistribute = (int) rc.getSupplyLevel() / numNearbyDrones;

					for (int i = 0; i < numNearbyDrones; i++) {
						if (allies[i].type == RobotType.DRONE) {
							rc.transferSupplies(toDistribute,
									allies[i].location);
						}
					}
				}
			}
			if (Clock.getRoundNum() % INTERWAVE_TIME >= INTERWAVE_TIME - 50) { // Dispatch to prepare for supplying
				goalLoc = rallyPt;
			} else { // Retrieve supply
				goalLoc = myHQ;
			}
		} else if (myJob == DroneJob.DEFEND) { // Defenders try to stay close to the guardpoint and avoid enemy attacks
			if (enemies.length > 0) {
				goalLoc = myLoc;
				for (int i = 0; i < enemies.length; i++) {
					goalLoc.add(enemies[i].location.directionTo(myLoc));
				}
			} else {
				goalLoc = guardPt;
			}
		} else {
			MapLocation dest;
			if (myJob == DroneJob.REPORT) {
				dest = rallyPt;
			} else {
				dest = targetPt;
			}

			// Retreat - Run away from nearby enemies
			if (enemies.length > 0) {
				if (allies.length > enemies.length) {
					goalLoc = myLoc;
					for (int i = 0; i < enemies.length; i++) {
						goalLoc.add(enemies[i].location.directionTo(myLoc));
					}
				} else {
					goalLoc = myLoc.add(myLoc.directionTo(dest),
							targetWeight(myLoc.distanceSquaredTo(dest)));
					MapLocation closestAlly = myLoc;
					int minDistanceFromAlly = Integer.MAX_VALUE;
					for (int i = 0; i < allies.length; i++) {
						int distanceFromAlly = myLoc
								.distanceSquaredTo(allies[i].location);
						if (distanceFromAlly < minDistanceFromAlly) {
							closestAlly = allies[i].location;
							minDistanceFromAlly = distanceFromAlly;
						}
					}
					goalLoc = goalLoc.add(myLoc.directionTo(closestAlly), 5);
				}
			}
			// Swarm - Gather near rally point and repel away from allies
			else {
				goalLoc = dest;
			}
		}
		NavSimple.walkTowards(myLoc.directionTo(goalLoc));
	}

	private static int targetWeight(int dSquared) {
		if (dSquared > 100) {
			return 5;
		} else if (dSquared > 9) {
			return 2;
		} else {
			return 1;
		}
	}
}
