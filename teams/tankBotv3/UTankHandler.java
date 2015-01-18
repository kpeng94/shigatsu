package tankBotv3;

import battlecode.common.*;

public class UTankHandler extends UnitHandler {

    private static RobotType myType = RobotType.TANK;
    private static RobotInfo[] enemies;
    private static int minDistance = Integer.MAX_VALUE;
    private static MapLocation closestLocation;
    private static TankState state = TankState.NEW;
    private static TankState nextState;
    private static int numberOfTanks = 0;
    private static boolean rallied = false;
    private static int numberOfTanksRallied = 0;
    private static MapLocation rallyPoint;
    private static double health;
    private static int damageIAmTaking;
    private static int alliesDamage;
    private static int[] revDirections = { 4, -3, 3, -2, 2, -1, 1, 0 };

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
                // e.printStackTrace();
                System.out.println(typ + " Execution Exception");
            }
            rc.yield(); // Yields to save remaining bytecodes
        }
    }

    protected static void init(RobotController rcon) throws GameActionException {
        initUnit(rcon);
        typ = RobotType.TANK;
    }

    protected static void execute() throws GameActionException {
        executeUnit();
        health = rc.getHealth();
        readBroadcasts();
        minDistance = Integer.MAX_VALUE;
        if (enemyTowers.length == 0) {
            closestLocation = enemyHQ;
        }
        for (int i = enemyTowers.length; --i >= 0;) {
            int distanceSquared = myHQ.distanceSquaredTo(enemyTowers[i]);
            if (distanceSquared <= minDistance) {
                closestLocation = enemyTowers[i];
                minDistance = distanceSquared;
            }
        }
        if (rc.isWeaponReady() && decideAttack()) {
            attack();
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
        nubMicro();
    }

    private static void nubMicro() throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(myType.sensorRadiusSquared,
                otherTeam);
        int[] closestEnemyInfo = getClosestEnemy(enemies);
        MapLocation closestEnemyLoc = new MapLocation(closestEnemyInfo[1],
                closestEnemyInfo[2]);
        int closestEnemyDistance = closestEnemyInfo[0];

    }

    /**
     * Requires that attackableEnemies is already set correctly.
     *
     * @param enemyLoc
     * @throws GameActionException
     */
    private static void retreatOrFight(MapLocation enemyLoc)
            throws GameActionException {
        if (attackableEnemies.length == 1) {
            for (int i = attackableEnemies.length; --i >= 0;) {
                if (attackableEnemies[i].health <= typ.attackPower) {
                    rc.attackLocation(attackableEnemies[i].location);
                    return;
                } else {
                    // consider action delay and stuff here and range
                }
            }

        }
        Direction direction = getRetreatDirection();
        if (direction != null) {
            if (attackableEnemies.length > 0) {
                rc.attackLocation(attackableEnemies[0].location);
            }
        } else {
            NavSimple.walkTowardsDirected(enemyLoc.directionTo(myLoc));
        }
    }

    private static Direction getRetreatDirection() {
        int x = 0;
        int y = 0;
        for (int i = visibleEnemies.length; --i >= 0;) {
            Direction d = visibleEnemies[i].location.directionTo(myLoc);
            x += d.dx;
            y += d.dy;
        }

        int ax = Math.abs(x);
        int ay = Math.abs(y);
        Direction retreatDir;
        if (ax >= 1.5 * ay) {
            retreatDir = x > 0 ? Direction.EAST : Direction.WEST;
        } else if (ay >= 1.5 * ax) {
            retreatDir = y > 0 ? Direction.SOUTH : Direction.NORTH;
        } else if (x > 0) {
            retreatDir = y > 0 ? Direction.SOUTH_EAST : Direction.NORTH_EAST;
        } else {
            retreatDir = y > 0 ? Direction.SOUTH_WEST : Direction.NORTH_WEST;
        }
        return null;
    }

    private static void rushCode() throws GameActionException {
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
                        closestLocation != enemyHQ);
            } else if (nextMove != Direction.NONE) {
                NavSimple.walkTowardsDirected(nextMove);
            }
        }
    }

    private static void rallyCode() throws GameActionException {
        if (rallyPoint == null) {
            rallyPoint = MapUtils.pointSection(myHQ, enemyHQ, 0.75);
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
        // TODO: figure out this heuristic better
        if (myLoc.distanceSquaredTo(rallyPoint) <= typ.attackRadiusSquared) {
            broadcastNearRallyPoint();
        }
        if (numberOfTanksRallied >= Constants.TANK_RUSH_COUNT) {
            broadcastTeamRush();
        }
        if (Comm.readBlock(Comm.getTankId(), 4) != 0 && rallied) {
            nextState = TankState.RUSH;
        }

    }

    private static void newCode() {
        // TODO Auto-generated method stub
        nextState = TankState.RALLY;

    }

    /**
     * TODO breaking ties between units that are equidistant?
     *
     * Gets the closest enemy robot or HQ if list of enemies is empty.
     *
     * @param enemies
     *            list of enemies
     * @return integer array containing: [minimum distance to closest enemy,
     *         enemy's x location, enemy's y location]
     * @throws GameActionException
     */
    private static int[] getClosestEnemy(RobotInfo[] enemies)
            throws GameActionException {
        int minDistance = myLoc.distanceSquaredTo(enemyHQ);
        MapLocation closestEnemyLoc = enemyHQ;

        for (int i = enemies.length; --i >= 0;) {
            int distanceToEnemy = myLoc.distanceSquaredTo(enemies[i].location);
            if (distanceToEnemy < minDistance) {
                minDistance = distanceToEnemy;
                closestEnemyLoc = enemies[i].location;
            }
        }
        int[] distanceData = { minDistance, closestEnemyLoc.x,
                closestEnemyLoc.y };
        return distanceData;
    }

    /**
     *
     * @param enemies
     * @return
     * @throws GameActionException
     */
    private static double[] getEnemyWithLeastHealth(RobotInfo[] enemies)
            throws GameActionException {
        double leastHealth = 2000;
        MapLocation leastHealthEnemyLoc = enemyHQ;

        for (int i = enemies.length; --i >= 0;) {
            double enemyHealth = enemies[i].health;
            if (enemyHealth < leastHealth) {
                leastHealth = enemyHealth;
                leastHealthEnemyLoc = enemies[i].location;
            }
        }

        double[] data = { leastHealth, leastHealthEnemyLoc.x,
                leastHealthEnemyLoc.y };
        return data;

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

    public static boolean decideAttack() {
        enemies = rc.senseNearbyRobots(typ.attackRadiusSquared, otherTeam);
        if (enemies.length > 0) {
            return true;
        }
        return false;
    }

    public static void detectEnemyKiting() throws GameActionException {

    }

    public static void attack() throws GameActionException {
        rc.attackLocation(enemies[0].location);
    }

    /**
     * TODO: think this through better. TODO: sum up damages instead Get number
     * of units that can hit me
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

    public static void tryMoveCloserToEnemy(MapLocation location,
            int maxEnemyExposure, boolean ignoreHQ) throws GameActionException {
        Direction toEnemy = myLoc.directionTo(location);
        int distanceSquaredToLoc = myLoc.distanceSquaredTo(location);
        Direction[] tryDirs = new Direction[] {
                toEnemy.rotateRight().rotateRight(),
                toEnemy.rotateLeft().rotateLeft(), toEnemy.rotateRight(),
                toEnemy.rotateLeft(), toEnemy };
        for (int i = tryDirs.length; --i >= 0;) {
            Direction tryDir = tryDirs[i];
            if (!rc.canMove(tryDir))
                continue;
            int newX = myLoc.x + tryDir.dx;
            int newY = myLoc.y + tryDir.dy;
            int deltaX = Math.abs(newX - location.x)
                    - Math.abs(myLoc.x - location.x);
            int deltaY = Math.abs(newY - location.y)
                    - Math.abs(myLoc.y - location.y);
            MapLocation newLoc = new MapLocation(newX, newY);
            int changingParams = 0;
            if (deltaY > 0) {
                changingParams++;
            }
            if (deltaX > 0) {
                changingParams++;
            }
            if (newLoc.distanceSquaredTo(location) >= distanceSquaredToLoc) {
                changingParams++;
            }
            if (changingParams >= 2) {
                continue;
            }
            if (enemyHQAttackCanMe(myLoc) && ignoreHQ)
                continue;
            rc.move(tryDir);
            return;
        }
    }
}
