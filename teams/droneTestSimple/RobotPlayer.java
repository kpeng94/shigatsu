package droneTestSimple;

import battlecode.common.*;

import java.util.*;

public class RobotPlayer {
	
	// Beaver jobs
	private enum Job {
		MINER, BUILDER, ATTACKER, SCOUT;
	}
	
	// Map for specific jobs
	static IdMap<Job> employment;
		
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
		
		employment = new IdMap<Job>();

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
					rc.setIndicatorString(2, "Team ore count: " + rc.getTeamOre());
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
					rc.broadcast(0, numBeavers);
					rc.broadcast(1, numDrones);
					rc.broadcast(100, numHelipads);

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
					// Obtain a job, or designate one
					Job job = employment.get(rc.getID());
					if (job == null) {
						int fate = rand.nextInt(100);
						if (fate < 80) {
							job = Job.ATTACKER;
						} else {
							job = Job.SCOUT;
						}
						employment.put(rc.getID(), job);
					}
					try {
		                rc.setIndicatorString(2, "I am a " + job);
		            } catch (Exception e) {
		                System.out.println("Unexpected exception");
		                e.printStackTrace();
		            }
					
                    if (rc.isWeaponReady()) {
						attackSomething();
					}
					if (rc.isCoreReady()) {
						int fate = rand.nextInt(1000);
						
						// Retreat - Run away from nearby enemies 
						RobotInfo[] enemies = rc.senseNearbyRobots(myRange / 2, enemyTeam);
						if (enemies.length > 0) {
							MapLocation myLoc = rc.getLocation();
							MapLocation retreatVec = myLoc;
							for (int i = 0; i < enemies.length; i++) {
								retreatVec.add(enemies[i].location.directionTo(myLoc));
							}
							tryMove(myLoc.directionTo(retreatVec));
						}
						else if (job == Job.ATTACKER){
							if (fate < 200) {
								tryMove(directions[rand.nextInt(8)]);
							} else {
								tryMove(rc.getLocation().directionTo(rc.senseEnemyHQLocation()));
							}
						}
						else {
							if (fate < 800) {
								tryMove(directions[rand.nextInt(8)]);
							} else {
								tryMove(rc.getLocation().directionTo(rc.senseEnemyHQLocation()));
							}
						}
						
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
							&& fate < Math.pow(1.2, 15 - numDrones
									+ numBeavers) * 10000) {
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
