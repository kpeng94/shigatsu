package pusheenTankBotMiningBuildDynamic;

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

        countChans = new int[] { Comm.getBeaverId(), Comm.getMinerId(), Comm.getTankId(), Comm.getMinerfactId(), Comm.getBarrackId(), Comm.getTankfactId(),
                Comm.getSupplyId() };

        initCounts();

        Count.setLimit(Comm.getBeaverId(), 1); // Maintain 1 beaver
    }

    protected static void execute() throws GameActionException {
        executeStructure();
        updateTowers();

        for (int i = countChans.length; --i >= 0;) {
            Count.resetBuffer(countChans[i]);
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
        

        Supply.spreadSupplies(Supply.DEFAULT_THRESHOLD);
        Distribution.spendBytecodesCalculating(7500);
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

    protected static void updateLimits() throws GameActionException {
        if (turnCooldown >= TURN_COUNTER && rc.getTeamOre() > ORE_THRESHOLD && getOreChangeInLastFewTurns() >= 0) {
            int tankFactCount = Count.getCount(Comm.getTankfactId());
            rc.setIndicatorString(1, "Number of tank factories: " + tankFactCount);
            rc.setIndicatorString(2, "Limit of tank factories: " + Count.getLimit(Comm.getTankfactId()));
            if (tankFactCount >= Count.getLimit(Comm.getTankfactId())) {
                System.out.println("Set tank factory limit to " + (tankFactCount + 1));
                Count.setLimit(Comm.getTankfactId(), tankFactCount + 1);
                turnCooldown = 0;
            }
        }
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
        if (Count.getCount(Comm.getTankId()) >= 25) { // Tanks >= 25
            Count.setLimit(Comm.getSupplyId(), 30);
        }
        if (Count.getCount(Comm.getMinerId()) >= 25) { // Miners >= 25
            Count.setLimit(Comm.getMinerfactId(), 1);
            Count.setLimit(Comm.getMinerId(), 40);
            Count.setLimit(Comm.getBeaverId(), 2);
            Count.setLimit(Comm.getBarrackId(), 1);
            // Count.setLimit(Comm.getTankfactId(), 4);
            Count.setLimit(Comm.getTankId(), 999);
            Count.setLimit(Comm.getSupplyId(), 10);
        } else if (Count.getCount(Comm.getTankfactId()) == 1) { // Tank factory
            Count.setLimit(Comm.getMinerId(), 25);
        } else if (Count.getCount(Comm.getMinerId()) >= 10) { // 10 miners
            Count.setLimit(Comm.getTankfactId(), 1);
            Count.setLimit(Comm.getTankId(), 999);
        } else if (Count.getCount(Comm.getMinerfactId()) == 1) { // 1 mining
                                                                 // fact
            Count.setLimit(Comm.getBeaverId(), 2);
            Count.setLimit(Comm.getBarrackId(), 1);
            Count.setLimit(Comm.getSoldierId(), 1); // random soldier scout guy
        } else if (Count.getCount(Comm.getBeaverId()) == 1) { // 1 beaver
            Count.setLimit(Comm.getMinerfactId(), 1);
            Count.setLimit(Comm.getMinerId(), 10);
        }

    }

    protected static void checkMinerFrontier() throws GameActionException {
        int frontier = Comm.readBlock(Comm.getMinerId(), Mining.FRONTIER_OFFSET);
        if (frontier != 0) {
            int priority = frontier >>> 16;
            MapLocation loc = MapUtils.decode(frontier & 0xFFFF);
            System.out.println(Clock.getRoundNum() + ": " + loc);
        }
    }

    private static final int TURN_COUNTER = 150;
    private static double[] oreCounts = new double[TURN_COUNTER];
    private static final int ORE_THRESHOLD = 1500;
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
    
//    private static int calculateSupplyUpkeep(){
//        
//    }

}
