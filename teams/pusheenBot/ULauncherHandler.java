package pusheenBot;

import battlecode.common.*;

public class ULauncherHandler extends UnitHandler {
	public static final int LAUNCHER_RUSH_COUNT = 3;

	private static RobotInfo[] enemies;
	private static int minDistance = 999999;
	private static MapLocation closestLocation;
	private static LauncherState state = LauncherState.RALLY;
	private static LauncherState nextState;
	private static boolean rallied = false;
	private static int numberOfLaunchersRallied = 0;
	private static int myWaveNumber = 1;
	private static MapLocation rallyPoint;


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
		typ = RobotType.LAUNCHER;
		myLoc = rc.getLocation();
		myWaveNumber = Comm.readBlock(Comm.getLauncherId(), Comm.WAVENUM_OFFSET);
		if (myWaveNumber % 3 == 1) {
	        closestLocation = MapUtils.decode(Comm.readBlock(Comm.getLauncherId(), Comm.WAVE_ONE_RALLYPOINT_OFFSET));
		} else if (myWaveNumber % 3 == 0) {
            closestLocation = MapUtils.decode(Comm.readBlock(Comm.getLauncherId(), Comm.WAVE_THREE_RALLYPOINT_OFFSET));
        } else if (myWaveNumber % 3 == 2) {
            closestLocation = MapUtils.decode(Comm.readBlock(Comm.getLauncherId(), Comm.WAVE_TWO_RALLYPOINT_OFFSET));
        }
		rallyPoint = MapUtils.pointSection(myLoc, closestLocation, 0.75);
//		minDistance = myLoc.distanceSquaredTo(closestLocation);
	}

	protected static void execute() throws GameActionException {
		executeUnit();
		Count.incrementBuffer(Comm.getLauncherId());
		minDistance = Integer.MAX_VALUE;

		boolean targetStillAlive = false;
		for (int i = enemyTowers.length; --i >= 0;) {
		    if (closestLocation == enemyTowers[i]) {
		        targetStillAlive = true;
		    }
		}
		if (!targetStillAlive) {
	        if (enemyTowers.length == 0) {
	            closestLocation = enemyHQ;
	        }
	        for (int i = enemyTowers.length; --i >= 0;) {
	            int distanceSquared = myLoc.distanceSquaredTo(enemyTowers[i]);
	            if (distanceSquared <= minDistance) {
	                closestLocation = enemyTowers[i];
	                minDistance = distanceSquared;
	            }
	        }
		}
		if (decideAttack()) {
			if (rc.getMissileCount() > 0) {
				attackAndMove();
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
			if (myLoc.distanceSquaredTo(closestLocation) <= 35) {
				NavSimple.walkTowardsSafe(myLoc.directionTo(closestLocation));
			} else if (nextMove != Direction.NONE) {
				NavSimple.walkTowardsDirected(nextMove);
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
					NavSimple.walkTowardsDirected(nextMove);
				} else {
					rallied = true;
				}
			}
		}
		numberOfLaunchersRallied = Count.getCountAtRallyPoint(Comm.getLauncherId(), myWaveNumber);
		//        rc.setIndicatorString(1, "My wave number is: " + myWaveNumber + ". The number of tanks that rallied is given by: " + numberOfTanksRallied);
		//        rc.setIndicatorString(2, "My loc is " + myLoc + "my dist is " + );
		// TODO: figure out this heuristic better

		if (myLoc.distanceSquaredTo(rallyPoint) <= typ.sensorRadiusSquared) {
			//            rc.setIndicatorString(0, "At clock turn " + Clock.getRoundNum()
			//                    + ", I am rallying to the rally point at: " + rallyPoint
			//                    + " and my rallied boolean is: " + rallied);
			Count.incrementAtRallyPoint(Comm.getLauncherId(), myWaveNumber);
		}
		if (numberOfLaunchersRallied >= LAUNCHER_RUSH_COUNT) {
			nextState = LauncherState.RUSH;
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

	public static void detectEnemyKiting() throws GameActionException {

	}

	public static void attackAndMove() throws GameActionException {
		Direction dir;
		RobotInfo closestEnemy = null;
		if (enemies.length > 0) {
			closestEnemy = Attack.getClosestEnemy(enemies);
			dir = myLoc.directionTo(closestEnemy.location);
		} else {
			dir = myLoc.directionTo(closestLocation);
		}
		if (rc.canLaunch(dir)) {
			rc.launchMissile(dir);
		}
		if (closestEnemy != null && rc.isCoreReady()) {
			if (closestEnemy.location.distanceSquaredTo(myLoc) <= 15) {
				NavSimple.walkTowardsSafe(closestEnemy.location.directionTo(myLoc));
			}
		}
	}

}
