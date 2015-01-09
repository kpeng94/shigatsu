package droneKiteOD;

import java.util.Arrays;

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
		
		// Couriers retrieve supply from hq and drop it off at the rally point
		switch (myJob) {
		case COURIER:
			goalLoc = executeCourier();
			break;
		case DEFEND:
			goalLoc = executeDefend();
			break;
		case REPORT:
			goalLoc = executeReport();
			break;
		case ATTACK:
			goalLoc = executeAttack();
			break;
		default:
			goalLoc = null;
			break;				
		}		
		NavSimple.walkTowards(myLoc.directionTo(goalLoc));
	}

	private static MapLocation executeCourier() throws GameActionException {
		
		MapLocation goalLoc;
		RobotInfo[] allies = rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, myTeam);
		MapLocation rallyPt = new MapLocation(
				rc.readBroadcast(NEXT_RALLY_X_CHANNEL),
				rc.readBroadcast(NEXT_RALLY_Y_CHANNEL));
		
		if (Clock.getRoundNum() % INTERWAVE_TIME == INTERWAVE_TIME - 1) { // Supply drones if about to attack
			
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
		
		return goalLoc;
	}

	private static MapLocation executeDefend() throws GameActionException {

		MapLocation goalLoc;
		RobotInfo[] enemies = rc.senseNearbyRobots(typ.attackRadiusSquared / 2, otherTeam);
		MapLocation guardPt = new MapLocation(
				rc.readBroadcast(NEXT_GUARD_X_CHANNEL),
				rc.readBroadcast(NEXT_GUARD_Y_CHANNEL));

		if (enemies.length > 0) {
			goalLoc = myLoc;
			for (int i = 0; i < enemies.length; i++) {
				goalLoc.add(enemies[i].location.directionTo(myLoc));
			}
		} else {
			goalLoc = guardPt;
		}

		return goalLoc;
	}
	
	private static MapLocation executeReport() throws GameActionException {

		MapLocation goalLoc;
		RobotInfo[] enemies = rc.senseNearbyRobots(typ.attackRadiusSquared / 2, otherTeam);

		// Retreat - Run away from nearby enemies
		if (enemies.length > 0) {
			goalLoc = myLoc.add(evade(typ.sensorRadiusSquared));
		}
		// Swarm - Gather near rally point
		else {
			goalLoc = new MapLocation(
					rc.readBroadcast(NEXT_RALLY_X_CHANNEL),
					rc.readBroadcast(NEXT_RALLY_Y_CHANNEL));
		}
		return goalLoc;
	}
	
	private static MapLocation executeAttack() throws GameActionException {

		MapLocation goalLoc;
		RobotInfo[] enemies = rc.senseNearbyRobots(typ.attackRadiusSquared / 2, otherTeam);

		// Retreat - Run away from nearby enemies
		if (enemies.length > 0) {
			goalLoc = myLoc.add(evade(typ.sensorRadiusSquared));
		}
		// Swarm - Gather near rally point
		else {
			goalLoc = new MapLocation(
					rc.readBroadcast(NEXT_TARGET_X_CHANNEL),
					rc.readBroadcast(NEXT_TARGET_Y_CHANNEL));
		}
		return goalLoc;
	}
	
	private static Direction evade(int radius) throws GameActionException {
		RobotInfo[] nearby = rc.senseNearbyRobots(radius);
		
		// Set directions to which we cant move to None
		Direction[] available = MapUtils.dirs;
		for (int i = 0; i < available.length; i++) {
			if (!rc.canMove(available[i]))
				available[i] = Direction.NONE;
		}
		
		// Assign to each direction a cost based on surrounding allies and enemies
		double[] cost = new double[available.length];
		for (RobotInfo unit: nearby) {
			for (int j = 0; j < MapUtils.dirs.length; j++) {
				if (unit.team == myTeam && myLoc.directionTo(unit.location) == MapUtils.dirs[j]) {
					cost[j] += 5 / myLoc.distanceSquaredTo(unit.location);
				}
				else if (unit.team == otherTeam && myLoc.directionTo(unit.location) == MapUtils.dirs[j]) {
					cost[j] += 10 / myLoc.distanceSquaredTo(unit.location);
				}
			}
		}
		
		rc.setIndicatorString(0, "Available directions " + Arrays.toString(available));
		rc.setIndicatorString(1, "Costs are " + Arrays.toString(cost));
		rc.setIndicatorString(2, "Total of " + nearby.length + " units");
		
		// Return the direction of least cost
		Direction result = Direction.NONE;
		double minCost = Double.MAX_VALUE;
		for (int i = 0; i < cost.length; i++) {
			if (cost[i] < minCost) {
				result = available[i];
				minCost = cost[i];
			}
		}
		
		return result;
	}
}
