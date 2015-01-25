package commanderBot;

import battlecode.common.*;

public class UTankHandler extends UnitHandler {
	public static final int ATTACK_THRESHOLD = 10;
	public static MapLocation rallyPoint = null;
	
	private static boolean isAttacking;
	private static boolean newRallyFound;

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
		myLoc = rc.getLocation();
		
		MapLocation[] towers = rc.senseTowerLocations();
		Direction toEnemyHQ = myLoc.directionTo(enemyHQ);
		int maxDist = 0;
		
		for (int i = towers.length; --i >= 0;) {
			int dist = myLoc.distanceSquaredTo(towers[i]);
			if (myLoc.directionTo(towers[i]).equals(toEnemyHQ) && dist > maxDist) { // tower towards enemy
				rallyPoint = towers[i];
				maxDist = dist;
			}
		}
		
		if (rallyPoint == null) {
			rallyPoint = MapUtils.pointSection(myHQ, enemyHQ, 0.75);
		}
		
		isAttacking = false;
		newRallyFound = false;
	}

	protected static void execute() throws GameActionException {
		executeUnit();
		Count.incrementBuffer(Comm.getTankId());
		
		rc.setIndicatorString(0, "" + rallyPoint);
		
		if (Clock.getRoundNum() >= 1800) {
			isAttacking = true;
		} else {
			if (myLoc.distanceSquaredTo(rallyPoint) <= 24) {
				if (Clock.getRoundNum() >= 1500) {
					isAttacking = true;
				} else {
					int tankCount = 0;
					RobotInfo[] aroundRally = rc.senseNearbyRobots(rallyPoint, 24, myTeam);
					if (aroundRally.length >= ATTACK_THRESHOLD) {
						for (int i = aroundRally.length; --i >= 0;) {
							if (aroundRally[i].type == RobotType.TANK) {
								tankCount++;
							}
						}
						if (tankCount >= ATTACK_THRESHOLD) {
							isAttacking = true;
						}
					}
				}
			}
		}
		
		RobotInfo[] enemies = rc.senseNearbyRobots(typ.attackRadiusSquared, otherTeam);
		
		if (isAttacking) {
			if (rc.isWeaponReady()) {
				Attack.tryAttackPrioritizeTowers();
			}
			if (rc.isCoreReady() && enemies.length == 0) {
				if (enemyTowers.length > 0) {
					MapLocation towerDest = Attack.getClosestTower();
					NavTangentBug.setDest(towerDest);
					NavTangentBug.calculate(2500);
					Direction dir = NavTangentBug.getNextMove();
					if (dir != Direction.NONE) {
						NavSimple.walkTowards(dir);
					}
				} else {
					NavTangentBug.setDest(enemyHQ);
					NavTangentBug.calculate(2500);
					Direction dir = NavTangentBug.getNextMove();
					if (dir != Direction.NONE) {
						NavSimple.walkTowards(dir);
					}
				}
			}
		} else {
			if (!newRallyFound) {
				int dest = rc.readBroadcast(Comm.TANK_RALLY_DEST_CHAN);
				if (dest > 0) { // new rally destination
					rallyPoint = MapUtils.decode(dest);
					newRallyFound = true;
				} else if (myLoc.distanceSquaredTo(rallyPoint) <= 8) {
					rallyPoint = myLoc;
					newRallyFound = true;
					rc.broadcast(Comm.TANK_RALLY_MAP_CHAN, NavBFS.newBFSTask(myLoc));
					rc.broadcast(Comm.TANK_RALLY_DEST_CHAN, MapUtils.encode(myLoc));
				}
			}
			if (rc.isWeaponReady()) {
				Attack.tryAttackClosestButKillIfPossible(enemies);
			}
			if (rc.isCoreReady() && enemies.length == 0) {
				if (newRallyFound) {
					int curPosInfo = NavBFS.readMapDataUncached(rc.readBroadcast(Comm.TANK_RALLY_MAP_CHAN), MapUtils.encode(myLoc));
					if (curPosInfo == 0) {
						NavTangentBug.setDest(rallyPoint);
						NavTangentBug.calculate(2500);
						Direction dir = NavTangentBug.getNextMove();
						if (dir != Direction.NONE) {
							NavSimple.walkTowards(dir);
						}
					} else {
						NavSimple.walkTowardsDirected(MapUtils.dirs[curPosInfo & 0x00000007]);
					}
				} else {
					NavTangentBug.setDest(rallyPoint);
					NavTangentBug.calculate(2500);
					Direction dir = NavTangentBug.getNextMove();
					if (dir != Direction.NONE) {
						NavSimple.walkTowards(dir);
					}
				}
			}
		}

		Supply.spreadSupplies(Supply.DEFAULT_THRESHOLD);
	}
	
}
