package ryoutabot;

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

    protected static void execute() throws GameActionException {
        executeUnit();
        Count.incrementBuffer(Comm.getTankId());
//        Debug.checkHealthOfNearbyAllies(2);
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
        if (rc.isWeaponReady() && Attack.canAttack(attackableEnemies)) {
            attack();
        } else if (rc.isCoreReady()
                && rc.senseNearbyRobots(typ.attackRadiusSquared, otherTeam).length == 0) {
            tryMoveCloserToEnemy(closestLocation, 1,
                    closestLocation != enemyHQ, true);
        }
    }

    /**
     * Our tanks are rushing to their destination; this is an offensive tactic.
     * @throws GameActionException
     */
    private static void rushCode() throws GameActionException {

        /**
         * If our weapon is ready and we should attack enemies that are in our range, we attack.
         * (if the fight is winning). Otherwise, we check if we can escape fast enough, or if we
         * can kite at all. If so, we try to do that. Lastly, if we have no choice, we simply fight.
         */
        Debug.clockString(0, "Rush mode aint got nothing else");

        /**
         * First, we check if there are any visible enemies. If there are, we should ignore our
         * rush to target and perform special attack micro. Obviously, we need to handle special checks
         * for when the visible enemy is
         */
        if (visibleEnemies.length > 0) {
            doMicro();
            return;
        }

        /**
         * Otherwise, there are no robots in our attack range, we should try to move towards our
         * destination, as long as there are no visible units in the way, so first we check for that.
         */
            // Check if we are outnumbering the enemy
//            if (false) {
//
//            } else {
//                RobotInfo closestEnemy = Utils.getClosestEnemy(visibleEnemies);
//                // Move closer to that enemy if I will be able to take a shot at the opponent before the
//                // opponent can take a shot at me
//                return;
//            }
//        }
        Debug.clockString(0, "Made it here");

        MapLocation destination = closestLocation;
        NavTangentBug.setDest(destination);
        NavTangentBug.calculate(2500);
        if (rc.isCoreReady()) {
            Direction nextMove = NavTangentBug.getNextMove();
            if (myLoc.distanceSquaredTo(closestLocation) <= 35) {
                tryMoveCloserToEnemy(closestLocation, 1,
                        closestLocation != enemyHQ, true);
            } else if (nextMove != Direction.NONE) {
                NavSimple.walkTowardsDirected(nextMove);
                Debug.clockString(2, "Tangent buggin");
            }
        }
    }

    /**
     * Our highest priority is if we are already in combat. In this stage, we figure out which of
     * the following to apply to our fight:
     *   1. We are going to win the fight (1v1)
     *   2. We are going to win the fight (numbers advantage)
     *   3. We are going to win the fight if we kite and they chase.
     *   4. We are going to lose the fight, but we can escape.
     *   5. We are going to lose the fight, but we can't escape.
     *   6. We are going to win the fight because we have superior range.
     *   7. We are going to lose the fight because we have no range. [subset of 4 and 5]
     *
     *
     * @return true if micro succeeded, false if did nothing with micro
     * @throws GameActionException
     */
    public static boolean doMicro() throws GameActionException {
        // COMBAT MODE PRIORITY: focus on the people who can attack us first.
        Debug.clockString(0, "Microing");
        Debug.logEnemyNumberData(1);
        if (numEnemiesAttackingUs >= 1) {
            Debug.clockString(0, "got some enemies");
            // First we find out how our numbers are
            int maxNumAlliesAttackingEnemy = 0;
            for (int i = numEnemiesAttackingUs; --i >= 0;) {
                int numAlliesAttackingEnemy = Utils.numAlliesCanAttackEnemyAtLocation(Handler.enemiesAttackingUs[i].location);
                if (myLoc.distanceSquaredTo(Handler.enemiesAttackingUs[i].location) <= typ.attackRadiusSquared)
                    numAlliesAttackingEnemy++;
                if (numAlliesAttackingEnemy > maxNumAlliesAttackingEnemy)
                    maxNumAlliesAttackingEnemy = numAlliesAttackingEnemy;
            }
            if (numEnemiesAttackingUs == 1) {
                Debug.clockString(0, "got some enemies attacking us (1) and " + maxNumAlliesAttackingEnemy + "friends");
                if (maxNumAlliesAttackingEnemy == 0) {
                    // No one around us can attack the enemy, even me. This means that we're getting
                    // outranged by this unit, which is largely difficult. By default, we're just
                    // going to try to run away.
                    tryToRetreat();
                } else if (maxNumAlliesAttackingEnemy == 1) {
                    // This is currently a 1v1 fight. It might not be 1v1 between me and the enemy,
                    // though. The following check will figure that out.
                    RobotInfo enemy = findAnAttackingEnemy(attackableEnemies);
                    if (enemy == null) {
                        // We can't attack the enemy, so the enemy is actually having a 1v1 fight with
                        // someone else. As a result, since we have the numbers advantage, we should go in.
                        // Our count here reveals that the enemy can also attack us though, so the next
                        // line should work.
                        enemy = enemiesAttackingUs[0];
                        if (rc.isCoreReady()) {
                            tryMoveCloserToEnemy(enemy.location, 1, enemy.location != Handler.enemyHQ, true);
                            Debug.clockString(0, "1v1 move");
                            return true;
                        }
                        return true;
                    }
                    // Otherwise, it should be a 1v1.
                    boolean winning1v1 = isWinning1v1(enemy);
                    rc.setIndicatorString(2, "Am I winning a 1v1" + winning1v1);
                    if (winning1v1) {
                        // We're winning a 1v1, so we try to fight the enemy
                        if (rc.isWeaponReady()) {
                            rc.attackLocation(enemy.location);
                            return true;
                        } else {
                            return true;
                        }
                    } else {
                        tryToRetreat();
                        Debug.clockString(0, "running fail");
                    }
                } else {
                    // We outnumber the enemy. Great victory. Now, we beat it up.
                    // We try to find the enemy, which may not be in our attack radius.
                    RobotInfo enemy = findAnAttackingEnemy(attackableEnemies);

                    // The enemy can attack us, but we can't attack them. Since we're winning, we
                    // should try to move towards them.
                    if (enemy == null) {
                        enemy = enemiesAttackingUs[0];
                        if (rc.isCoreReady()) {
                            tryMoveCloserToEnemy(enemy.location, 1, enemy.location != Handler.enemyHQ, true);
                            Debug.clockString(0, "Outnumber move vs 1");
                            return true;
                        }
                        return true;
                    }
                    // If our weapon is ready, we should attack.
                    if (rc.isWeaponReady()) {
                        rc.attackLocation(enemy.location);
                        return true;
                    }
//                    } else {
//                        // Otherwise we should check if we're already in the range of the enemy.
//
//                        RobotInfo closestEnemyUnit = Utils.closestNonConstructingUnit(Handler.visibleEnemies);
//                        int numAlliesFighting = numAlliesInAttackRange(closestEnemyUnit.location);
//                        if (closestEnemyUnit != null && rc.isCoreReady()) {
//                            tryMoveCloserToEnemy(closestEnemyUnit.location,
//                                    numAlliesFighting + 1, closestEnemyUnit.location != Handler.enemyHQ,
//                                    false);
//                            Debug.clockString(0, "moving in to the party");
//
//                        } else {
//                            return true;
//                        }
//                    }

                }
            } else if (numEnemiesAttackingUs > maxNumAlliesAttackingEnemy) {
//                    && !Attack.guessIfFightIsWinning()) {
                // We are being outnumbered and we don't think we're going to win.
                tryToRetreat();
            } else {
                // We are doing the outnumbering and there is already an enemy attacking me, so I'm
                // just going to fight.
                if (rc.isWeaponReady()) {
                    RobotInfo enemy = findAnAttackingEnemy(attackableEnemies);
                    if (enemy == null) {
                        enemy = enemiesAttackingUs[0];
                        if (rc.isCoreReady()) {
                            tryMoveCloserToEnemy(enemy.location, 1, enemy.location != Handler.enemyHQ, true);
                            Debug.clockString(0, "Outnumber move");
                            return true;
                        }
                        return true;
                    }
                    rc.attackLocation(enemy.location);
                }
            }
            return true;
        } else if (numAttackableEnemies > 0) {
                // We actually outrange the enemy, so let's take advantage of it and attack them.
                if (rc.isWeaponReady()) {
//                    RobotInfo enemy = findAnEnemy(attackableEnemies);
                    rc.attackLocation(attackableEnemies[0].location);
                }
                return true;

        } else {
            rc.setIndicatorString(0, "I have nothing to do on turn " + Clock.getRoundNum());
            // NON-COMBAT MODE

            // TODO (kpeng94): handle HQ range
            // We're not getting attacked by enemies, so we should check for a few things. we should help allies being attacked if
            // we can win the fight or run away if we can't. The last thing we
            // should do is take advantage of useless buildings that are not
            // doing anything.
            RobotInfo closestEnemyUnit = Utils.closestNonConstructingUnit(Handler.visibleEnemies);
            if (closestEnemyUnit != null) {
                // TODO: figure out what is actually going on here
                int numAlliesFighting = numAlliesInAttackRange(closestEnemyUnit.location);
                rc.setIndicatorString(0, "I have nothing to do, but I have detected that there are some allies fighting: " + numAlliesFighting + " on turn "  + Clock.getRoundNum());
                if (numAlliesFighting > 0) {
                    if (rc.isCoreReady()) {
                        tryMoveCloserToEnemy(closestEnemyUnit.location,
                                numAlliesFighting + 1, closestEnemyUnit.location != Handler.enemyHQ,
                                true);
                        Debug.clockString(0, "Helping buddy move");
                    }
                    return true;
                }
            }
        }
        return true;
    }


    /**
     * Tries to run away from the enemy.
     * @throws GameActionException
     */
    public static void tryToRetreat() throws GameActionException {
        boolean canHitEnemyDownThisTurn = false;
        boolean enemyCanHitUs = false;
        for (int i = numEnemiesAttackingUs; --i >= 0;) {
            RobotInfo enemy = enemiesAttackingUs[i];
            if (enemy.health <= typ.attackPower && rc.isWeaponReady()) {
                canHitEnemyDownThisTurn = true;
                break;
            }
            if (enemy.weaponDelay < typ.attackDelay && enemy.buildingLocation == null) {
                enemyCanHitUs = true;
                break;
            }
        }

        if (canHitEnemyDownThisTurn || !enemyCanHitUs) {
            if (rc.isWeaponReady()) {

            RobotInfo enemy = findAnAttackingEnemy(attackableEnemies);
            if (enemy != null) {
                rc.attackLocation(enemy.location);
                return;
            }
            }
        }
        Direction dir = chooseRetreatDirection();
        Debug.clockString(1, "mad danger ranger");
        if (dir == null) {
            if (rc.isWeaponReady()) {
                RobotInfo enemy = findAnAttackingEnemy(attackableEnemies);
                if (enemy != null) {
                    rc.attackLocation(enemy.location);
                } else {
                    if (rc.isCoreReady()) {
                        rc.move(myLoc.directionTo(enemiesAttackingUs[0].location));
                        Debug.clockString(0, "tried to retreat but i be moving ");
                    }
                }
            }
        } else {
            if (rc.isCoreReady()) {
                rc.move(dir);
                Debug.clockString(0, "tried to retreat in dir" + dir);
            }
        }

    }

    private static Direction chooseRetreatDirection() throws GameActionException {
        int repelX = 0;
        int repelY = 0;
        for (int i = visibleEnemies.length; --i >= 0;) {
            Direction repelDir = visibleEnemies[i].location.directionTo(myLoc);
            repelX += repelDir.dx;
            repelY += repelDir.dy;
        }
        int absRepelX = Math.abs(repelX);
        int absRepelY = Math.abs(repelY);
        Direction retreatDir;
        if (absRepelX >= 1.5 * absRepelY) {
            retreatDir = repelX > 0 ? Direction.EAST : Direction.WEST;
        } else if (absRepelY >= 1.5 * absRepelX) {
            retreatDir = repelY > 0 ? Direction.SOUTH : Direction.NORTH;
        } else if (repelX > 0) {
            retreatDir = repelY > 0 ? Direction.SOUTH_EAST : Direction.NORTH_EAST;
        } else {
            retreatDir = repelY > 0 ? Direction.SOUTH_WEST : Direction.NORTH_WEST;
        }

        int bestMinEnemyDistSq = 999999;
        for (int j = visibleEnemies.length; j-- > 0;) {
            int enemyDistSq = myLoc.distanceSquaredTo(visibleEnemies[j].location);
            if (enemyDistSq < bestMinEnemyDistSq) bestMinEnemyDistSq = enemyDistSq;
        }
        Direction bestDir = null;
        int[] tryDirs = new int[] { 0, 1, -1, 2, -2, 3, -3, 4 };
        for (int i = 0; i < tryDirs.length; i++) {
            Direction tryDir = Direction.values()[(retreatDir.ordinal() + tryDirs[i] + 8) % 8];
            if (!rc.canMove(tryDir)) continue;
            MapLocation tryLoc = myLoc.add(tryDir);
            if (Utils.isInTheirHQAttackRange(tryLoc)) continue;

            int minEnemyDistSq = 999999;
            RobotType enemyType = null;
            for (int j = visibleEnemies.length; j-- > 0;) {
                int enemyDistSq = tryLoc.distanceSquaredTo(visibleEnemies[j].location);
                if (enemyDistSq < minEnemyDistSq) minEnemyDistSq = enemyDistSq;

            }
            if (enemyType != null && minEnemyDistSq > enemyType.attackRadiusSquared) {
                return tryDir; // we can escape!!
            }
            if (minEnemyDistSq > bestMinEnemyDistSq) {
                bestMinEnemyDistSq = minEnemyDistSq;
                bestDir = tryDir;
            }
        }

        return bestDir;
    }
    private static int numAlliesInAttackRange(
            MapLocation location) {
        // TODO (kpeng94): change variable for 24
        RobotInfo[] allies = rc.senseNearbyRobots(location, 24, myTeam);
        return allies.length;
    }

    /**
     * Figures out whether we are winning a 1v1 fight. This breaks down into
     * a few cases.
     *
     * 1. If the two robots are the same type, whoever can kill the enemy first
     * is the winner.
     *
     * 2. If the two robots are not the same type, but have the same range, we
     * take into account a different number of factors that come from
     * MD, CD, LD, and AD.
     *
     * 3. If you have a smaller range, but the enemy is within your attack radius,
     * same as 2.
     *
     * 4. If you are being outranged, we constitute this as an automatic defeat.
     *
     * 5. You outrange the enemy. You should be kiting if possible.
     *
     * @param enemy
     * @return
     */
    public static boolean isWinning1v1(RobotInfo enemy) {
        // If we are the same type, calculate the rate of death for each of
        // the units
        if (typ == enemy.type) {
            double myWeaponDelay = rc.getWeaponDelay();
            double enemyWeaponDelay = enemy.weaponDelay;
            double mySupply = rc.getSupplyLevel();
            double enemySupply = enemy.supplyLevel;
            double myHealth = rc.getHealth();
            double enemyHealth = enemy.health;
            int numOfHitsUntilEnemyDeath = (int) (enemyHealth / typ.attackPower);
            int numOfHitsUntilMyDeath = (int) (myHealth / typ.attackPower);
            // Does not yet account for supply. Assuming equal supply.
            // In this case, no matter what we will lose because
            // at most we can get one shot off and then it will be even, but
            // the enemy definitely goes first.
            if (numOfHitsUntilMyDeath < numOfHitsUntilEnemyDeath) {
                return false;
            }
            if (numOfHitsUntilMyDeath > numOfHitsUntilEnemyDeath) {
                return true;
            }
            // Here the number of hits is the same, so whoever has the lower action delay wins,
            // unless we lose in execution order and the
            if (myWeaponDelay - enemyWeaponDelay <= -1) {
                return true;
            }
            if (enemyWeaponDelay - myWeaponDelay <= -1) {
                return true;
            }
            return false;
        }
        if (typ.attackRadiusSquared < enemy.type.attackRadiusSquared) {
            if (myLoc.distanceSquaredTo(enemy.location) <= typ.attackRadiusSquared) {
                return true;
            }
            return true;
        }
        return false;
    }

    private static void rallyCode() throws GameActionException {
        if (rc.isWeaponReady() && Attack.canAttack(attackableEnemies)) {
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
