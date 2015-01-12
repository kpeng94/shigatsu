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
		rc.yield();
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
		// TODO Auto-generated method stub
		NavTangentBug.setDest(MapUtils.pointSection(myHQ, enemyHQ, 0.75));
		NavTangentBug.calculate(2500);			
		if (rc.isCoreReady() && rc.senseNearbyRobots(15, otherTeam).length == 0) {
			Direction nextMove = NavTangentBug.getNextMove();
			if (nextMove != Direction.NONE) {
				NavSimple.walkTowardsDirected(nextMove);
			}
		}
		rc.setIndicatorString(0, "" + numberOfLaunchers);
		if (numberOfLaunchers >= Constants.LAUNCHER_RUSH_COUNT) {
			nextLauncherState = LauncherState.RUSH;
		}
	}

	private static void newCode() {
		// TODO Auto-generated method stub
	}

	public static boolean decideAttack() {
		enemies = rc.senseNearbyRobots(15, otherTeam);
		if (enemies.length > 0) {
			return true;
		}
		return false;
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
		numberOfLaunchers  = Comm.readBlock(Comm.getLauncherId(), 1);
	}
}
