package tankBotv3;

import battlecode.common.*;

public class UTankHandler extends UnitHandler {

	private static RobotType myType = RobotType.TANK;
	private static RobotInfo[] enemies;
	private static int minDistance = Integer.MAX_VALUE;
	private static MapLocation closestLocation;
	private static TankState state = TankState.NEW;
	private static TankState nextState;
	private static int numberOfTanks = 0;
	private static boolean rallied = false;
	private static int numberOfTanksRallied = 0;
	private static MapLocation rallyPoint;
	
	private enum TankState {
		NEW,
		RALLY,
		RUSH,
		SWARM,
		FIGHTING
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
		typ = RobotType.TANK;
	}

	protected static void execute() throws GameActionException {
		executeUnit();
		readBroadcasts();
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
			case FIGHTING:
				fightingCode();
				break;
		}
		if (nextState != null) {
			state = nextState;
			nextState = null;
		}		
		Supply.spreadSupplies(Supply.DEFAULT_THRESHOLD);		
	}
	
	private static void fightingCode() throws GameActionException {
		nubMicro();
	}

	private static void nubMicro() throws GameActionException {
		RobotInfo[] enemies = rc.senseNearbyRobots(myType.sensorRadiusSquared, otherTeam);
		int[] closestEnemyInfo = getClosestEnemy(enemies);
		MapLocation closestEnemyLoc = new MapLocation(closestEnemyInfo[1], closestEnemyInfo[2]);
		int closestEnemyDistance = closestEnemyInfo[0];
		
		
	}
	
	private static void retreat(MapLocation enemyLoc) throws GameActionException {
		NavSimple.walkTowardsDirected(enemyLoc.directionTo(myLoc));
	}

	private static void rushCode() throws GameActionException {
		MapLocation destination = closestLocation.add(myHQToEnemyHQ, -3);
		if (closestLocation != null) {
			switch (myHQ.directionTo(closestLocation)) {
			case NORTH_EAST:
			case NORTH_WEST:
			case SOUTH_EAST:
			case SOUTH_WEST:
				destination = closestLocation.add(myHQToEnemyHQ.rotateRight(), -3).add(myHQToEnemyHQ.rotateLeft(), -2);
				break;
			case NORTH:
			case EAST:
			case SOUTH:
			case WEST:
				destination = closestLocation.add(myHQToEnemyHQ, -3).add(myHQToEnemyHQ.rotateRight().rotateRight(), -2);
				break;
			default:
				break;
			}
		}
		NavTangentBug.setDest(destination);
		NavTangentBug.calculate(2500);
		if (rc.isCoreReady() && rc.senseNearbyRobots(typ.attackRadiusSquared, otherTeam).length == 0) {
			Direction nextMove = NavTangentBug.getNextMove();
			if (nextMove != Direction.NONE) {
				NavSimple.walkTowardsDirected(nextMove);
			}
		}		
	}

	private static void rallyCode() throws GameActionException {
		if (rallyPoint == null) {
			rallyPoint = MapUtils.pointSection(myHQ, enemyHQ, 0.75);
		}
		NavTangentBug.setDest(rallyPoint);
		NavTangentBug.calculate(2500);
		if (rc.isCoreReady() && rc.senseNearbyRobots(typ.attackRadiusSquared, otherTeam).length == 0) {
			Direction nextMove = NavTangentBug.getNextMove();
			if (nextMove != Direction.NONE) {
				NavSimple.walkTowardsDirected(nextMove);
			} else {
				rallied = true;
			}
		}
		// TODO: figure out this heuristic better
		if (myLoc.distanceSquaredTo(rallyPoint) <= typ.attackRadiusSquared) {
			broadcastNearRallyPoint();
		}
		if (numberOfTanksRallied >= Constants.TANK_RUSH_COUNT) {
			broadcastTeamRush();
		}
		if (Comm.readBlock(Comm.getTankId(), 4) != 0 && rallied) {
			nextState = TankState.RUSH;
		}		
		
	}

	private static void newCode() {
		// TODO Auto-generated method stub
		nextState = TankState.RALLY;
		
	}
	
	/**
	 * TODO breaking ties between units that are equidistant?
	 * 
	 * Gets the closest enemy robot or HQ if list of enemies is empty.
	 * 
	 * @param enemies list of enemies
	 * @return integer array containing: [minimum distance to closest enemy, enemy's x location, enemy's y location]
	 * @throws GameActionException
	 */
	private static int[] getClosestEnemy(RobotInfo[] enemies) throws GameActionException {
		int minDistance = myLoc.distanceSquaredTo(enemyHQ);
		MapLocation closestEnemyLoc = enemyHQ;
		
		for (int i = enemies.length; --i >= 0;) {
			int distanceToEnemy = myLoc.distanceSquaredTo(enemies[i].location);
			if (distanceToEnemy < minDistance) {
				minDistance = distanceToEnemy;
				closestEnemyLoc = enemies[i].location;
			}
		}
		int[] distanceData = {minDistance, closestEnemyLoc.x, closestEnemyLoc.y};
		return distanceData;
	}
	
	/**
	 * 
	 * @param enemies
	 * @return
	 * @throws GameActionException
	 */
	private static double[] getEnemyWithLeastHealth(RobotInfo[] enemies) throws GameActionException {
		double leastHealth = 2000;
		MapLocation leastHealthEnemyLoc = enemyHQ;

		for (int i = enemies.length; --i >= 0;) {
			double enemyHealth = enemies[i].health;
			if (enemyHealth < leastHealth) {
				leastHealth = enemyHealth;
				leastHealthEnemyLoc = enemies[i].location;
			}
		}
		
		double[] data = {leastHealth, leastHealthEnemyLoc.x, leastHealthEnemyLoc.y};
		return data;

	}
	
	public static void readBroadcasts() throws GameActionException {
		numberOfTanks = Comm.readBlock(Comm.getTankId(), Comm.COUNT_OFFSET);
		numberOfTanksRallied = Comm.readBlock(Comm.getTankId(), Comm.COUNT_NEARRALLYPOINT_OFFSET);
	}

	public static void broadcastNearRallyPoint() throws GameActionException {
		numberOfTanksRallied++;
		Comm.writeBlock(Comm.getTankId(), Comm.COUNT_NEARRALLYPOINT_OFFSET, numberOfTanksRallied);
	}
	public static void broadcastTeamRush() throws GameActionException {
		Comm.writeBlock(Comm.getTankId(), 4, 1);
	}

	public static boolean decideAttack() {
		enemies = rc.senseNearbyRobots(typ.attackRadiusSquared, otherTeam);
		if (enemies.length > 0) {
			return true;
		}
		return false;
	}
	
	public static void detectEnemyKiting() throws GameActionException {
		
	}
	
	public static void attack() throws GameActionException {
		rc.attackLocation(enemies[0].location);
	}
}
