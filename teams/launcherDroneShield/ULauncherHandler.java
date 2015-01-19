package launcherDroneShield;

import battlecode.common.*;

public class ULauncherHandler extends UnitHandler {
	
	private static RobotInfo[] enemies;
	private static int minDistance;
	private static MapLocation closestLocation;
	private static LauncherState state = LauncherState.RALLY;
	private static LauncherState nextLauncherState;
	private static boolean rallied = false;
	private static int numberOfLaunchersRallied = 0;

//	private static int mySensorRadiusSquared = myType.sensorRadiusSquared;
	
	private enum LauncherState {
		NEW,
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
	}

	protected static void execute() throws GameActionException {
		executeUnit();
		
		numberOfLaunchersRallied = Comm.readBlock(Comm.getLauncherId(), 22);
		
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
			case NEW:
				newCode();
				break;
			case RALLY:
				rallyCode();
				break;
			case RUSH:
				rushCode();
				break;
		}
		
		if (nextLauncherState != null) {
			state = nextLauncherState;
			nextLauncherState = null;
		}
		Supply.spreadSupplies(Supply.DEFAULT_THRESHOLD);
	}

	private static void rushCode() throws GameActionException {
		MapLocation destination = closestLocation.add(myHQ.directionTo(enemyHQ), -4);
		NavTangentBug.setDest(destination);
		NavTangentBug.calculate(2500);
		if (rc.isCoreReady() && rc.senseNearbyRobots(15, otherTeam).length == 0) {
			Direction nextMove = NavTangentBug.getNextMove();
			if (nextMove != Direction.NONE) {
				NavSimple.walkTowardsDirected(nextMove);
				Rally.forceSetTargetPt(myLoc.add(nextMove));
			}
		}
	}

	private static void rallyCode() throws GameActionException {
		MapLocation rallyPoint = MapUtils.pointSection(myHQ, enemyHQ, 0.75);
		NavTangentBug.setDest(rallyPoint);
		NavTangentBug.calculate(2500);			
		if (rc.isCoreReady() && rc.senseNearbyRobots(15, otherTeam).length == 0) {
			Direction nextMove = NavTangentBug.getNextMove();
			if (nextMove != Direction.NONE) {
				NavSimple.walkTowardsDirected(nextMove);
			} else {
				rallied = true;
			}
		}
		if (myLoc.distanceSquaredTo(rallyPoint) <= 8) {
			broadcastNearRallyPoint();
		}
		if (numberOfLaunchersRallied >= Constants.LAUNCHER_RUSH_COUNT) {
			broadcastTeamRush();
		}
		if (Comm.readBlock(Comm.getLauncherId(), 24) != 0 && rallied) {
			nextLauncherState = LauncherState.RUSH;
		}
	}

	private static void newCode() {
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

	public static void broadcastNearRallyPoint() throws GameActionException {
		numberOfLaunchersRallied++;
		Comm.writeBlock(Comm.getLauncherId(), 22, numberOfLaunchersRallied);
	}
	
	public static void broadcastTeamRush() throws GameActionException {
		Comm.writeBlock(Comm.getLauncherId(), 24, 1);
	}

}

