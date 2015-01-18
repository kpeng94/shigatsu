package tankBotv4;

import battlecode.common.*;

public class SHQHandler extends StructureHandler {
	public static final RobotType myType = RobotType.HQ;
	public static final int SPLASH_RANGE = 52;
	
	public static MapLocation[] towerLocs;
	public static int towerNum;

	public static int range;
	public static boolean splash;
	public static RobotInfo[] inRangeEnemies;
	
	private static int hqDistX, hqDistY, maxTowerDistX, maxTowerDistY;
	public static RobotInfo[] myRobots;
	static int numBeavers = 0;
	static int numLaunchers = 0;
    static int numMiners = 0;
    static int numMinerFactories = 0;
    static int numTanks = 0;
    static int numSupplyDepots = 0;
    static int numHelipads = 0;
    static int numAerospaceLabs = 0;
	static int numBarracks = 0;
	static int numTankFactories = 0;
	static int numTechInstitutes = 0;
	static int numTrainingFields = 0;
	static int numCommanders = 0;
	static int numSoldiers = 0;
	static int numBashers = 0;
	static int numDrones = 0;
	static int numComputers = 0;
    private static double oreAmount = 0;
	
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
				e.printStackTrace();
				System.out.println(typ + " Execution Exception");
			}
			rc.yield(); // Yields to save remaining bytecodes
		}
	}

	protected static void init(RobotController rcon) throws GameActionException {
		initStructure(rcon);
        rc.broadcast(Comm.HQ_MAP_CHAN, NavBFS.newBFSTask(myHQ));
	}

	protected static void execute() throws GameActionException {
		executeStructure();
		oreAmount = rc.getTeamOre();
		updateTowers();
		updateUnitCounts();
		if(Clock.getRoundNum() % 3 == 0)
		    resetMinerFrontier();
		if (rc.isWeaponReady()) { // Try to attack
			calculateAttackable();
			tryAttack();
		}
		if (rc.isCoreReady()) { // Try to spawn
			if (numBeavers < 1) {
				Spawner.trySpawn(myLoc.directionTo(enemyHQ).opposite(), RobotType.BEAVER, oreAmount);
			}
		}
		RobotInfo[] nearbyUnits = rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, myTeam);
		for (int i = nearbyUnits.length; --i >= 0;) {
			if (nearbyUnits[i].supplyLevel == 0) {
				rc.transferSupplies(2000, nearbyUnits[i].location);
			}
		}
		Supply.spreadSupplies(Supply.DEFAULT_THRESHOLD);
		Distribution.spendBytecodesCalculating(7500);
	}
	
	protected static void updateTowers() {
		towerLocs = rc.senseTowerLocations();
		towerNum = towerLocs.length;
	}
	
	protected static void resetMinerFrontier() throws GameActionException{
	    int frontier = Comm.readBlock(Comm.getMinerId(), UMinerHandler.FRONTIER_OFFSET);
	    if(frontier != 0){
            int priority = frontier >>> 16;
            MapLocation loc = MapUtils.decode(frontier & 0xFFFF);
	    }
	    Comm.writeBlock(Comm.getMinerId(), UMinerHandler.FRONTIER_OFFSET, 0);
	}
	
	protected static void calculateAttackable() {
		if (towerNum >= 5) {
			range = GameConstants.HQ_BUFFED_ATTACK_RADIUS_SQUARED;
			splash = true;
			inRangeEnemies = rc.senseNearbyRobots(SPLASH_RANGE, otherTeam);
		} else {
			range = typ.attackRadiusSquared;
			splash = false;
			inRangeEnemies = rc.senseNearbyRobots(range, otherTeam);
		}
	}
	
	/**
	 * Calculates the best target to attack for the HQ taking the following heuristics 
	 *   into account:
	 *     -Amount of damage done to enemy units
	 *     -HP that the opponents have left [TODO]
	 *     -The rate that the opponents can attack you [TODO]
	 *     -Priority for robots that have large amounts of supply [TODO]
	 * @return
	 */
	public MapLocation calculateBestTarget() throws GameActionException {
		double baseDamage = myType.attackPower;
		if (towerNum >= 6) {
			baseDamage *= GameConstants.HQ_BUFFED_DAMAGE_MULTIPLIER_LEVEL_2;			
		} else if (towerNum >= 3) {
			baseDamage *= GameConstants.HQ_BUFFED_DAMAGE_MULTIPLIER_LEVEL_1;
		}
		double maxDamage = baseDamage;
		RobotInfo[] enemies = rc.senseNearbyRobots(myType.sensorRadiusSquared, otherTeam);
		MapLocation bestLocation = null;
		
		for (RobotInfo enemy : enemies) {
			double damage = baseDamage;
			MapLocation location = enemy.location;
			RobotInfo[] enemiesHitBySplash = rc.senseNearbyRobots(location, GameConstants.HQ_BUFFED_SPLASH_RADIUS_SQUARED, otherTeam);
			damage += enemiesHitBySplash.length * GameConstants.HQ_BUFFED_SPLASH_RATE * baseDamage;
			if (damage >= maxDamage) {
				maxDamage = damage;
				bestLocation = location;
			}
		}
		
		return bestLocation;
	}
	
	protected static void tryAttack() throws GameActionException {
		if (inRangeEnemies.length > 0) {
			MapLocation minLoc = inRangeEnemies[0].location;
			int minRange = myLoc.distanceSquaredTo(minLoc);
			for (int i = inRangeEnemies.length; --i >= 0;) { // Get minimum in array
				RobotInfo enemy = inRangeEnemies[i];
				MapLocation enemyLoc = enemy.location;
				int enemyRange = myLoc.distanceSquaredTo(enemyLoc);
				if (enemyRange < minRange) {
					minRange = enemyRange;
					minLoc = enemyLoc;
				}
			}
			
			if (minRange < range) { // Splash damage calculations
				rc.attackLocation(minLoc);
			} else {
				int dx = minLoc.x - myLoc.x;
				int dy = minLoc.y - myLoc.y;
				MapLocation newLoc = new MapLocation(minLoc.x - ((dx == 0) ? 0 : ((dx > 0) ? 1 : -1)), minLoc.y - ((dy == 0) ? 0 : ((dy > 0) ? 1 : -1)));
				if (myLoc.distanceSquaredTo(newLoc) < range) {
					rc.attackLocation(newLoc);
				}
			}
		}
	}

	
	public static void calculateLooseBoundsOnMap() {
		hqDistX = myHQ.x > enemyHQ.x ? (myHQ.x - enemyHQ.x) : (enemyHQ.x - myHQ.x);
		hqDistY = myHQ.y > enemyHQ.y ? (myHQ.y - enemyHQ.y) : (enemyHQ.y - myHQ.y);
		MapLocation[] towerLocations = rc.senseTowerLocations();
		MapLocation[] enemyTowerLocations = rc.senseEnemyTowerLocations();
		int minTowerX = Integer.MAX_VALUE;
		int minTowerY = Integer.MAX_VALUE;
		int maxTowerX = Integer.MIN_VALUE;
		int maxTowerY = Integer.MIN_VALUE;
		int minETowerX = Integer.MAX_VALUE;
		int minETowerY = Integer.MAX_VALUE;
		int maxETowerX = Integer.MIN_VALUE;
		int maxETowerY = Integer.MIN_VALUE;
		for (MapLocation towerLocation : towerLocations) {
			if (towerLocation.x < minTowerX) {
				minTowerX = towerLocation.x;				
			}
			if (towerLocation.x > maxTowerX) {
				maxTowerX = towerLocation.x;
			}
			if (towerLocation.y < minTowerY) {
				minTowerY = towerLocation.y;
			}
			if (towerLocation.y > maxTowerY) {
				maxTowerY = towerLocation.y;
			}
		}
		for (MapLocation towerLocation : enemyTowerLocations) {
			if (towerLocation.x < minETowerX) {
				minETowerX = towerLocation.x;				
			}
			if (towerLocation.x > maxETowerX) {
				maxETowerX = towerLocation.x;
			}
			if (towerLocation.y < minETowerY) {
				minETowerY = towerLocation.y;
			}
			if (towerLocation.y > maxETowerY) {
				maxETowerY = towerLocation.y;
			}
		}
		
//		The width of the map must be at least as big as the distance between the largest X and smallest X of opposing sides
        int dx1 = maxTowerX - minETowerX;
        int dx2 = maxETowerX - minETowerX;
        maxTowerDistX = (dx1 - dx2 > 0) ? dx1 : dx2;
        int dy1 = maxTowerY - minETowerY;
        int dy2 = maxETowerY - minETowerY;
        maxTowerDistY = (dy1 - dy2 > 0) ? dy1 : dy2;
	}	
	
	protected static void updateUnitCounts() throws GameActionException {
		int mlx = 0;
		int mly = 0;
		myRobots = rc.senseNearbyRobots(999999, myTeam);
		numSoldiers = numCommanders = numTechInstitutes = numTankFactories = numBashers 
					= numDrones = numComputers = numTrainingFields = numBarracks 
					= numAerospaceLabs = numBeavers = numLaunchers = numMiners 
					= numMinerFactories = numTanks = numSupplyDepots = numHelipads = 0;
		for (RobotInfo r : myRobots) {
			RobotType type = r.type;
			switch (type) {
				case BEAVER:
					numBeavers++;
					break;
				case LAUNCHER:
					numLaunchers++;
					break;
				case MINER:
					mlx += r.location.x;
					mly += r.location.y;
					numMiners++;
					break;
				case MINERFACTORY:
					numMinerFactories++;
					break;
				case TANK:
					numTanks++;
					break;
				case SUPPLYDEPOT:
					numSupplyDepots++;
					break;
				case HELIPAD:
					numHelipads++;
					break;
				case AEROSPACELAB:
					numAerospaceLabs++;
					break;
				case BARRACKS:
					numBarracks ++;
					break;
				case TANKFACTORY:
					numTankFactories++;
					break;
				case TECHNOLOGYINSTITUTE:
					numTechInstitutes++;
					break;
				case TRAININGFIELD:
					numTrainingFields++;
					break;
				case COMMANDER:
					numCommanders++;
					break;
				case SOLDIER:
					numSoldiers++;
					break;
				case BASHER:
					numBashers++;
					break;
				case DRONE:
					numDrones++;
					break;
				case COMPUTER:
					numComputers++;
					break;
			}
		}
		if (numMiners != 0) {
			mlx /= numMiners;
			mly /= numMiners;
			mlx = (mlx - myHQ.x + 256) % 256;
			mly = (mly - myHQ.y + 256) % 256;
			int averagePosOfMiners = mlx * 256 + mly;
			Comm.writeBlock(Comm.getMinerId(), 2, averagePosOfMiners);			
		}
		
		// Still Missing some I think
		Comm.writeBlock(Comm.getHeliId(), Comm.COUNT_OFFSET, numHelipads);
		Comm.writeBlock(Comm.getBeaverId(), Comm.COUNT_OFFSET, numBeavers);
		Comm.writeBlock(Comm.getLauncherId(), Comm.COUNT_OFFSET, numLaunchers);
		Comm.writeBlock(Comm.getMinerId(), Comm.COUNT_OFFSET, numMiners);
		Comm.writeBlock(Comm.getTankId(), Comm.COUNT_OFFSET, numTanks);
		Comm.writeBlock(Comm.getMinerfactId(), Comm.COUNT_OFFSET, numMinerFactories);
		Comm.writeBlock(Comm.getSupplyId(), Comm.COUNT_OFFSET, numSupplyDepots);
		Comm.writeBlock(Comm.getAeroId(), Comm.COUNT_OFFSET, numAerospaceLabs);
		Comm.writeBlock(Comm.getBarrackId(), Comm.COUNT_OFFSET, numBarracks);
		Comm.writeBlock(Comm.getTankfactId(), Comm.COUNT_OFFSET, numTankFactories);
		Comm.writeBlock(Comm.getCommanderId(), Comm.COUNT_OFFSET, numCommanders);
		Comm.writeBlock(Comm.getComputerId(), Comm.COUNT_OFFSET, numComputers);
		Comm.writeBlock(Comm.getDroneId(), Comm.COUNT_OFFSET, numDrones);
		Comm.writeBlock(Comm.getTrainingId(), Comm.COUNT_OFFSET, numTrainingFields);
		Comm.writeBlock(Comm.getTechId(), Comm.COUNT_OFFSET, numTechInstitutes);
	}	
}
