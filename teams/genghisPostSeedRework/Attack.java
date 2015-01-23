package genghisPostSeedRework;

import battlecode.common.*;

public class Attack {

    public static void tryAttackClosestButKillIfPossible(RobotInfo[] enemies) throws GameActionException {
        if (enemies.length == 0)
            return;

        int minDist = 999999;
        int minHP = 999999;
        MapLocation minLoc = null;

        int minMissileDist = 999999;
        int minMissileHP = 999999;
        MapLocation minMissileLoc = null;
        for (int i = enemies.length; --i >= 0;) {
            RobotInfo enemy = enemies[i];
            if (enemies[i].type == RobotType.MISSILE) { // Focus lower hp
                                                        // missiles, followed by
                                                        // closer missiles
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
                if (enemy.health < Handler.typ.attackPower) { // insta kill low
                                                              // hp enemy
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

    public static MapLocation getClosestTower() {
        int minDist = 999999;
        MapLocation minTower = null;
        for (int i = Handler.enemyTowers.length; --i >= 0;) {
            MapLocation tower = Handler.enemyTowers[i];
            int towerDist = Handler.myLoc.distanceSquaredTo(tower);
            if (towerDist < minDist) {
                minDist = towerDist;
                minTower = tower;
            }
        }
        return minTower;
    }

    public static RobotInfo getClosestEnemy(RobotInfo[] enemies) {
        int minDist = 999999;
        RobotInfo closestEnemy = null;
        for (int i = enemies.length; --i >= 0;) {
            MapLocation enemyLoc = enemies[i].location;
            int enemyDist = Handler.myLoc.distanceSquaredTo(enemyLoc);
            if (enemyDist < minDist) {
                minDist = enemyDist;
                closestEnemy = enemies[i];
            }
        }
        return closestEnemy;
    }

    /**
     * Returns true if location is in an enemy tower's attack range. 
     * Naive implementation.
     * @param loc
     * @return
     */
    public static boolean isInEnemyTowerRange(MapLocation loc) {
        for (int i = Handler.enemyTowers.length; --i >= 0;) {
            if(loc.distanceSquaredTo(Handler.enemyTowers[i]) <= 24)
                return true;
        }
        return false;
    }

}
