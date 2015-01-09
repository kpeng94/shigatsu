package tankBotv2;

import java.util.Random;

import battlecode.common.*;

public class TankRobot extends Robot {
	public static Direction directionToEnemyHQ;
	public static RobotInfo[] enemies;
	static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	static int numberOfLiveTanks = 1; // one for itself
	static MapLocation rallyPoint;
	static boolean rallied = false;
	
	public static void init(RobotController rc) throws GameActionException {
        rand = new Random(rc.getID());
		myRange = rc.getType().attackRadiusSquared;
		myTeam = rc.getTeam();
		enemyTeam = myTeam.opponent();
		myTeamHQ = rc.senseHQLocation();
		enemyTeamHQ = rc.senseEnemyHQLocation();
		directionToEnemyHQ = myTeamHQ.directionTo(enemyTeamHQ);
		rallyPoint = MapUtils.pointSection(myTeamHQ, enemyTeamHQ, 0.75);		
	}
	
	public static void run(RobotController controller) throws Exception {
		rc = controller;
		init(rc);
		while(true) {
			readNumberOfLiveTanks();
			rc.setIndicatorString(0, "There are # beavs:" + numberOfLiveTanks);
			myLocation = rc.getLocation();
			if (rc.isWeaponReady() && decideAttack()) {
				attack();
			}
			if (rc.isCoreReady() && rc.senseNearbyRobots(RobotType.TANK.attackRadiusSquared, enemyTeam).length == 0) {
				if (numberOfLiveTanks < 4) {
					rallied = false;
					tryMove(rc.getLocation().directionTo(rallyPoint));
				}
				else if (numberOfLiveTanks < 10 && !rallied) {
					tryMove(rc.getLocation().directionTo(rallyPoint));
				}
//				tryBuild(directions[rand.nextInt(8)],RobotType.BARRACKS);
				else {
					rallied = true;
					tryMove(rc.getLocation().directionTo(rc.senseEnemyHQLocation()));					
				}
//				tryMove(rc.senseHQLocation().directionTo(rc.getLocation()));
			}
			rc.yield();
		}
	}

	
	public static void readNumberOfLiveTanks() throws GameActionException {
		numberOfLiveTanks = rc.readBroadcast(TANK_COUNT_CHANNEL);
	}
	
	public static boolean decideAttack() {
		enemies = rc.senseNearbyRobots(myRange, enemyTeam);
		if (enemies.length > 0) {
			return true;
		}
		return false;
	}
	
	public static void attack() throws GameActionException {
		rc.attackLocation(enemies[0].location);
	}

	
	static void tryMove(Direction d) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = {0,1,-1,2,-2};
		int dirint = directionToInt(d);
		while (offsetIndex < 5 && !rc.canMove(directions[(dirint+offsets[offsetIndex]+8)%8])) {
			offsetIndex++;
		}
		if (offsetIndex < 5) {
			rc.move(directions[(dirint+offsets[offsetIndex]+8)%8]);
		}
	}	

	/**
	 * 
	 * @param d
	 * @param type
	 * @throws GameActionException
	 */
	static void tryBuild(Direction d, RobotType type) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = {0,1,-1,2,-2,3,-3,4};
		int dirint = directionToInt(d);
		while (offsetIndex < 8 && !rc.canMove(directions[(dirint+offsets[offsetIndex]+8)%8])) {
			offsetIndex++;
		}
		if (offsetIndex < 8) {
			rc.build(directions[(dirint+offsets[offsetIndex]+8)%8], type);
		}
	}
	
	

	static int directionToInt(Direction d) {
		switch (d) {
			case NORTH:
				return 0;
			case NORTH_EAST:
				return 1;
			case EAST:
				return 2;
			case SOUTH_EAST:
				return 3;
			case SOUTH:
				return 4;
			case SOUTH_WEST:
				return 5;
			case WEST:
				return 6;
			case NORTH_WEST:
				return 7;
			default:
				return -1;
		}
	}	
}
