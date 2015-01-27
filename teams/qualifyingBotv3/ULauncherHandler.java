package qualifyingBotv3;

import battlecode.common.*;

public class ULauncherHandler extends UnitHandler {
	public static final int LAUNCHER_RUSH_COUNT = 4;
	public static final int PROXIMITY_DIST = 75;
	
	private static LauncherState state = LauncherState.RALLY;
	private static LauncherState nextState;
	private static int numberOfLaunchersRallied = 0;
	private static int myWaveNumber = 1;
	private static MapLocation rallyPoint;
	
	private static RobotInfo[] sensedEnemies;
	private static MapLocation closestLocation;
	private static RobotInfo closestEnemy = null;
	private static int numMissilesToShoot = 3;
	
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
		myWaveNumber = Comm.readBlock(Comm.getLauncherId(), Comm.WAVENUM_OFFSET);
		rallyPoint = MapUtils.pointSection(myHQ, enemyHQ, 0.75);
	}

	protected static void execute() throws GameActionException {
		executeUnit();
		Count.incrementBuffer(Comm.getLauncherId());
		
		int pushRound = rc.readBroadcast(Comm.FINAL_PUSH_ROUND_CHAN);
//		if (pushRound > 0 && Clock.getRoundNum() >= pushRound) {
			state = LauncherState.RUSH;
//		}
		
		int numMyTowers = rc.senseTowerLocations().length;
		int numEnemyTowers = enemyTowers.length;
		
		if (numEnemyTowers > 0) {
			int minToMe = 999999;
			int minToHQ = 999999;
			MapLocation closestToMe = null;
			MapLocation closestToHQ = null;
			for (int i = enemyTowers.length; --i >= 0;) {
				MapLocation tower = enemyTowers[i];
				int distToMe = myLoc.distanceSquaredTo(tower);
				int distToHQ = myHQ.distanceSquaredTo(tower);
				if (distToMe < minToMe) {
					minToMe = distToMe;
					closestToMe = tower;
				}
				if (distToHQ < minToHQ) {
					minToHQ = distToHQ;
					closestToHQ = tower;
				}
			}
			
			if (minToMe <= PROXIMITY_DIST) { // Close enough to a structure to siege
				closestLocation = closestToMe;
			} else {
				if (pushRound > 0 && Clock.getRoundNum() >= pushRound && numEnemyTowers < 5 && numMyTowers < numEnemyTowers) { // final push!!!
					closestLocation = enemyHQ;
				}
//				if (numEnemyTowers < 3 && myLoc.distanceSquaredTo(enemyHQ) < minToMe) {
//					System.out.println("WAT");
//					closestLocation = enemyHQ;
//				}
				if (minToMe <= myLoc.distanceSquaredTo(closestToHQ)) { // twice as close to structure
					closestLocation = closestToMe;
				} else {
					closestLocation = closestToHQ;
				}
			}
		} else {
			closestLocation = enemyHQ;
		}
		
		if (decideAttack()) {
			if (rc.getMissileCount() > 0) {
				launcherAttack();
			}
			if (rc.isCoreReady()) {
				launcherMove();
			}
		} else {
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
			default:
				break;
			}
		}
		
		if (rc.getSupplyLevel() < 1000) {
			Supply.dumpSupplyTo(myLoc, RobotType.LAUNCHER);
		}

		if (nextState != null) {
			state = nextState;
			nextState = null;
		}
		Supply.spreadSupplies(Supply.DEFAULT_THRESHOLD);
	}

	private static void rushCode() throws GameActionException {
		MapLocation destination = closestLocation;
		NavTangentBug.setDest(destination);
		NavTangentBug.calculate(2500);
		if (rc.isCoreReady()
				&& rc.senseNearbyRobots(typ.attackRadiusSquared, otherTeam).length == 0) {
			Direction nextMove = NavTangentBug.getNextMove();
			if (myLoc.distanceSquaredTo(closestLocation) <= PROXIMITY_DIST) {
				NavSimple.walkTowardsSafe(myLoc.directionTo(closestLocation));
			} else if (nextMove != Direction.NONE) {
				NavSimple.walkTowardsSafeAll(nextMove);
			}
		}
	}

	private static void rallyCode() throws GameActionException {
		NavTangentBug.setDest(rallyPoint);
		NavTangentBug.calculate(2500);
		// TODO (kpeng94): maybe change this so that when you're in a really close region, bug
		// around instead
		if (rc.isCoreReady()) {
			if (rc.senseNearbyRobots(typ.attackRadiusSquared, otherTeam).length == 0) {
				Direction nextMove = NavTangentBug.getNextMove();
				if (nextMove != Direction.NONE) {
					NavSimple.walkTowardsSafeAll(nextMove);
				}
			}
		}
		numberOfLaunchersRallied = Count.getCountAtRallyPoint(Comm.getLauncherId(), myWaveNumber);
		if (myLoc.distanceSquaredTo(rallyPoint) <= typ.sensorRadiusSquared) {
			Count.incrementAtRallyPoint(Comm.getLauncherId(), myWaveNumber);
		}
		if (numberOfLaunchersRallied >= LAUNCHER_RUSH_COUNT) {
			nextState = LauncherState.RUSH;
		}
	}

	private static void newCode() {
	}

	public static boolean decideAttack() {
		sensedEnemies = rc.senseNearbyRobots(typ.sensorRadiusSquared, otherTeam);
		boolean isNonWimpyEnemy = false;
		numMissilesToShoot = 3;
		int numWimpy = 0;
		if (sensedEnemies.length > 0) {
			int minDist = 999999;
			closestEnemy = null;
			for (int i = sensedEnemies.length; --i >= 0;) {
				RobotInfo enemy = sensedEnemies[i];
				MapLocation enemyLoc = enemy.location;
				int enemyDist = Handler.myLoc.distanceSquaredTo(enemyLoc);
				if (enemyDist < minDist) {
					minDist = enemyDist ;
					closestEnemy = sensedEnemies[i];
				}
				if (!isNonWimpyEnemy) {
					if (enemy.type.isBuilding || enemy.type == RobotType.COMMANDER || enemy.type == RobotType.TANK || enemy.type == RobotType.LAUNCHER) {
						isNonWimpyEnemy = true;
					} else {
						numWimpy++;
					}
				}
			}
			closestEnemy = Attack.getClosestEnemy(sensedEnemies);			
		}
		
		if (!isNonWimpyEnemy && sensedEnemies.length > 0) {
			RobotInfo[] allies = rc.senseNearbyRobots(typ.sensorRadiusSquared, myTeam);
			int numMissiles = 0;
			for (int i = allies.length; --i >= 0;) {
				if (allies[i].type == RobotType.MISSILE) {
					numMissiles++;
				}
			}
			if (numMissiles >= 2 * numWimpy) {
				numMissilesToShoot = 0;
			} else {
				int remaining = 2 * numWimpy - numMissiles;
				numMissilesToShoot = remaining >= 3 ? 3 : remaining;
			}
		}
		
		if (sensedEnemies.length > 0 || closestLocation.distanceSquaredTo(myLoc) <= 52) {
			return true;
		}
		return false;
	}

	public static void detectEnemyKiting() throws GameActionException {

	}

	public static void launcherAttack() throws GameActionException {
		Direction dir;
		if (sensedEnemies.length > 0) {
			dir = myLoc.directionTo(closestEnemy.location);
		} else {
			if (myLoc.distanceSquaredTo(closestLocation) > 45) {
				return;
			}
			dir = myLoc.directionTo(closestLocation);
		}
		if (numMissilesToShoot > 0 && rc.canLaunch(dir)) {
			rc.launchMissile(dir);
			numMissilesToShoot--;
		}
		if (numMissilesToShoot > 0 && rc.canLaunch(dir.rotateLeft())) {
			rc.launchMissile(dir.rotateLeft());
			numMissilesToShoot--;
		}
		if (numMissilesToShoot > 0 && rc.canLaunch(dir.rotateRight())) {
			rc.launchMissile(dir.rotateRight());
			numMissilesToShoot--;
		}
	}
	
	public static void launcherMove() throws GameActionException {
		if (sensedEnemies.length == 0) { // No nearby enemies
			Direction toClosest = myLoc.directionTo(closestLocation);
			if (NavSafeBug.safeTile(myLoc.add(toClosest))) {
				Direction dir = NavSafeBug.dirToBugIn(closestLocation);
				if (dir != Direction.NONE) {
					rc.move(dir);
				}
			}
		} else { // There are nearby enemies
			if (rc.getMissileCount() == 0) { // Out of missiles
				NavSimple.walkTowardsSafe(closestEnemy.location.directionTo(myLoc));
			} else if (rc.getMissileCount() <= 2) { // low on missiles
				if (myLoc.distanceSquaredTo(closestEnemy.location) <= 18) {
					NavSimple.walkTowardsSafe(closestEnemy.location.directionTo(myLoc));
				}
			} else {
				if (myLoc.distanceSquaredTo(closestEnemy.location) <= 15) {
					NavSimple.walkTowardsSafe(closestEnemy.location.directionTo(myLoc));
				}
			}
		}
//		if (closestEnemy != null && rc.isCoreReady()) {
//			if (closestEnemy.location.distanceSquaredTo(myLoc) <= 18) {
//				NavSimple.walkTowardsSafe(closestEnemy.location.directionTo(myLoc));
//			}
//		}
	}

}
