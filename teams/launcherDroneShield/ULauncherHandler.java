package launcherDroneShield;

import battlecode.common.*;

public class ULauncherHandler extends UnitHandler {
	
	public static final int LAUNCHER_RALLY_NUM = 5;
	public static final int NUM_RALLIED_CHANNEL = 22;
	public static final int RUSH_CHANNEL = 24;
	
	private static RobotInfo[] enemies;
	private static int minDistance;
	private static MapLocation closestLocation;
	private static LauncherState state;
	private static int numLaunchersRallied;
	private static boolean rallied;
	
	private enum LauncherState {
		RALLY,
		RUSH
	}
	
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

	protected static void init(RobotController rcon) throws GameActionException {
		initUnit(rcon);
		state = LauncherState.RALLY;
		numLaunchersRallied = 0;
		rallied = false;
	}

	protected static void execute() throws GameActionException {
		executeUnit();
		
		numLaunchersRallied = Comm.readBlock(Comm.getLauncherId(), NUM_RALLIED_CHANNEL);
		
		minDistance = Integer.MAX_VALUE;
		if (enemyTowers.length == 0) {
			closestLocation = enemyHQ;
		}
		for (int i = enemyTowers.length; --i >= 0;) {
			int distanceSquared = myHQ.distanceSquaredTo(enemyTowers[i]);
			if (distanceSquared <= minDistance) {
				closestLocation = enemyTowers[i];
				minDistance = distanceSquared;
			}
		}
		
		if (rc.isWeaponReady() && decideAttack()) {
			attack();
		}
		switch (state) {
			case RALLY:
				rallyCode();
				break;
			case RUSH:
				rushCode();
				break;
		}
		
		Supply.spreadSupplies(Supply.DEFAULT_THRESHOLD);
	}
	
	private static void rallyCode() throws GameActionException {
		rc.setIndicatorString(1, "Rallying");
		
		if (Comm.readBlock(Comm.getLauncherId(), RUSH_CHANNEL) == 1) {
			state = LauncherState.RUSH;
			rushCode();
			return;
		}
		
		MapLocation rallyPoint = Rally.get(LAUNCHER_RALLY_NUM);
		NavTangentBug.setDest(rallyPoint);
		NavTangentBug.calculate(2500);
		if (rc.isCoreReady() && rc.senseNearbyRobots(15, otherTeam).length == 0) {
			Direction nextMove = NavTangentBug.getNextMove();
			if (nextMove != Direction.NONE) {
				NavSimple.walkTowardsDirected(nextMove);
			} else {
				rallyPoint = myLoc;
				Rally.set(LAUNCHER_RALLY_NUM, myLoc);
			}
		}
		
		if (myLoc.distanceSquaredTo(rallyPoint) <= 8 && !rallied) {
			rc.setIndicatorString(0, "Incrementing num rallied");
			rallied = true;
			Comm.writeBlock(Comm.getLauncherId(), NUM_RALLIED_CHANNEL, ++numLaunchersRallied);
		}
		if (numLaunchersRallied >= Constants.LAUNCHER_RUSH_COUNT) {
			Comm.writeBlock(Comm.getLauncherId(), RUSH_CHANNEL, 1);
		}
	}

	private static void rushCode() throws GameActionException {
		rc.setIndicatorString(1, "Rushing");
		MapLocation destination = closestLocation.add(myHQ.directionTo(enemyHQ), -4);
		NavTangentBug.setDest(destination);
		NavTangentBug.calculate(2500);
		if (rc.isCoreReady() && rc.senseNearbyRobots(15, otherTeam).length == 0) {
			Direction nextMove = NavTangentBug.getNextMove();
			if (nextMove != Direction.NONE) {
				NavSimple.walkTowardsDirected(nextMove);
				Rally.set(UDroneHandler.DRONE_SHIELD_RALLY_NUM, myLoc.add(nextMove));
			}
		}
	}

	public static boolean decideAttack() {
		enemies = rc.senseNearbyRobots(typ.sensorRadiusSquared, otherTeam);
		if (enemies.length > 0 || closestLocation.distanceSquaredTo(myLoc) <= 35) {
			return true;
		}
		return false;
	}
	
	public static void attack() throws GameActionException {
		Direction dir;
		if (enemies.length > 0) {
			dir = myLoc.directionTo(enemies[0].location);			
		} else {
			dir = myLoc.directionTo(closestLocation);
		}
		if (rc.canLaunch(dir)) {
			rc.launchMissile(dir);
		}
	}

}

