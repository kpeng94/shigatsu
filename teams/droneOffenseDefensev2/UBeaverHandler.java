package droneOffenseDefensev2;

import battlecode.common.*;

public class UBeaverHandler extends UnitHandler {
	private static RobotInfo[] inRangeEnemies;
	
	private static int numFactories;
	private static int numHelipads;
	private static int numSpacesCanMove;

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

	protected static void init(RobotController rcon) {
		initUnit(rcon);
	}

	protected static void execute() throws GameActionException {
		executeUnit();
		updateCounts();
		numSpacesCanMove = 0;
		for (Direction d: MapUtils.dirs) {
			if (rc.canMove(d)) {
				numSpacesCanMove++;
			}
		}
		
		if (rc.isWeaponReady()) {
			inRangeEnemies = rc.senseNearbyRobots(typ.attackRadiusSquared,
					otherTeam);
			tryAttack();
		}
		if (rc.isCoreReady()) {
			act();
		}
	}

	protected static void tryAttack() throws GameActionException {
		if (inRangeEnemies.length > 0) {
			MapLocation minLoc = inRangeEnemies[0].location;
			int minRange = myLoc.distanceSquaredTo(minLoc);
			for (int i = inRangeEnemies.length - 1; i > 0; i--) { // Get minimum
																	// in array
				RobotInfo enemy = inRangeEnemies[i];
				MapLocation enemyLoc = enemy.location;
				int enemyRange = myLoc.distanceSquaredTo(enemyLoc);
				if (enemyRange < minRange) {
					minRange = enemyRange;
					minLoc = enemyLoc;
				}
			}

			rc.attackLocation(minLoc);
		}
	}
	
	protected static void updateCounts() throws GameActionException {
		numHelipads = Comm.readBlock(Comm.getHeliId(), 0);
		numFactories = Comm.readBlock(Comm.getMinerfactId(), 0);
	}
	
	protected static void act() throws GameActionException {
		double oreAmount = rc.getTeamOre();
		// Prioritize miner factories
		if (numFactories < MINERFACTORY_THRESHOLD) {
			if (myLoc.distanceSquaredTo(myHQ) <= 3 || numSpacesCanMove <= 4 || !canBuildWithGap()) {
				NavSimple.walkTowards(myLoc.directionTo(enemyHQ));
			} else if (oreAmount >= RobotType.MINERFACTORY.oreCost) {
				buildWithGap(RobotType.MINERFACTORY);
			} else if (oreAmount <= RobotType.MINERFACTORY.oreCost - GameConstants.HQ_ORE_INCOME * RobotType.BEAVER.movementDelay) {
				if (rc.senseOre(myLoc) >= ORE_THRESHOLD && rc.canMine()) {
					rc.mine();
				} else {
					NavSimple.walkRandom();
				}
			}
		} else if (numHelipads < HELIPAD_THRESHOLD) {
			if (myLoc.distanceSquaredTo(myHQ) <= 3 || numSpacesCanMove <= 4 || !canBuildWithGap()) {
				rc.setIndicatorString(0, "Moving sq");
				NavSimple.walkTowards(myLoc.directionTo(enemyHQ));
			} else if (oreAmount >= RobotType.HELIPAD.oreCost) {
				buildWithGap(RobotType.HELIPAD);
			} else if (oreAmount <= RobotType.HELIPAD.oreCost - GameConstants.HQ_ORE_INCOME * RobotType.BEAVER.movementDelay) {
				rc.setIndicatorString(0, "Moving or");
				if (rc.senseOre(myLoc) >= ORE_THRESHOLD && rc.canMine()) {
					rc.mine();
				} else {
					NavSimple.walkRandom();
				}
			}
		}
	}
	
	private static boolean canBuildWithGap() throws GameActionException {
		for (Direction d: MapUtils.dirs) {
			MapLocation site = myLoc.add(d);
			boolean canBuild = rc.senseRobotAtLocation(site) == null;
			for (Direction d2: MapUtils.dirs) {
				RobotInfo info = rc.senseRobotAtLocation(site.add(d2));
				if (info != null && Handler.isBuilding(info)) {
					canBuild = false;
				}
			}
			
			if (canBuild) {
				return true;
			}
		}
		return false;
	}
	
	private static void buildWithGap(RobotType type) throws GameActionException {
		for (Direction d: MapUtils.dirs) {
			MapLocation site = myLoc.add(d);
			boolean canBuild = rc.senseRobotAtLocation(site) == null;
			for (Direction d2: MapUtils.dirs) {
				RobotInfo info = rc.senseRobotAtLocation(site.add(d2));
				if (info != null && Handler.isBuilding(info)) {
					canBuild = false;
				}
			}
			
			if (canBuild) {
				Spawner.tryBuild(d, type);
				return;
			}
		}
	}
}
