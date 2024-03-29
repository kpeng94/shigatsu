package ryoutabot;

import battlecode.common.*;

public class SHQHandler extends StructureHandler {
    public static final int SPLASH_RANGE = 52;

    public static MapLocation[] towerLocs;
    public static int towerNum;

    public static int range;
    public static boolean splash;
    public static RobotInfo[] inRangeEnemies;

    private static int hqDistX, hqDistY, maxTowerDistX, maxTowerDistY;
    public static RobotInfo[] myRobots;
    public static int numBeavers = 0;
    public static int numLaunchers = 0;
    public static int numMiners = 0;
    public static int numMinerFactories = 0;
    public static int numTanks = 0;
    public static int numSupplyDepots = 0;
    public static int numHelipads = 0;
    public static int numAerospaceLabs = 0;
    public static int numBarracks = 0;
    public static int numTankFactories = 0;
    public static int numTechInstitutes = 0;
    public static int numTrainingFields = 0;
    public static int numCommanders = 0;
    public static int numSoldiers = 0;
    public static int numBashers = 0;
    public static int numDrones = 0;
    public static int numComputers = 0;
    public static MapLocation closestTowerToEnemy = null;

    private static int[] countChans;

    private static int currentTankWave = 1;

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
        typ = RobotType.HQ;
        rc.broadcast(Comm.HQ_MAP_CHAN, NavBFS.newBFSTask(myHQ));
        countChans = new int[]{Comm.getBeaverId(), Comm.getMinerId(), Comm.getTankId(),
                Comm.getMinerfactId(), Comm.getBarrackId(), Comm.getTankfactId(), Comm.getSupplyId()};

        initCounts();
        Count.setLimit(Comm.getBeaverId(), Constants.NUM_OF_BEAVERS);
        // Broadcast tank wave
        Comm.writeBlock(Comm.getTankId(), Comm.WAVENUM_OFFSET, currentTankWave);
    }

    protected static void execute() throws GameActionException {
        executeStructure();

        // Perform updates every round
        updateTowers();

        // Reset the channel counts for different unit types
        for (int i = countChans.length; --i >= 0;) {
            Count.resetBuffer(countChans[i]);
        }

        // Reset counts for tank waves
        for (int i = currentTankWave; --i >= 0;) {
            Count.resetBufferForGroup(Comm.getTankId(), i + 1);
        }

        Utils.updateOre();

        broadcastRallyPoint(1);
        if (rc.isWeaponReady()) { // Try to attack
            calculateAttackable();
            tryAttack();
        }
        if (rc.isCoreReady()) { // Try to spawn
            trySpawn();
        }
        updateBuildStates();

        RobotInfo[] nearbyUnits = rc.senseNearbyRobots(
                GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, myTeam);
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
        int smallestDistance = Integer.MAX_VALUE;
        if (towerNum == 0)
            smallestDistance = myHQ.distanceSquaredTo(enemyHQ);
        for (int i = towerNum; --i >= 0;) {
            int distanceFromTowerToEnemyHQ = towerLocs[i]
                    .distanceSquaredTo(enemyHQ);
            if (distanceFromTowerToEnemyHQ < smallestDistance) {
                smallestDistance = distanceFromTowerToEnemyHQ;
                closestTowerToEnemy = towerLocs[i];
            }
        }
    }

    /**
     * Try to spawn a beaver whenever the number of beavers is too small.
     * @throws GameActionException
     */
    protected static void trySpawn() throws GameActionException {
        if (Count.getCount(countChans[0]) < Count.getLimit(countChans[0])) {
            Spawner.trySpawn(myLoc.directionTo(enemyHQ).opposite(), RobotType.BEAVER, countChans[0]);
        }
    }

    protected static void initCounts() throws GameActionException {
        int towers = rc.senseTowerLocations().length;
        Comm.writeBlock(Comm.getHqId(), Count.COUNT_BUFFER_START, 1);
        Comm.writeBlock(Comm.getTowerId(), Count.COUNT_BUFFER_START, towers);
    }

    protected static void updateWaveCount() throws GameActionException {
        int waveOneCount = Comm.readBlock(Comm.getTankId(),
                Comm.TANK_WAVE_ONE_ACTION_OFFSET);
        Comm.writeBlock(Comm.getTankId(), Comm.TANK_WAVE_ONE_ACTION_OFFSET, 0);
        int waveTwoCount = Comm.readBlock(Comm.getTankId(),
                Comm.TANK_WAVE_TWO_ACTION_OFFSET);
        Comm.writeBlock(Comm.getTankId(), Comm.TANK_WAVE_TWO_ACTION_OFFSET, 0);
        int waveThreeCount = Comm.readBlock(Comm.getTankId(),
                Comm.TANK_WAVE_THREE_ACTION_OFFSET);
        Comm.writeBlock(Comm.getTankId(), Comm.TANK_WAVE_THREE_ACTION_OFFSET, 0);
    }

    protected static void calculateAttackable() {
        if (towerNum >= 5) {
            range = GameConstants.HQ_BUFFED_ATTACK_RADIUS_SQUARED;
            splash = true;
            inRangeEnemies = rc.senseNearbyRobots(SPLASH_RANGE, otherTeam);
        } else if (towerNum >= 2) {
            range = GameConstants.HQ_BUFFED_ATTACK_RADIUS_SQUARED;
            splash = false;
            inRangeEnemies = rc.senseNearbyRobots(range, otherTeam);
        } else {
            range = typ.attackRadiusSquared;
            splash = false;
            inRangeEnemies = rc.senseNearbyRobots(range, otherTeam);
        }
    }

    /**
     * Calculates the best target to attack for the HQ taking the following
     * heuristics into account:
     * -Amount of damage done to enemy units
     * -HP that
     * the opponents have left [TODO]
     * -The rate that the opponents can attack
     * you [TODO]
     * -Priority for robots that have large amounts of supply [TODO]
     *
     * @return
     */
    public MapLocation calculateBestTarget() throws GameActionException {
        double baseDamage = typ.attackPower;
        if (towerNum >= 6) {
            baseDamage *= GameConstants.HQ_BUFFED_DAMAGE_MULTIPLIER_LEVEL_2;
        } else if (towerNum >= 3) {
            baseDamage *= GameConstants.HQ_BUFFED_DAMAGE_MULTIPLIER_LEVEL_1;
        }
        double maxDamage = baseDamage;
        RobotInfo[] enemies = rc.senseNearbyRobots(typ.sensorRadiusSquared,
                otherTeam);
        MapLocation bestLocation = null;

        for (RobotInfo enemy : enemies) {
            double damage = baseDamage;
            MapLocation location = enemy.location;
            RobotInfo[] enemiesHitBySplash = rc.senseNearbyRobots(location,
                    GameConstants.HQ_BUFFED_SPLASH_RADIUS_SQUARED, otherTeam);
            damage += enemiesHitBySplash.length
                    * GameConstants.HQ_BUFFED_SPLASH_RATE * baseDamage;
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
                MapLocation newLoc = minLoc.add(minLoc.directionTo(myLoc));
                if (myLoc.distanceSquaredTo(newLoc) < range) {
                    rc.attackLocation(newLoc);
                }
            }
        }
    }

    protected static void updateBuildStates() throws GameActionException {
        if (Count.getCount(Comm.getTankId()) >= 25) { // Tanks >= 25
            Count.setLimit(Comm.getSupplyId(), 30);
        }
        if (Count.getCount(Comm.getMinerId()) >= 15) { // Miners >= 25
            Count.setLimit(Comm.getMinerfactId(), 1);
            Count.setLimit(Comm.getMinerId(), 20);
            Count.setLimit(Comm.getTankfactId(), 5);
            Count.setLimit(Comm.getSupplyId(), 10);
            Count.setLimit(Comm.getTankId(), 999);
        } else if (Count.getCount(Comm.getTankfactId()) == 1) { // Tank factory
            Count.setLimit(Comm.getMinerId(), 15);
            Count.setLimit(Comm.getTankfactId(), 3);
            Count.setLimit(Comm.getSupplyId(), 2);
            Count.setLimit(Comm.getTankId(), 999);
        } else if (Count.getCount(Comm.getTrainingId()) == 1) {
            Count.setLimit(Comm.getCommanderId(), 1);
            Count.setLimit(Comm.getTankId(), 999);
            Count.setLimit(Comm.getTankfactId(), 1);
        } else if (Count.getCount(Comm.getTechId()) == 1) {
            Count.setLimit(Comm.getTrainingId(), 1);
        } else if (Count.getCount(Comm.getBarrackId()) == 1) {
            Count.setLimit(Comm.getSoldierId(), 1);
            Count.setLimit(Comm.getTankfactId(), 1);
        } else if (Count.getCount(Comm.getMinerfactId()) == 1) {
            // When we have a mining factory, prioritize building a technology institute
            Count.setLimit(Comm.getTechId(), 1);
            Count.setLimit(Comm.getBeaverId(), 2);
            Count.setLimit(Comm.getBarrackId(), 1);
        } else if (Count.getCount(Comm.getBeaverId()) == 1) {
            // When we have only one beaver (this is early game most likely), prioritize building
            // a mining factory and miners, so we can gather some resources.
            Count.setLimit(Comm.getMinerfactId(), 1);
            Count.setLimit(Comm.getMinerId(), 15);
            Count.setLimit(Comm.getSupplyId(), 3);
        }

//        else if (Count.getCount(Comm.getMinerId()) >= 5) { // 10 miners
//            Count.setLimit(Comm.getTankfactId(), 1);
//        }

        // If there are enough tanks at the rally point, increment the wave count
        // The tanks should detect this by themselves and move
        if (Count.getCountAtRallyPoint(Comm.getTankId(), currentTankWave) >= Constants.TANK_RUSH_COUNT) {
            currentTankWave++;
            Count.incrementWaveNum(Comm.getTankId());
        }
    }

    private static void broadcastRallyPoint(int wave)
            throws GameActionException {
        Comm.writeBlock(Comm.getTankId(), Comm.TANK_WAVE_ONE_RALLYPOINT_OFFSET,
                MapUtils.encode(closestTowerToEnemy));

    }
}
