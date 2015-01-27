package qualifyingBotv3;

import battlecode.common.*;

public class SHQHandler extends StructureHandler {
    public static final int SPLASH_RANGE = 52;

    public static MapLocation[] towerLocs;
    public static int towerNum;

    public static int range;
    public static boolean splash;
    public static RobotInfo[] inRangeEnemies;

    private static int prevOre = 0;

    private static int[] countChans;

    private static int currentLauncherWave = 1;

    private static boolean seenTanks = false;
    private static boolean canSurroundHQ;

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
        initStructure(rcon);
        rc.broadcast(Comm.HQ_MAP_CHAN, NavBFS.newBFSTask(myHQ));

        prevOre = GameConstants.ORE_INITIAL_AMOUNT;

        countChans = new int[] { Comm.getBeaverId(), Comm.getMinerId(), Comm.getLauncherId(), Comm.getSoldierId(), Comm.getCommanderId(), Comm.getComputerId(), Comm.getDroneId(), Comm.getMinerfactId(), Comm.getBarrackId(), Comm.getHeliId(), Comm.getAeroId(), Comm.getTechId(), Comm.getTrainingId(), Comm.getSupplyId() };

        initCounts();
        enemyTowers = rc.senseEnemyTowerLocations();
        canSurroundHQ = checkSurroundability();

        Count.setLimit(Comm.getBeaverId(), 1); // Maintain 1 beaver
        Count.setLimit(Comm.getDroneId(), 1); // Maintain 1 drone
        Comm.writeBlock(Comm.getLauncherId(), Comm.WAVENUM_OFFSET, currentLauncherWave);
        rc.broadcast(Comm.FINAL_PUSH_ROUND_CHAN, rc.getRoundLimit() * 7 / 8);
        seenTanks = (((int) rc.getTeamMemory()[0]) == 1);
    }

    protected static void execute() throws GameActionException {
        executeStructure();
        updateTowers();
        
        for (int i = countChans.length; --i >= 0;) {
            Count.resetBuffer(countChans[i]);
        }
        // Reset counts for tank waves
        for (int i = currentLauncherWave; --i >= 0;) {
            Count.resetBufferForGroup(Comm.getLauncherId(), i + 1);
        }

        updateOre();
        Count.incrementBuffer(Comm.getHqId());

        if (rc.isWeaponReady()) { // Try to attack
            calculateAttackable();
            tryAttack();
        }

        if (rc.isCoreReady()) { // Try to spawn
            trySpawn();
        }
        updateBuildStates();
        updateLimits();
        updateOreCounts();

        rc.broadcast(Comm.COMMANDER_RALLY_DEST_CHAN, MapUtils.encode(enemyHQ));

        RobotInfo[] nearbyRobots = Handler.rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, Handler.myTeam);

        // Supply.spreadSupplies(Supply.DEFAULT_THRESHOLD, nearbyRobots);
        Supply.distributeHQSupply();
        Supply.incExpiration();

        for (int i = nearbyRobots.length; --i >= 0;) {
            RobotInfo robot = nearbyRobots[i];
            if (robot.type == RobotType.COMMANDER) {
                int neededSupply = (int) (20 * (rc.getRoundLimit() - Clock.getRoundNum()) - robot.supplyLevel);
                if (neededSupply > 0) {
                    rc.transferSupplies((int) (rc.getSupplyLevel() > neededSupply ? neededSupply : rc.getSupplyLevel()), robot.location);
                }
            }
        }
        Distribution.spendBytecodesCalculating(7500);
    }

    private static final int SURROUND_RANGE = 97;

    protected static boolean checkSurroundability() {
        int singletons = 0;
        for (int j = enemyTowers.length; --j >= 0;) {
            int currentClose = 0;
            if(enemyTowers[j].distanceSquaredTo(enemyHQ) <= SURROUND_RANGE)
                currentClose++;
            for (int i = enemyTowers.length; --i >= 0;) {
                if (currentClose == 2) {
                    break;
                }
                if (!enemyTowers[i].equals(enemyTowers[j]) && enemyTowers[i].distanceSquaredTo(enemyTowers[j]) <= SURROUND_RANGE)
                    currentClose++;
            }
            if (currentClose == 1)
                singletons++;
            if(currentClose == 0)
                return false;
        }
        if(singletons <= 2)
            return true;
        return false;
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

    protected static void tryAttack() throws GameActionException {
        if (inRangeEnemies.length > 0) {
            MapLocation minLoc = inRangeEnemies[0].location;
            int minRange = myLoc.distanceSquaredTo(minLoc);
            for (int i = inRangeEnemies.length; --i >= 0;) { // Get minimum in
                                                             // array
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

    protected static void trySpawn() throws GameActionException {
        if (Count.getCount(countChans[0]) < Count.getLimit(countChans[0])) {
            Spawner.trySpawn(myLoc.directionTo(enemyHQ).opposite(), RobotType.BEAVER, countChans[0]);
        }
    }

    protected static void updateTowers() {
        towerLocs = rc.senseTowerLocations();
        towerNum = towerLocs.length;
    }

    protected static void initCounts() throws GameActionException {
        int towers = rc.senseTowerLocations().length;
        Comm.writeBlock(Comm.getHqId(), Count.COUNT_BUFFER_START, 1);
        Comm.writeBlock(Comm.getTowerId(), Count.COUNT_BUFFER_START, towers);
    }

    protected static void updateOre() throws GameActionException {
        int spent = rc.readBroadcast(Comm.SPENT_ORE_BUFFER_CHAN);
        int gained = (int) (rc.getTeamOre() - prevOre + spent);
        prevOre = (int) rc.getTeamOre();
        rc.broadcast(Comm.SPENT_ORE_BUFFER_CHAN, 0);
        rc.broadcast(Comm.PREV_ORE_CHAN, prevOre);
        rc.broadcast(Comm.SPENT_ORE_CHAN, spent);
        rc.broadcast(Comm.GAINED_ORE_CHAN, gained);
    }

    protected static void updateBuildStates() throws GameActionException {
        if (!seenTanks && !canSurroundHQ) {
            if (Count.getCount(Comm.getLauncherId()) >= 1) {
                Count.setLimit(Comm.getSupplyId(), 10 * Count.getLimit(Comm.getAeroId()));
            } else if (Count.getCount(Comm.getSoldierId()) >= 10) {
                Count.setLimit(Comm.getLauncherId(), 999);
                Count.setLimit(Comm.getSoldierId(), 30);
            } else if (Count.getCount(Comm.getMinerId()) >= 10) { // 10 miners
                Count.setLimit(Comm.getHeliId(), 1);
                Count.setLimit(Comm.getAeroId(), 1);
                Count.setLimit(Comm.getMinerId(), 30);
                Count.setLimit(Comm.getLauncherId(), 999);
                Count.setLimit(Comm.getSoldierId(), 10);
            } else if (Count.getCount(Comm.getMinerfactId()) == 1) { // 1 mining
                                                                     // fact
                Count.setLimit(Comm.getBeaverId(), 2);
                Count.setLimit(Comm.getBarrackId(), 1);
                Count.setLimit(Comm.getSoldierId(), 5);
                Count.setLimit(Comm.getTechId(), 1);
                Count.setLimit(Comm.getTrainingId(), 1);
                Count.setLimit(Comm.getCommanderId(), 1);
            } else if (Count.getCount(Comm.getBeaverId()) == 1) { // 1 beaver
                Count.setLimit(Comm.getMinerfactId(), 1);
                Count.setLimit(Comm.getMinerId(), 10);
            }
        } else {
            if (Count.getCount(Comm.getLauncherId()) >= 1) {
                Count.setLimit(Comm.getSupplyId(), 10 * Count.getLimit(Comm.getAeroId()));
                // } else if (Count.getCount(Comm.getSoldierId()) >= 10) {
                // Count.setLimit(Comm.getLauncherId(), 999);
                // Count.setLimit(Comm.getSoldierId(), 50);
            } else if (Count.getCount(Comm.getMinerId()) >= 10) { // 10 miners
                Count.setLimit(Comm.getHeliId(), 1);
                Count.setLimit(Comm.getAeroId(), 1);
                Count.setLimit(Comm.getMinerId(), 30);
                Count.setLimit(Comm.getLauncherId(), 999);
                // Count.setLimit(Comm.getSoldierId(), 20);
            } else if (Count.getCount(Comm.getMinerfactId()) == 1) { // 1 mining
                                                                     // fact
                Count.setLimit(Comm.getBeaverId(), 2);
                // Count.setLimit(Comm.getBarrackId(), 1);
                // Count.setLimit(Comm.getSoldierId(), 5);
                Count.setLimit(Comm.getTechId(), 1);
                Count.setLimit(Comm.getTrainingId(), 1);
                Count.setLimit(Comm.getCommanderId(), 1);
            } else if (Count.getCount(Comm.getBeaverId()) == 1) { // 1 beaver
                Count.setLimit(Comm.getMinerfactId(), 1);
                Count.setLimit(Comm.getMinerId(), 10);
            }
        }

        if (rc.getRoundLimit() - Clock.getRoundNum() < 250) {
            Count.setLimit(Comm.getHandwashId(), 1);
        }

        if (Count.getCountAtRallyPoint(Comm.getLauncherId(), currentLauncherWave) >= ULauncherHandler.LAUNCHER_RUSH_COUNT) {
            currentLauncherWave++;
            Count.incrementWaveNum(Comm.getLauncherId());
        }
    }

    private static final int TURN_COUNTER = 100;
    private static double[] oreCounts = new double[TURN_COUNTER];
    private static final int ORE_THRESHOLD = 1000;
    private static int turnCooldown = 0;

    /**
     * Returns the ore delta since TURN_COUNTER turns ago. Returns -999999 if
     * the ore TURN_COUNTER turns ago was 0 (or not enough turns have passed).
     * 
     * @return
     */
    private static double getOreChangeInLastFewTurns() {
        int oreCountIndex = Clock.getRoundNum() % TURN_COUNTER;
        double prevOre = oreCounts[oreCountIndex];
        if (oreCounts[oreCountIndex] != 0) {
            return rc.getTeamOre() - prevOre;
        }
        return -999999;
    }

    private static void updateOreCounts() {
        turnCooldown++;
        int index = Clock.getRoundNum() % TURN_COUNTER;
        oreCounts[index] = rc.getTeamOre();
    }

    protected static void updateLimits() throws GameActionException {
        if (turnCooldown >= TURN_COUNTER && rc.getTeamOre() > ORE_THRESHOLD && getOreChangeInLastFewTurns() >= 0) {
            int barrackCount = Count.getCount(Comm.getBarrackId());
            if (barrackCount >= 1 && barrackCount >= Count.getLimit(Comm.getBarrackId()) && barrackCount < 2) {
                Count.setLimit(Comm.getBarrackId(), barrackCount + 1);
                turnCooldown = 0;
            }
            int aeroCount = Count.getCount(Comm.getAeroId());
            if (aeroCount >= 1 && aeroCount >= Count.getLimit(Comm.getAeroId()) && aeroCount < 5) {
                Count.setLimit(Comm.getAeroId(), aeroCount + 1);
                turnCooldown = 0;
            }
        }
    }

}
