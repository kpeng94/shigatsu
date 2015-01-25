package commanderBot;

import battlecode.common.*;

public class UCommanderHandler extends UnitHandler {
	// Thresholds for when to stop safebugging around towers and HQ
	public static final int TOWER_THRESHOLD = 35;
	public static final int HQ_SMALL_THRESHOLD = 35;
	public static final int HQ_LARGE_THRESHOLD = 55;
	public static final int HQ_SPLASH_THRESHOLD = 75;
	public static final int ALLY_RADIUS = 15;
	
	public static int prevRally; // Previously rally point value (to determine when to update)
	public static MapLocation rallyPoint; // Commander rally point

	public static RobotInfo[] sensedEnemies; // enemies that I can sense
	public static RobotInfo[] attackableEnemies; // enemies that I can attack (subset of sensed)
	public static RobotInfo[] nearbyAllies; // allies within ALLY_RADIUS
	
	public static boolean prevNavTangent; // Whether the previous navigation was tangent bug or not
	
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
				e.printStackTrace();
				System.out.println(typ + " Execution Exception");
			}
			rc.yield(); // Yields to save remaining bytecodes
		}
	}

	protected static void init(RobotController rcon) throws GameActionException {
		initUnit(rcon);
		prevRally = 0;
		rallyPoint = enemyHQ;
		prevNavTangent = true;
	}

	protected static void execute() throws GameActionException {
		executeUnit();
		Count.incrementBuffer(Comm.getCommanderId());
		
		int rally = rc.readBroadcast(Comm.COMMANDER_RALLY_DEST_CHAN); // Checks for updated rally point
		if (rally != prevRally) {
			prevRally = rally;
			rallyPoint = MapUtils.decode(rally);
			prevNavTangent = true;
			NavTangentBug.setDestForced(rallyPoint);
		}
		
		// Calculate nearby enemies and allies
		attackableEnemies = rc.senseNearbyRobots(typ.attackRadiusSquared, otherTeam);
		sensedEnemies = rc.senseNearbyRobots(typ.sensorRadiusSquared, otherTeam);
		nearbyAllies = rc.senseNearbyRobots(ALLY_RADIUS, myTeam);

		if (Clock.getRoundNum() >= rc.readBroadcast(Comm.FINAL_PUSH_ROUND_CHAN)) { // FINAL PUSH!!! (ignore micro for most part)
			
		} else {
			if (sensedEnemies.length > 0) { // Gotta micro
				commanderMicro();
			} else { // No enemies in sight, just nav
				if (rc.isCoreReady()) {
					commanderNav();
				}
			}
		}

	}
	
	// Micro for commander
	// Attacks and moves because of no CD and LD
	protected static void commanderMicro() {
		if (rc.isWeaponReady()) {
			
		}
		if (rc.isCoreReady()) {
			
		}
	}
	
	// Navigation for commander, tries to get to rally point
	// Tries to tangent bug to rally point until it is no longer safe
	// Then it safe bugs around until it is far away from danger
	// Then it resumes tangent bugging
	// Assumes no enemies are nearby
	protected static void commanderNav() throws GameActionException {
		MapLocation closestTower = Attack.getClosestTower();
		int HQ_threshold = enemyTowers.length >= 5 ? HQ_SPLASH_THRESHOLD : (enemyTowers.length >= 2 ? HQ_LARGE_THRESHOLD : HQ_SMALL_THRESHOLD);
		if (myLoc.distanceSquaredTo(closestTower) > TOWER_THRESHOLD && myLoc.distanceSquaredTo(enemyHQ) > HQ_threshold) { // Out of range, resume tangent bugging
			if (!prevNavTangent) {
				NavTangentBug.setDestForced(rallyPoint);
			}
			NavTangentBug.calculate(2500);
			Direction nextMove = NavTangentBug.getNextMove();
			if (nextMove != Direction.NONE) {
				NavSimple.walkTowardsDirected(nextMove);
			}
			prevNavTangent = true;
		} else { // in danger range, safe bug around
			if (prevNavTangent) {
				NavSafeBug.resetDir();
			}
			Direction dir = NavSafeBug.dirToBugIn(rallyPoint);
			if (dir != Direction.NONE) {
				rc.move(dir);
			}
			prevNavTangent = false;
		}
	}
	
}
