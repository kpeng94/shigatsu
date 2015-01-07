package droneTestSwarm;

import battlecode.common.*;

import java.util.*;

public class RobotPlayer {

	// Channel constants
	private static final int NUM_BEAVER_CHANNEL = 0;
	private static final int NUM_DRONE_CHANNEL = 1;
	private static final int NUM_COURIER_CHANNEL = 3;
	private static final int NUM_HELIPAD_CHANNEL = 100;
	private static final int WAVE_NUMBER_CHANNEL = 2;
	private static final int DRONE_PROMOTION_CHANNEL = 10;
	private static final int NEXT_RALLY_X_CHANNEL = 11;
	private static final int NEXT_RALLY_Y_CHANNEL = 12;
	private static final int NEXT_TARGET_X_CHANNEL = 13;
	private static final int NEXT_TARGET_Y_CHANNEL = 15;

	// Subjobs
	private enum Job {
		NONE, REPORT, ATTACK, COURIER;
	}

	// Map for specific jobs
	static Job job;
	static int nextTriggerRound;
	static int waveNumber;

	static RobotController rc;
	static Team myTeam;
	static Team enemyTeam;
	static int myRange;
	static Random rand;
	static Direction[] directions = { Direction.NORTH, Direction.NORTH_EAST,
			Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH,
			Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST };

	public static void run(RobotController tomatojuice) {
		rc = tomatojuice;
		rand = new Random(rc.getID());

		myRange = rc.getType().attackRadiusSquared;
		MapLocation enemyLoc = rc.senseEnemyHQLocation();
		Direction lastDirection = null;
		myTeam = rc.getTeam();
		enemyTeam = myTeam.opponent();
		RobotInfo[] myRobots;

		// employment = new IdMap<Job>();
		job = Job.NONE;
		nextTriggerRound = 0;
		waveNumber = 1;

		while (true) {
			try {
				rc.setIndicatorString(0, "This is an indicator string.");
				rc.setIndicatorString(1, "I am a " + rc.getType());
			} catch (Exception e) {
				e.printStackTrace();
			}
			switch (rc.getType()) {
			case HQ:
				try {
					rc.setIndicatorString(2,
							"Team ore count: " + rc.getTeamOre());
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					int fate = rand.nextInt(10000);
					myRobots = rc.senseNearbyRobots(999999, myTeam);
					int numBeavers = 0;
					int numDrones = 0;
					int numHelipads = 0;
					for (RobotInfo r : myRobots) {
						RobotType type = r.type;
						if (type == RobotType.DRONE) {
							numDrones++;
						} else if (type == RobotType.BEAVER) {
							numBeavers++;
						} else if (type == RobotType.HELIPAD) {
							numHelipads++;
						}
					}
					
					// Give supply to every drone in the vicinity if they need some
					RobotInfo[] allies = rc.senseNearbyRobots(rc.getLocation(), 15, myTeam);
					for (int i = 0; i < allies.length; i++) {
						if (allies[i].type == RobotType.DRONE && allies[i].supplyLevel == 0) {
							rc.transferSupplies(Math.max((int) rc.getSupplyLevel(), 300), allies[i].location);
						}
					}

					// Designate the next area to build helipads
					// Initially, choose the friendly tower closest to enemy
					// towers
					MapLocation nextRallySite = rc.senseHQLocation();
					MapLocation nextTargetSite = rc.senseHQLocation();
					int minDistanceFromEnemy = Integer.MAX_VALUE;

					MapLocation[] myTowers = rc.senseTowerLocations();
					MapLocation[] enemyTowers = rc.senseEnemyTowerLocations();
					for (int i = 0; i < myTowers.length; i++) {
						for (int j = 0; j < enemyTowers.length; j++) {
							int distanceFromEnemy = myTowers[i]
									.distanceSquaredTo(enemyTowers[j]);
							if (distanceFromEnemy < minDistanceFromEnemy) {
								nextRallySite = myTowers[i];
								nextTargetSite = enemyTowers[j];
								minDistanceFromEnemy = distanceFromEnemy;
							}
						}
					}

					rc.broadcast(NUM_BEAVER_CHANNEL, numBeavers);
					rc.broadcast(NUM_DRONE_CHANNEL, numDrones);
					rc.broadcast(NUM_HELIPAD_CHANNEL, numHelipads);
					rc.broadcast(WAVE_NUMBER_CHANNEL, waveNumber);

					rc.broadcast(NEXT_RALLY_X_CHANNEL, nextRallySite.x);
					rc.broadcast(NEXT_RALLY_Y_CHANNEL, nextRallySite.y);
					rc.broadcast(NEXT_TARGET_X_CHANNEL, nextTargetSite.x);
					rc.broadcast(NEXT_TARGET_Y_CHANNEL, nextTargetSite.y);

					if (Clock.getRoundNum() == nextTriggerRound) {
						rc.broadcast(DRONE_PROMOTION_CHANNEL, 1);
						nextTriggerRound += 250;
						waveNumber++;
					} else {
						rc.broadcast(DRONE_PROMOTION_CHANNEL, 0);
					}

					if (rc.isWeaponReady()) {
						attackSomething();
					}

					if (rc.isCoreReady() && rc.getTeamOre() >= 100
							&& fate < Math.pow(1.2, 12 - numBeavers) * 10000) {
						trySpawn(directions[rand.nextInt(8)], RobotType.BEAVER);
					}
				} catch (Exception e) {
					System.out.println("HQ Exception");
					e.printStackTrace();
				}
				break;

			case TOWER:
				try {
					if (rc.isWeaponReady()) {
						attackSomething();
					}
				} catch (Exception e) {
					System.out.println("Tower Exception");
					e.printStackTrace();
				}
				break;

			case BEAVER:
				try {
					if (rc.isWeaponReady()) {
						attackSomething();
					}
					if (rc.isCoreReady()) {
						int fate = rand.nextInt(1000);
						if (fate < 8 && rc.getTeamOre() >= 300) {
							tryBuild(directions[rand.nextInt(8)],
									RobotType.HELIPAD);
						} else if (fate < 600) {
							rc.mine();
						} else if (fate < 900) {
							tryMove(directions[rand.nextInt(8)]);
						} else {
							tryMove(rc.senseHQLocation().directionTo(
									rc.getLocation()));
						}
					}
				} catch (Exception e) {
					System.out.println("Beaver Exception");
					e.printStackTrace();
				}
				break;

			case DRONE:
				try {

					if (job == Job.NONE) {
						int numCouriers = rc.readBroadcast(NUM_COURIER_CHANNEL);
						if (numCouriers < rc.readBroadcast(WAVE_NUMBER_CHANNEL)) {
							job = Job.COURIER;
							System.out.println("Created Courier");
							rc.broadcast(NUM_COURIER_CHANNEL, numCouriers + 1);
						} else
							job = Job.REPORT;
					} else if (rc.readBroadcast(DRONE_PROMOTION_CHANNEL) == 1
							&& job == Job.REPORT) {
						job = Job.ATTACK;
					}

					rc.setIndicatorString(2, "I do" + job);

					if (rc.isWeaponReady()) {
						attackSomething();
					}
					if (rc.isCoreReady()) {

						MapLocation myLoc = rc.getLocation();
						MapLocation goalLoc;

						if (job == Job.COURIER) {
							
							int mySupply = (int) rc.getSupplyLevel();
							MapLocation rallyPt = new MapLocation(
									rc.readBroadcast(NEXT_RALLY_X_CHANNEL),
									rc.readBroadcast(NEXT_RALLY_Y_CHANNEL));
							MapLocation myHq = rc.senseHQLocation();
							
							if (mySupply > 0) {	// Dropoff supply
								if (myLoc.distanceSquaredTo(rallyPt) < 15) {
									rc.transferSupplies(mySupply, rallyPt);
									goalLoc = myHq;
								} else {
									goalLoc = rallyPt;
								}
							} else { // Retrieve supply
								goalLoc = myHq;
							}
						} else {
							MapLocation rallyPt;

							if (job == Job.REPORT) {
								rallyPt = new MapLocation(
										rc.readBroadcast(NEXT_RALLY_X_CHANNEL),
										rc.readBroadcast(NEXT_RALLY_Y_CHANNEL));
							} else {
								rallyPt = new MapLocation(
										rc.readBroadcast(NEXT_TARGET_X_CHANNEL),
										rc.readBroadcast(NEXT_TARGET_Y_CHANNEL));
							}

							RobotInfo[] allies = rc.senseNearbyRobots(
									myRange / 2, myTeam);
							RobotInfo[] enemies = rc.senseNearbyRobots(
									myRange / 2, enemyTeam);
							// Retreat - Run away from nearby enemies
							if (enemies.length > 0) {
								if (allies.length > enemies.length) {
									goalLoc = myLoc;
									for (int i = 0; i < enemies.length; i++) {
										goalLoc.add(enemies[i].location
												.directionTo(myLoc));
									}
								} else {
									goalLoc = myLoc
											.add(myLoc.directionTo(rallyPt),
													targetWeight(myLoc
															.distanceSquaredTo(rallyPt)));
									MapLocation closestAlly = myLoc;
									int minDistanceFromAlly = Integer.MAX_VALUE;
									for (int i = 0; i < allies.length; i++) {
										int distanceFromAlly = myLoc
												.distanceSquaredTo(allies[i].location);
										if (distanceFromAlly < minDistanceFromAlly) {
											closestAlly = allies[i].location;
											minDistanceFromAlly = distanceFromAlly;
										}
									}
									goalLoc = goalLoc.add(
											myLoc.directionTo(closestAlly), 5);
								}
							}
							// Swarm - Gather near rally point and repel away
							// from
							// allies
							else {
								goalLoc = myLoc.add(myLoc.directionTo(rallyPt),
										targetWeight(myLoc
												.distanceSquaredTo(rallyPt)));
								if (allies.length > 0) {

									MapLocation closestAlly = myLoc;
									int minDistanceFromAlly = Integer.MAX_VALUE;
									for (int i = 0; i < allies.length; i++) {
										int distanceFromAlly = myLoc
												.distanceSquaredTo(allies[i].location);
										if (distanceFromAlly < minDistanceFromAlly) {
											closestAlly = allies[i].location;
											minDistanceFromAlly = distanceFromAlly;
										}
									}
									goalLoc = goalLoc.add(
											myLoc.directionTo(closestAlly), -3);
								}
							}
						}
						tryMove(myLoc.directionTo(goalLoc));

					}
				} catch (Exception e) {
					System.out.println("Drone Exception");
					e.printStackTrace();
				}
				break;

			case HELIPAD:
				try {
					int fate = rand.nextInt(10000);

					// get information broadcasted by the HQ
					int numBeavers = rc.readBroadcast(0);
					int numDrones = rc.readBroadcast(1);

					if (rc.isCoreReady()
							&& rc.getTeamOre() >= 125
							&& fate < Math
									.pow(1.2, 15 - numDrones + numBeavers) * 10000) {
						trySpawn(directions[rand.nextInt(8)], RobotType.DRONE);
					}
				} catch (Exception e) {
					System.out.println("Helipad Exception");
					e.printStackTrace();
				}
				break;
			default:
				break;
			}

			rc.yield();
		}
	}

	// This method will attack an enemy in sight, if there is one
	static void attackSomething() throws GameActionException {
		RobotInfo[] enemies = rc.senseNearbyRobots(myRange, enemyTeam);
		if (enemies.length > 0) {
			rc.attackLocation(enemies[0].location);
		}
	}

	// This method will attempt to move in Direction d (or as close to it as
	// possible)
	static void tryMove(Direction d) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = { 0, 1, -1, 2, -2 };
		int dirint = directionToInt(d);
		boolean blocked = false;
		while (offsetIndex < 5
				&& !rc.canMove(directions[(dirint + offsets[offsetIndex] + 8) % 8])) {
			offsetIndex++;
		}
		if (offsetIndex < 5) {
			rc.move(directions[(dirint + offsets[offsetIndex] + 8) % 8]);
		}
	}

	// This method will attempt to spawn in the given direction (or as close to
	// it as possible)
	static void trySpawn(Direction d, RobotType type)
			throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = { 0, 1, -1, 2, -2, 3, -3, 4 };
		int dirint = directionToInt(d);
		boolean blocked = false;
		while (offsetIndex < 8
				&& !rc.canSpawn(
						directions[(dirint + offsets[offsetIndex] + 8) % 8],
						type)) {
			offsetIndex++;
		}
		if (offsetIndex < 8) {
			rc.spawn(directions[(dirint + offsets[offsetIndex] + 8) % 8], type);
		}
	}

	// This method will attempt to build in the given direction (or as close to
	// it as possible)
	static void tryBuild(Direction d, RobotType type)
			throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = { 0, 1, -1, 2, -2, 3, -3, 4 };
		int dirint = directionToInt(d);
		boolean blocked = false;
		while (offsetIndex < 8
				&& !rc.canMove(directions[(dirint + offsets[offsetIndex] + 8) % 8])) {
			offsetIndex++;
		}
		if (offsetIndex < 8) {
			rc.build(directions[(dirint + offsets[offsetIndex] + 8) % 8], type);
		}
	}

	private static int targetWeight(int dSquared) {
		if (dSquared > 100) {
			return 5;
		} else if (dSquared > 9) {
			return 2;
		} else {
			return 1;
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
