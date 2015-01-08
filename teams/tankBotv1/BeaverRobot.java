package tankBotv1;

import java.util.Random;

import battlecode.common.*;

public class BeaverRobot extends Robot {
	public static Direction directionToEnemyHQ;
	public static RobotInfo[] enemies;
	static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	static int numberOfLiveBeavers = 1; // one for itself
	static MapLocation rallyPoint;
	static boolean rallied = false;
	static int job = 0;
	static int directionToEnemyHQInt;
	private static int distanceToEnemyHQ;
	private static int numBarracksBuilt = 0;
	
	public static void init(RobotController rc) throws GameActionException {
        rand = new Random(rc.getID());
		myRange = rc.getType().attackRadiusSquared;
		myTeam = rc.getTeam();
		enemyTeam = myTeam.opponent();
		myTeamHQ = rc.senseHQLocation();
		enemyTeamHQ = rc.senseEnemyHQLocation();
		directionToEnemyHQ = myTeamHQ.directionTo(enemyTeamHQ);
		directionToEnemyHQInt = directionToInt(directionToEnemyHQ);
		distanceToEnemyHQ = myTeamHQ.distanceSquaredTo(enemyTeamHQ);
		rallyPoint = MapUtils.pointSection(myTeamHQ, enemyTeamHQ, 0.75);
		getJob();
	}
	
	public static void run(RobotController controller) throws Exception {
		rc = controller;
		init(rc);
		while(true) {
			readNumberOfLiveBeavers();
			rc.setIndicatorString(0, "There are # beavs:" + numberOfLiveBeavers);
			myLocation = rc.getLocation();
			if (rc.isWeaponReady() && decideAttack()) {
				attack();
			}
			if (job == 0) {		
				if (rc.isCoreReady()) {
					if (numberOfLiveBeavers < 10) {
						rallied = false;
						tryMove(rc.getLocation().directionTo(rallyPoint));
					}
					else if (numberOfLiveBeavers < 50 && !rallied) {
						tryMove(rc.getLocation().directionTo(rallyPoint));
					}
	//				tryBuild(directions[rand.nextInt(8)],RobotType.BARRACKS);
					else {
						rallied = true;
						tryMove(rc.getLocation().directionTo(rc.senseEnemyHQLocation()));					
					}
	//				tryMove(rc.senseHQLocation().directionTo(rc.getLocation()));
				}
			} else if (job == SCOUT_BEAVER_0) {
				if (rc.isCoreReady()) {
					if (myLocation.distanceSquaredTo(myTeamHQ) + 
						myLocation.distanceSquaredTo(enemyTeamHQ) > distanceToEnemyHQ + 200 || 
						myLocation.distanceSquaredTo(myTeamHQ) * 3 > myLocation.distanceSquaredTo(enemyTeamHQ)) {
						if (rc.getTeamOre() >= RobotType.BARRACKS.oreCost && numBarracksBuilt < 1) {
							tryBuild(directions[rand.nextInt(8)],RobotType.BARRACKS);			
							numBarracksBuilt++;
						}
					} else {
						tryMove(directions[(directionToEnemyHQInt + 8) % 8]);					
					}
				}
			} else if (job == SCOUT_BEAVER_1) {
				if (rc.isCoreReady()) {
					if (myLocation.distanceSquaredTo(myTeamHQ) + 
							myLocation.distanceSquaredTo(enemyTeamHQ) > distanceToEnemyHQ + 200) {
							rc.setIndicatorString(1, "About to try to build");
							if (rc.getTeamOre() >= RobotType.TANKFACTORY.oreCost) {
								tryBuild(directions[rand.nextInt(8)],RobotType.TANKFACTORY);
							} else {
								if (rc.senseOre(myLocation) >= 20 && rc.canMine()) {
									rc.mine();
								} else {
									tryMove(directions[rand.nextInt(8)]);
								}
							}
					} else {
//						Need to figure out where the best places to mine are.
						tryMove(directions[(directionToEnemyHQInt + 1) % 8]);					
					}
				}
			} else if (job == SCOUT_BEAVER_7) {
				if (rc.isCoreReady()) {
					if (myLocation.distanceSquaredTo(myTeamHQ) + 
							myLocation.distanceSquaredTo(enemyTeamHQ) > distanceToEnemyHQ + 200) {
							rc.setIndicatorString(2, "About to try to build");
							if (rc.getTeamOre() >= RobotType.TANKFACTORY.oreCost) {
								tryBuild(directions[rand.nextInt(8)],RobotType.TANKFACTORY);
							}
					} else {
						tryMove(directions[(directionToEnemyHQInt + 7) % 8]);					
					}
				}
			}

			rc.yield();
		}
	}
	
	/**
	 * Gets the purpose of this bot as soon as the 
	 * @throws GameActionException 
	 */
	private static void getJob() throws GameActionException {
		job = rc.readBroadcast(Clock.getRoundNum());
	}

	public static MapLocation getScoutLocation() {
			
		return null;
	}
	
	/**
	 * Scout in directions that are generally away from the enemy HQ.
	 */
	public static void scout() {

//		switch (directionToEnemyHQ) {
//			case WEST:
//			case NORTH:
//			case EAST:
//			case SOUTH:
//				
//				break;
//			case NORTH_EAST:
//			case NORTH_WEST:
//			case SOUTH_EAST:
//			case SOUTH_WEST:
//				offsets = {0,1,-1,2,-2};
//				break;
//		}
//		
	}
	
	public static void updateInformation() {
	}
	
	public static void readNumberOfLiveBeavers() throws GameActionException {
		numberOfLiveBeavers = rc.readBroadcast(67);
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
