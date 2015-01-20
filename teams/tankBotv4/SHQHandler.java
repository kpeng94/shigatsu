package tankBotv4;

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
    }

    protected static void execute() throws GameActionException {
        executeStructure();

        // Perform updates every round
        updateTowers();

        // Reset the channel counts for different unit types
        for (int i = countChans.length; --i >= 0;) {
            Count.resetBuffer(countChans[i]);
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
//        System.out.println(Count.getLimit(countChans[0]));
        System.out.println(oreAmount);
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
        if (Count.getCount(Comm.getMinerId()) >= 25) { // Miners >= 25
            Count.setLimit(Comm.getMinerfactId(), 1);
            Count.setLimit(Comm.getMinerId(), 40);
            Count.setLimit(Comm.getBeaverId(), 2);
            Count.setLimit(Comm.getBarrackId(), 1);
            Count.setLimit(Comm.getTankfactId(), 4);
            Count.setLimit(Comm.getTankId(), 999);
            Count.setLimit(Comm.getSupplyId(), 10);
        } else if (Count.getCount(Comm.getTankfactId()) == 1) { // Tank factory
            Count.setLimit(Comm.getMinerId(), 25);
        } else if (Count.getCount(Comm.getMinerId()) >= 10) { // 10 miners
            Count.setLimit(Comm.getTankfactId(), 1);
            Count.setLimit(Comm.getTankId(), 999);
        } else if (Count.getCount(Comm.getMinerfactId()) == 1) { // 1 mining fact
            Count.setLimit(Comm.getBeaverId(), 2);
            Count.setLimit(Comm.getBarrackId(), 1);
            Count.setLimit(Comm.getSoldierId(), 1); // random soldier scout guy
        } else if (Count.getCount(Comm.getBeaverId()) == 1) { // 1 beaver
            Count.setLimit(Comm.getMinerfactId(), 1);
            Count.setLimit(Comm.getMinerId(), 10);
        }

    }

//    protected static void updateUnitCounts() throws GameActionException {
//        int mlx = 0;
//        int mly = 0;
//        myRobots = rc.senseNearbyRobots(999999, myTeam);
//        numSoldiers = numCommanders = numTechInstitutes = numTankFactories = numBashers = numDrones = numComputers = numTrainingFields = numBarracks = numAerospaceLabs = numBeavers = numLaunchers = numMiners = numMinerFactories = numTanks = numSupplyDepots = numHelipads = 0;
//        for (RobotInfo r : myRobots) {
//            RobotType type = r.type;
//            switch (type) {
//                case BEAVER:
//                    numBeavers++;
//                    break;
//                case LAUNCHER:
//                    numLaunchers++;
//                    break;
//                case MINER:
//                    mlx += r.location.x;
//                    mly += r.location.y;
//                    numMiners++;
//                    break;
//                case MINERFACTORY:
//                    numMinerFactories++;
//                    break;
//                case TANK:
//                    numTanks++;
//                    break;
//                case SUPPLYDEPOT:
//                    numSupplyDepots++;
//                    break;
//                case HELIPAD:
//                    numHelipads++;
//                    break;
//                case AEROSPACELAB:
//                    numAerospaceLabs++;
//                    break;
//                case BARRACKS:
//                    numBarracks++;
//                    break;
//                case TANKFACTORY:
//                    numTankFactories++;
//                    break;
//                case TECHNOLOGYINSTITUTE:
//                    numTechInstitutes++;
//                    break;
//                case TRAININGFIELD:
//                    numTrainingFields++;
//                    break;
//                case COMMANDER:
//                    numCommanders++;
//                    break;
//                case SOLDIER:
//                    numSoldiers++;
//                    break;
//                case BASHER:
//                    numBashers++;
//                    break;
//                case DRONE:
//                    numDrones++;
//                    break;
//                case COMPUTER:
//                    numComputers++;
//                    break;
//            }
//        }
//        if (numMiners != 0) {
//            mlx /= numMiners;
//            mly /= numMiners;
//            mlx = (mlx - myHQ.x + 256) % 256;
//            mly = (mly - myHQ.y + 256) % 256;
//            int averagePosOfMiners = mlx * 256 + mly;
//            Comm.writeBlock(Comm.getMinerId(), 2, averagePosOfMiners);
//        }
//
//        // Still Missing some I think
//        Comm.writeBlock(Comm.getHeliId(), Comm.COUNT_OFFSET, numHelipads);
//        Comm.writeBlock(Comm.getBeaverId(), Comm.COUNT_OFFSET, numBeavers);
//        Comm.writeBlock(Comm.getLauncherId(), Comm.COUNT_OFFSET, numLaunchers);
//        Comm.writeBlock(Comm.getMinerId(), Comm.COUNT_OFFSET, numMiners);
//        Comm.writeBlock(Comm.getTankId(), Comm.COUNT_OFFSET, numTanks);
//        Comm.writeBlock(Comm.getMinerfactId(), Comm.COUNT_OFFSET,
//                numMinerFactories);
//        Comm.writeBlock(Comm.getSupplyId(), Comm.COUNT_OFFSET, numSupplyDepots);
//        Comm.writeBlock(Comm.getAeroId(), Comm.COUNT_OFFSET, numAerospaceLabs);
//        Comm.writeBlock(Comm.getBarrackId(), Comm.COUNT_OFFSET, numBarracks);
//        Comm.writeBlock(Comm.getTankfactId(), Comm.COUNT_OFFSET,
//                numTankFactories);
//        Comm.writeBlock(Comm.getCommanderId(), Comm.COUNT_OFFSET, numCommanders);
//        Comm.writeBlock(Comm.getComputerId(), Comm.COUNT_OFFSET, numComputers);
//        Comm.writeBlock(Comm.getDroneId(), Comm.COUNT_OFFSET, numDrones);
//        Comm.writeBlock(Comm.getTrainingId(), Comm.COUNT_OFFSET,
//                numTrainingFields);
//        Comm.writeBlock(Comm.getTechId(), Comm.COUNT_OFFSET, numTechInstitutes);
//    }

    private static void broadcastRallyPoint(int wave)
            throws GameActionException {
        Comm.writeBlock(Comm.getTankId(), Comm.TANK_WAVE_ONE_RALLYPOINT_OFFSET,
                MapUtils.encode(closestTowerToEnemy));

    }

    private static void broadcastRallyPoint(int[] types, int wave) {

    }
}
