package tankBotv4;

import battlecode.common.*;

public class UTankHandler extends UnitHandler {

    private static RobotType myType = RobotType.TANK;
    private static RobotInfo[] enemies;
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
        // rallyPoint = getRallyPointBroadcast(1);
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
        readBroadcasts();
        minDistance = Integer.MAX_VALUE;
        if (enemyTowers.length == 0) {
            closestLocation = enemyHQ;
        }
        Utils.updateEnemyInfo();
        for (int i = enemyTowers.length; --i >= 0;) {
            int distanceSquared = myHQ.distanceSquaredTo(enemyTowers[i]);
            if (distanceSquared <= minDistance) {
                closestLocation = enemyTowers[i];
                minDistance = distanceSquared;
            }
        }
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
        Utils.updateEnemyInfo();
        if (rc.isWeaponReady() && Attack.shouldAttack(enemies)) {
            attack();
        } else if (rc.isCoreReady()
                && rc.senseNearbyRobots(typ.attackRadiusSquared, otherTeam).length == 0) {
            tryMoveCloserToEnemy(closestLocation, 1,
                    closestLocation != enemyHQ, true);
        }
        // nubMicro();
    }

    private static void nubMicro() throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(myType.sensorRadiusSquared,
                otherTeam);
        MapLocation closestEnemyLoc = Attack.getClosestEnemy(enemies);

    }

    private static void rushCode() throws GameActionException {
        // This check is for the fact that the unit might be blocking another
        // unit. Here we are assuming that we are going on the offensive.

        // closestLocation is assumed to be the location that we are targeting
        // boolean shouldMoveInstead = false;
        // alliesTwoAround = rc.senseNearbyRobots(myLoc, 2, myTeam);
        // for (int i = alliesTwoAround.length; --i >= 0;) {
        // if (alliesTwoAround[i].location.distanceSquaredTo(closestLocation) >
        // typ.attackRadiusSquared) {
        // // shouldMoveInstead = true;
        // boolean b = tryMoveCloserToEnemy(closestLocation, 1,
        // closestLocation != enemyHQ);
        // if (b)
        // return;
        // }
        // }
        if (rc.isWeaponReady() && Attack.shouldAttack(enemies)
        // && !shouldMoveInstead
        ) {
            attack();
        }
        MapLocation destination = closestLocation;
        if (closestLocation != null) {
            // switch (myHQ.directionTo(closestLocation)) {
            // case NORTH_EAST:
            // case NORTH_WEST:
            // case SOUTH_EAST:
            // case SOUTH_WEST:
            // destination = closestLocation.add(myHQToEnemyHQ.rotateRight(),
            // -3).add(myHQToEnemyHQ.rotateLeft(), -2);
            // break;
            // case NORTH:
            // case EAST:
            // case SOUTH:
            // case WEST:
            // destination = closestLocation.add(myHQToEnemyHQ,
            // -3).add(myHQToEnemyHQ.rotateRight().rotateRight(), -2);
            // break;
            // default:
            // break;
            // }
        }
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
        if (rc.isWeaponReady() && Attack.shouldAttack(enemies)) {
            attack();
        }
        NavTangentBug.setDest(rallyPoint);
        NavTangentBug.calculate(2500);
        if (rc.isCoreReady()
                && rc.senseNearbyRobots(typ.attackRadiusSquared, otherTeam).length == 0) {
            Direction nextMove = NavTangentBug.getNextMove();
            if (nextMove != Direction.NONE) {
                NavSimple.walkTowardsDirected(nextMove);
            } else {
                rallied = true;
            }
        }
        rc.setIndicatorString(0, "At clock turn " + Clock.getRoundNum()
                + ", I am rallying to the rally point at: " + rallyPoint
                + " and my rallied boolean is: " + rallied);
        // TODO: figure out this heuristic better
        if (myLoc.distanceSquaredTo(rallyPoint) <= typ.attackRadiusSquared / 2) {
            broadcastNearRallyPoint();
        }
        if (numberOfTanksRallied >= Constants.TANK_RUSH_COUNT) {
            broadcastTeamRush();
        }
        if (Comm.readBlock(Comm.getTankId(), 4) != 0 && rallied) {
            nextState = TankState.RUSH;
        }

    }

    /**
     * Decide what to do on spawn.
     */
    private static void newCode() {
        nextState = TankState.RALLY;

    }

    public static void readBroadcasts() throws GameActionException {
        numberOfTanks = Comm.readBlock(Comm.getTankId(), Comm.COUNT_OFFSET);
        numberOfTanksRallied = Comm.readBlock(Comm.getTankId(),
                Comm.COUNT_NEARRALLYPOINT_OFFSET);
    }

    public static void broadcastNearRallyPoint() throws GameActionException {
        numberOfTanksRallied++;
        Comm.writeBlock(Comm.getTankId(), Comm.COUNT_NEARRALLYPOINT_OFFSET,
                numberOfTanksRallied);
    }

    public static void broadcastTeamRush() throws GameActionException {
        Comm.writeBlock(Comm.getTankId(), 4, 1);
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
            attackLocation = enemies[0].location;
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
