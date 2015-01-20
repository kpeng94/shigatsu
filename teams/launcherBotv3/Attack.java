package launcherBotv3;

import battlecode.common.*;

public class Attack {

    //
    //
    // DIFFERENT METHODS FOR CHOOSING TARGETS
    //
    //

    /**
     * Gets the closest enemy robot or null if list of enemies is empty.
     *
     * TODO (kpeng94): breaking ties between units that are equidistant?
     *
     * @param enemies
     *            list of enemies
     * @return integer array containing: [minimum distance to closest enemy,
     *         enemy's x location, enemy's y location]
     * @throws GameActionException
     */
    public static MapLocation getClosestEnemy(RobotInfo[] enemies)
            throws GameActionException {
        int minDistance = Handler.myLoc.distanceSquaredTo(Handler.enemyHQ);
        MapLocation closestEnemyLoc = null;

        for (int i = enemies.length; --i >= 0;) {
            int distanceToEnemy = Handler.enemyHQ
                    .distanceSquaredTo(enemies[i].location);
            if (distanceToEnemy < minDistance) {
                minDistance = distanceToEnemy;
                closestEnemyLoc = enemies[i].location;
            }
        }

        return closestEnemyLoc;
    }

    /**
     * Gets the enemy robot with least health or null if list of enemies is empty.
     * REQUIRES that Handler.attackableEnemies is properly updated.
     *
     * @return
     * @throws GameActionException
     */
    public static MapLocation getEnemyWithLeastHealth()
            throws GameActionException {
        double leastHealth = 2000;
        MapLocation leastHealthEnemyLoc = null;

        for (int i = Handler.attackableEnemies.length; --i >= 0;) {
            double enemyHealth = Handler.attackableEnemies[i].health;
            if (enemyHealth < leastHealth) {
                leastHealth = enemyHealth;
                leastHealthEnemyLoc = Handler.attackableEnemies[i].location;
            }
        }
        return leastHealthEnemyLoc;
    }

    /**
     * Chooses which target to attack based on the amount of damage reduction
     * this turn.
     *
     * @return
     */
    public static MapLocation chooseTargetByDamageReduction() {
        double maxDamageReduction = 0;
        MapLocation attackLocation = null;
        for (int i = Handler.attackableEnemies.length; --i >= 0;) {
            double dmgReduction = 0;
            RobotType enemyType = Handler.attackableEnemies[i].type;
            MapLocation enemyLocation = Handler.attackableEnemies[i].location;
            // Sense my team's robots that can attack this. TODO: fix for
            // launchers + towers + HQ
            RobotInfo[] myTeamAttacking = Handler.rc.senseNearbyRobots(enemyLocation,
                    15, Handler.myTeam);
            double damageSum = 0;
            for (int j = myTeamAttacking.length; --j >= 0;) {
                if (myTeamAttacking[j].weaponDelay < 1
                        && myTeamAttacking[j].location
                                .distanceSquaredTo(enemyLocation) <= myTeamAttacking[j].type.attackRadiusSquared) {
                    damageSum += myTeamAttacking[j].type.attackPower;
                }
            }
            if (damageSum > enemyType.maxHealth) {
                damageSum = enemyType.maxHealth;
            }

            if (enemyType != RobotType.LAUNCHER && enemyType.attackDelay != 0) {
                dmgReduction = enemyType.attackPower / enemyType.attackDelay
                        * damageSum / enemyType.maxHealth;
            }
            // if (enemyType == RobotType.TOWER) {
            // dmgReduction *= 3;
            // }
            if (dmgReduction > maxDamageReduction) {
                maxDamageReduction = dmgReduction;
                attackLocation = Handler.attackableEnemies[i].location;
            }
        }
        return attackLocation;

    }

    /**
     * Decides whether it is appropriate to attack
     *
     * TODO (kpeng94): this definitely requires more thought
     *
     * @return
     */
    public static boolean canAttack(RobotInfo[] enemies) {
        if (enemies.length > 0) {
            return true;
        }
        return false;
    }

    /**
     * Requires that attackableEnemies is already set correctly.
     *
     * @param enemyLoc
     * @throws GameActionException
     */
    public static void retreatOrFight(MapLocation enemyLoc)
            throws GameActionException {
        if (Handler.enemiesAttackingUs.length == 1) {
            for (int i = Handler.enemiesAttackingUs.length; --i >= 0;) {
                if (Handler.enemiesAttackingUs[i].health <= Handler.typ.attackPower) {
                    Handler.rc.attackLocation(Handler.enemiesAttackingUs[i].location);
                    return;
                } else {
                    // consider action delay and stuff here and range
                }
            }

        }
        Direction direction = getRetreatDirection();
        if (direction != null) {
            if (Handler.enemiesAttackingUs.length > 0) {
                Handler.rc.attackLocation(Handler.enemiesAttackingUs[0].location);
            }
        } else {
            NavSimple.walkTowardsDirected(enemyLoc.directionTo(Handler.myLoc));
        }
    }

    private static Direction getRetreatDirection() {
        int x = 0;
        int y = 0;
        for (int i = Handler.visibleEnemies.length; --i >= 0;) {
            Direction d = Handler.visibleEnemies[i].location.directionTo(Handler.myLoc);
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

    public static void tryAttackClosestButKillIfPossible(RobotInfo[] enemies) throws GameActionException {
        if (enemies.length == 0) return;

        int minDist = 999999;
        int minHP = 999999;
        MapLocation minLoc = null;

        int minMissileDist = 999999;
        int minMissileHP = 999999;
        MapLocation minMissileLoc = null;
        for (int i = enemies.length; --i >= 0;) {
            RobotInfo enemy = enemies[i];
            if (enemies[i].type == RobotType.MISSILE) { // Focus lower hp missiles, followed by closer missiles
                if (enemy.health == 1) { // insta kill low hp missile
                    Handler.rc.attackLocation(enemy.location);
                    return;
                }
                int dist = enemy.location.distanceSquaredTo(Handler.myLoc);
                if (enemy.health < minMissileHP || (enemy.health == minMissileHP && dist < minMissileDist)) {
                    minMissileDist = dist;
                    minMissileHP = (int) enemy.health;
                    minMissileLoc = enemy.location;
                }
            } else {
                if (enemy.health < Handler.typ.attackPower) { // insta kill low hp enemy
                    Handler.rc.attackLocation(enemy.location);
                    return;
                }
                int dist = enemy.location.distanceSquaredTo(Handler.myLoc);
                if (dist < minDist || (dist == minDist && enemy.health < minHP)) {
                    minDist = dist;
                    minHP = (int) enemy.health;
                    minLoc = enemy.location;
                }
            }
        }
        if (minDist == 999999) { // only missiles
            Handler.rc.attackLocation(minMissileLoc);
        } else {
            Handler.rc.attackLocation(minLoc);
        }
    }

    public static void tryAttackPrioritizeTowers() throws GameActionException {
        if (Handler.rc.canAttackLocation(Handler.enemyHQ)) {
            Handler.rc.attackLocation(Handler.enemyHQ);
            return;
        }
        for (int i = Handler.enemyTowers.length; --i >= 0;) {
            if (Handler.rc.canAttackLocation(Handler.enemyTowers[i])) {
                Handler.rc.attackLocation(Handler.enemyTowers[i]);
                return;
            }
        }

        tryAttackClosestButKillIfPossible(Handler.rc.senseNearbyRobots(Handler.typ.attackRadiusSquared, Handler.otherTeam));
    }




    public static void detectEnemyKiting() throws GameActionException {

    }

    //
    //
    // Movement Micro
    //
    //





























    //
    //
    // MICRO
    //
    //

    /**
     * This micro handles 1v1 fights
     *
     * @return
     * @throws GameActionException
     */
    public static boolean inCombatMicro() throws GameActionException {
        // First thing we check is if we are in combat.
        if (Handler.numEnemiesAttackingUs >= 1) {
            int maxAlliesAttackingEnemy = 0;
            for (int i = Handler.enemiesAttackingUs.length; --i >= 0;) {
                // int numAlliesAttackingEnemy = 1 +
                // numOtherAlliedSoldiersInAttackRange(attackableEnemies[i].location);
                // we deliberately include buildings in this count to encourage
                // our
                // soldiers to defend buildings:
                int numAlliesAttackingEnemy = 1 + numOtherAlliedSoldiersAndBuildingsInAttackRange(Handler.enemiesAttackingUs[i].location);
                if (numAlliesAttackingEnemy > maxAlliesAttackingEnemy)
                    maxAlliesAttackingEnemy = numAlliesAttackingEnemy;
            }
            if (Handler.numEnemiesAttackingUs == 1) {
                if (maxAlliesAttackingEnemy == 1) {
                    // 1v1 case
                    RobotInfo enemy = Handler.findAnAttackingEnemy(Handler.enemiesAttackingUs);
                    // Bad heurstic, fix (TODO)
                    boolean weAreWinning1v1 = Handler.health >= enemy.health;
                    if (weAreWinning1v1) {
                        Handler.rc.attackLocation(enemy.location);
                    } else {
                        retreat();
                    }
                } else {
                    // We have more units
                    RobotInfo enemy = findAnAttackingEnemy(Handler.enemiesAttackingUs);
                    Handler.rc.attackLocation(enemy.location);
                }
            } else if (Handler.numEnemiesAttackingUs > maxAlliesAttackingEnemy
                    && !guessIfFightIsWinning()) {
                retreat();
            } else {
                attackEnemy();
            }
            return true;
        } else {
            // No enemies detected combating against us, so we are currently not
            // in combat. In this case, we should help allies being attacked if
            // we can win the fight or run away if we can't. The last thing we
            // should do is take advantage of useless buildings that are not
            // doing anything.
            MapLocation closestEnemyUnit = closestNonConstructingUnit(
                    Handler.visibleEnemies, Handler.myLoc);
            if (closestEnemyUnit != null) {
                int numAlliesFighting = numOtherAlliedSoldiersAndBuildingsInAttackRange(closestEnemyUnit);
                if (numAlliesFighting > 0) {
                    tryMoveCloserToEnemy(closestEnemyUnit,
                            numAlliesFighting + 1, closestEnemyUnit != Handler.enemyHQ,
                            true);
                }
            }
            return false;
        }
    }






    //
    //
    // General helpers
    //
    //

    /**
     * TODO (kpeng94): implement this method
     * @return
     */
    public static boolean guessIfFightIsWinning() {
        return true;
    }

    public static boolean canKite() {
        // TODO Auto-generated method stub
        return false;
    }

}
