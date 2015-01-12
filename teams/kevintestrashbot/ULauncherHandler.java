package kevintestrashbot;

import battlecode.common.*;

public class ULauncherHandler extends UnitHandler {
	
	private static RobotType myType = RobotType.LAUNCHER;
	private static RobotInfo[] enemies;
	private static int minDistance = Integer.MAX_VALUE;
	private static MapLocation closestLocation;
	private static LauncherState state = LauncherState.RALLY;
	private static LauncherState nextLauncherState;
	private static int numberOfLaunchers = 0;
	private static boolean rallied = false;
	private static int numberOfLaunchersRallied = 0;

//	private static int mySensorRadiusSquared = myType.sensorRadiusSquared;
	
	private enum LauncherState {
		NEW,
		RALLY,
		RUSH,
		SWARM
	}
	
	public static void loop(RobotController rcon) {
		try {
			init(rcon);
		} catch (Exception e) {
			 e.printStackTrace();
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
	}

	protected static void execute() throws GameActionException {
		executeUnit();
		readBroadcasts();
		for (MapLocation tower : enemyTowers) {
			int distanceSquared = myHQ.distanceSquaredTo(tower);
			if (distanceSquared <= minDistance) {
				closestLocation = tower;
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
		// TODO Auto-generated method stub
		NavTangentBug.setDest(closestLocation.add(myHQToEnemyHQ));
		NavTangentBug.calculate(2500);
		if (rc.isCoreReady() && rc.senseNearbyRobots(15, otherTeam).length == 0) {
			Direction nextMove = NavTangentBug.getNextMove();
			if (nextMove != Direction.NONE) {
				NavSimple.walkTowardsDirected(nextMove);
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
//		System.out.println(numberOfLaunchersRallied);
		if (numberOfLaunchersRallied >= Constants.LAUNCHER_RUSH_COUNT) {
			broadcastTeamRush();
		}
		if (Comm.readBlock(Comm.getLauncherId(), 4) != 0 && rallied) {
			nextLauncherState = LauncherState.RUSH;
		}
	}

	private static void newCode() {
	}

	public static boolean decideAttack() {
		enemies = rc.senseNearbyRobots(myType.sensorRadiusSquared, otherTeam);
		if (enemies.length > 0) {
			return true;
		}
		return false;
	}
	
	public static void detectEnemyKiting() throws GameActionException {
		
	}
	
	public static void attack() throws GameActionException {
		Direction dir = myLoc.directionTo(enemies[0].location);
		if (rc.canLaunch(dir)) {
			rc.launchMissile(dir);
		}
	}
	
	/**
	 * Calculates the best target square among the potential targets
	 * @param potentialTargets
	 * @return
	 * @throws GameActionException
	 */
	public MapLocation calculateBestTarget(RobotInfo[] robot) throws GameActionException {
		RobotInfo[] myTeamRobots = rc.senseNearbyRobots(myType.sensorRadiusSquared, myTeam);
		RobotInfo[] otherTeamRobots = rc.senseNearbyRobots(myType.sensorRadiusSquared, otherTeam);
		
		MapLocation bestLocation = null;
		return bestLocation; // will return null if there were no positive score enemy targets
	}
	
	public static void readBroadcasts() throws GameActionException {
		numberOfLaunchers = Comm.readBlock(Comm.getLauncherId(), 1);
		numberOfLaunchersRallied = Comm.readBlock(Comm.getLauncherId(), 2);
	}

	public static void broadcastNearRallyPoint() throws GameActionException {
		numberOfLaunchersRallied++;
		Comm.writeBlock(Comm.getLauncherId(), 2, numberOfLaunchersRallied);
	}
	
	public static void broadcastTeamRush() throws GameActionException {
		Comm.writeBlock(Comm.getLauncherId(), 4, 1);
	}

}
