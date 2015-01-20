package tankBotv4;

import battlecode.common.*;

public class UTankHandler extends UnitHandler {

    private static RobotType myType = RobotType.TANK;
    private static int minDistance = Integer.MAX_VALUE;
    private static MapLocation closestLocation;
    private static TankState state = TankState.NEW;
    private static TankState nextState;
    private static TankState prevState = TankState.NEW;
    private static int numberOfTanks = 0;
    private static boolean rallied = false;
    private static int numberOfTanksRallied = 0;
    private static MapLocation rallyPoint;
    private static int damageIAmTaking;
    private static int alliesDamage;
    private static int[] revDirections = { 4, -3, 3, -2, 2, -1, 1, 0 };
    private static RobotInfo[] alliesTwoAround;
    private static int wave = 0;
    private static int myWaveNumber = 1;

    private enum TankState {
        NEW, RALLY, RUSH, SWARM, FIGHTING
    }

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
        initUnit(rcon);
        typ = RobotType.TANK;
        rallyPoint = MapUtils.pointSection(myHQ, enemyHQ, 0.75);
        myWaveNumber = Comm.readBlock(Comm.getTankId(), Comm.WAVENUM_OFFSET);
    }

//    private static MapLocation getRallyPointBroadcast(int wave)
//            throws GameActionException {
//        int offset;
//        switch (wave) {
//            case 1:
//            default:
//                offset = Comm.TANK_WAVE_ONE_RALLYPOINT_OFFSET;
//                break;
//            case 2:
//                offset = Comm.TANK_WAVE_TWO_RALLYPOINT_OFFSET;
//                break;
//            case 3:
//                offset = Comm.TANK_WAVE_THREE_RALLYPOINT_OFFSET;
//                break;
//        }
//        return MapUtils.decode(Comm.readBlock(Comm.getTankId(), offset));
//    }

    protected static void execute() throws GameActionException {
        executeUnit();
        Count.incrementBuffer(Comm.getTankId());
        health = rc.getHealth();
        minDistance = Integer.MAX_VALUE;
        if (enemyTowers.length == 0) {
            closestLocation = enemyHQ;
        } else {
            closestLocation = Utils.getClosestTower();
        }

        Utils.updateEnemyInfo();
        switch (state) {
            case NEW:
                newCode();
                break;
            case RALLY:
                rallyCode();
                break;
            case RUSH:
                rushCode();
                break;
            case FIGHTING:
                fightingCode();
                break;
        }
        if (nextState != null) {
            state = nextState;
            nextState = null;
        }
        Supply.spreadSupplies(Supply.DEFAULT_THRESHOLD);
    }

    private static void fightingCode() throws GameActionException {
        if (rc.isWeaponReady() && Attack.shouldAttack(attackableEnemies)) {
            attack();
        } else if (rc.isCoreReady()
                && rc.senseNearbyRobots(typ.attackRadiusSquared, otherTeam).length == 0) {
            tryMoveCloserToEnemy(closestLocation, 1,
                    closestLocation != enemyHQ, true);
        }
    }

    private static void rushCode() throws GameActionException {
        // This check is for the fact that the unit might be blocking another
        // unit. Here we are assuming that we are going on the offensive.

        if (rc.isWeaponReady() && Attack.shouldAttack(attackableEnemies)) {
            attack();
        }
        MapLocation destination = closestLocation;
        NavTangentBug.setDest(destination);
        NavTangentBug.calculate(2500);
        if (rc.isCoreReady()
                && rc.senseNearbyRobots(typ.attackRadiusSquared, otherTeam).length == 0) {
            Direction nextMove = NavTangentBug.getNextMove();
            if (myLoc.distanceSquaredTo(closestLocation) <= 35) {
                tryMoveCloserToEnemy(closestLocation, 1,
                        closestLocation != enemyHQ, true);
            } else if (nextMove != Direction.NONE) {
                NavSimple.walkTowardsDirected(nextMove);
            }
        }
    }

    private static void rallyCode() throws GameActionException {
        if (rc.isWeaponReady() && Attack.shouldAttack(attackableEnemies)) {
            attack();
        }
        NavTangentBug.setDest(rallyPoint);
        NavTangentBug.calculate(2500);
        // TODO (kpeng94): maybe change this so that when you're in a really close region, bug
        // around instead
        if (rc.isCoreReady()) {
            if (rc.senseNearbyRobots(typ.attackRadiusSquared, otherTeam).length == 0) {
                Direction nextMove = NavTangentBug.getNextMove();
                if (nextMove != Direction.NONE) {
                    NavSimple.walkTowardsDirected(nextMove);
                } else {
                    rallied = true;
                }
            }
        }
        numberOfTanksRallied = Count.getCountAtRallyPoint(Comm.getTankId(), myWaveNumber);
//        rc.setIndicatorString(1, "My wave number is: " + myWaveNumber + ". The number of tanks that rallied is given by: " + numberOfTanksRallied);
//        rc.setIndicatorString(2, "My loc is " + myLoc + "my dist is " + );
        // TODO: figure out this heuristic better

        if (myLoc.distanceSquaredTo(rallyPoint) <= typ.sensorRadiusSquared) {
//            rc.setIndicatorString(0, "At clock turn " + Clock.getRoundNum()
//                    + ", I am rallying to the rally point at: " + rallyPoint
//                    + " and my rallied boolean is: " + rallied);
            Count.incrementAtRallyPoint(Comm.getTankId(), myWaveNumber);
        }
        if (numberOfTanksRallied >= Constants.TANK_RUSH_COUNT) {
            nextState = TankState.RUSH;
        }

    }

    /**
     * Decide what to do on spawn.
     */
    public static void newCode() {
        if (Clock.getRoundNum() < 2000) {
            nextState = TankState.RALLY;
        } else {

        }
    }

    public static void broadcastNearRallyPoint() throws GameActionException {
        numberOfTanksRallied++;
        Comm.writeBlock(Comm.getTankId(), Comm.COUNT_NEARRALLYPOINT_OFFSET,
                numberOfTanksRallied);
    }

    public static void detectEnemyKiting() throws GameActionException {

    }

    /**
     * Assumes enemies array is nonempty and correctly updated.
     *
     * @throws GameActionException
     */
    public static void attack() throws GameActionException {
        double damageReduction = 0;
        // int[] closestEnemy = getClosestEnemy(enemies);
        // MapLocation attackLocation = new MapLocation(closestEnemy[1],
        // closestEnemy[2]);
        MapLocation attackLocation = Attack.chooseTargetByDamageReduction();
        if (attackLocation == null) {
            attackLocation = attackableEnemies[0].location;
        }
        rc.attackLocation(attackLocation);
    }



    /**
     * TODO: think this through better.
     * TODO: sum up damages instead
     * Get number of units that can hit me
     *
     * @param location
     * @param team
     * @return
     */
    public static int getUnitsCanHitMe(MapLocation location, Team team) {
        RobotInfo[] enemiesList = rc.senseNearbyRobots(location, 24,
                team.opponent());
        int count = 0;
        for (int i = enemiesList.length; --i >= 0;) {
            MapLocation loc = enemiesList[i].location;
            int distSqToLoc = location.distanceSquaredTo(loc);
            if (distSqToLoc <= enemiesList[i].type.attackRadiusSquared) {
                count++;
            }
        }
        return count;
    }

    public static void calculateSortedEnemyTowers() {
        MapLocation[] towers = rc.senseEnemyTowerLocations();
        if (enemyTowers == null || towers.length != enemyTowers.length) {
            for (int i = towers.length; --i >= 0;) {
                MapLocation towerLoc = towers[i];
                int currentLocation = i;
                for (int j = i; --j >= 0;) {
                    if (myHQ.distanceSquaredTo(towerLoc) < myHQ
                            .distanceSquaredTo(towers[j])) {
                        MapLocation temp = towers[j];
                        towers[j] = towerLoc;
                        towers[currentLocation] = temp;
                        currentLocation = j;
                    }
                }
            }
            enemyTowers = towers;
        }
    }

    /**
     * Hacky heuristic, but might be worth, assumes that enemyTowers is already
     * sorted by distance from your HQ
     *
     */
    public static void calculateDistanceBetweenClosestEnemyTowers() {

    }

}
