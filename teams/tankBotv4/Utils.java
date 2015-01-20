package tankBotv4;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Utils {

    //
    //
    //  Updates
    //
    //

    /**
     * Requires typ, rc, myLoc to be initialized and correct before this
     * function is called. Sets both visible enemies in my range and attacking
     * enemies in my range
     */
    public static void updateEnemyInfo() {
        Handler.attackableEnemies = Handler.rc.senseNearbyRobots(Handler.typ.attackRadiusSquared, Handler.otherTeam);
        Handler.visibleEnemies = Handler.rc.senseNearbyRobots(Handler.typ.sensorRadiusSquared, Handler.otherTeam);
        Handler.numVisibleEnemyAttackers = 0;
        for (int i = Handler.visibleEnemies.length; --i >= 0;) {
            if (Handler.visibleEnemies[i].type.canAttack()
                    && Handler.visibleEnemies[i].buildingLocation == null) {
                Handler.numVisibleEnemyAttackers++;
            }
        }

        Handler.numEnemiesAttackingUs = 0;
        if (Handler.visibleEnemies.length > 0) {
            int[] visibleAttackArray = new int[Handler.visibleEnemies.length];
            for (int i = Handler.visibleEnemies.length; --i >= 0;) {
                // Conditions that we check:
                // The enemy can attack, we're within the enemy's attack range,
                // and it is not building (hence buildingLocation is null)
                if (Handler.visibleEnemies[i].type.canAttack()
                        && Handler.myLoc.distanceSquaredTo(Handler.visibleEnemies[i].location) <= Handler.visibleEnemies[i].type.attackRadiusSquared
                        && Handler.visibleEnemies[i].buildingLocation == null) {
                    Handler.numEnemiesAttackingUs++;
                    visibleAttackArray[i] = 1;
                }
            }
            Handler.enemiesAttackingUs = new RobotInfo[Handler.numEnemiesAttackingUs];
            int attackIndex = Handler.numEnemiesAttackingUs - 1;
            for (int i = Handler.visibleEnemies.length; --i >= 0;) {
                if (visibleAttackArray[i] == 1) {
                    Handler.enemiesAttackingUs[attackIndex] = Handler.visibleEnemies[i];
                    attackIndex--;
                }
            }
        } else {
            // Empty array for no enemies attacking us
            Handler.enemiesAttackingUs = new RobotInfo[0];
        }
    }

    public static void updateOre() throws GameActionException {
        int spent = Handler.rc.readBroadcast(Comm.SPENT_ORE_BUFFER_CHAN);
        int gained = (int) (Handler.rc.getTeamOre() - Handler.prevOre + spent);
        Handler.prevOre = (int) Handler.rc.getTeamOre();
        Handler.rc.broadcast(Comm.SPENT_ORE_BUFFER_CHAN, 0);
        Handler.rc.broadcast(Comm.PREV_ORE_CHAN, Handler.prevOre);
        Handler.rc.broadcast(Comm.SPENT_ORE_CHAN, spent);
        Handler.rc.broadcast(Comm.GAINED_ORE_CHAN, gained);
    }






    /**
     * Checks whether you are in the range of a tower (other than the one at
     * location) after moving in the direction tryDir.
     *
     * REQUIRES that Handler.enemyTowers is properly updated.
     *
     * @param tryDir
     * @param location
     * @return
     */
    public static boolean inRangeOfOtherTowers(Direction tryDir,
            MapLocation location) {
        MapLocation destination = Handler.myLoc.add(tryDir);
        for (int i = Handler.enemyTowers.length; --i >= 0;) {
            if (Handler.enemyTowers[i] == location)
                continue;
            if (Handler.enemyTowers[i].distanceSquaredTo(destination) <= RobotType.TOWER.attackRadiusSquared)
                return true;
        }
        if (destination.distanceSquaredTo(Handler.enemyHQ) <= RobotType.HQ.attackRadiusSquared
                && Handler.enemyTowers.length < 2 && location != Handler.enemyHQ)
            return true;
        if (destination.distanceSquaredTo(Handler.enemyHQ) <= GameConstants.HQ_BUFFED_ATTACK_RADIUS_SQUARED
                && Handler.enemyTowers.length >= 2 && location != Handler.enemyHQ)
            return true;
        return false;
    }


    /**
     * Helper function to check whether the given location is in range of the enemy HQ.
     * REQUIRES that Handler.enemyTowers and Handler.enemyHQ are properly updated.
     *
     * @param location
     *            Location to check
     * @return true if location is in the range of the enemy HQ, false otherwise
     */
    public static boolean inRangeOfEnemyHQ(MapLocation location) {
        int distance = Handler.enemyHQ.distanceSquaredTo(location);
        int enemyHQAttackRadius;
        if (Handler.enemyTowers.length >= 2) {
            enemyHQAttackRadius = GameConstants.HQ_BUFFED_ATTACK_RADIUS_SQUARED;
        } else {
            enemyHQAttackRadius = RobotType.HQ.attackRadiusSquared;
        }
        if (distance <= enemyHQAttackRadius) {
            return true;
        }
        return false;
    }


    /**
     * Converts a direction to an integer equivalent
     * @param d Direction
     * @return integer equivalent
     */
    public static int directionToInt(Direction d) {
        switch (d) {
            case NORTH:
                return 0;
            case NORTH_EAST:
                return 1;
            case EAST:
                return 2;
            case SOUTH_EAST:
                return 3;
            case SOUTH:
                return 4;
            case SOUTH_WEST:
                return 5;
            case WEST:
                return 6;
            case NORTH_WEST:
                return 7;
            default:
                return -1;
        }
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
//    public static void calculateLooseBoundsOnMap() {
//        hqDistX = myHQ.x > enemyHQ.x ? (myHQ.x - enemyHQ.x)
//                : (enemyHQ.x - myHQ.x);
//        hqDistY = myHQ.y > enemyHQ.y ? (myHQ.y - enemyHQ.y)
//                : (enemyHQ.y - myHQ.y);
//        MapLocation[] towerLocations = rc.senseTowerLocations();
//        MapLocation[] enemyTowerLocations = rc.senseEnemyTowerLocations();
//        int minTowerX = Integer.MAX_VALUE;
//        int minTowerY = Integer.MAX_VALUE;
//        int maxTowerX = Integer.MIN_VALUE;
//        int maxTowerY = Integer.MIN_VALUE;
//        int minETowerX = Integer.MAX_VALUE;
//        int minETowerY = Integer.MAX_VALUE;
//        int maxETowerX = Integer.MIN_VALUE;
//        int maxETowerY = Integer.MIN_VALUE;
//        for (MapLocation towerLocation : towerLocations) {
//            if (towerLocation.x < minTowerX) {
//                minTowerX = towerLocation.x;
//            }
//            if (towerLocation.x > maxTowerX) {
//                maxTowerX = towerLocation.x;
//            }
//            if (towerLocation.y < minTowerY) {
//                minTowerY = towerLocation.y;
//            }
//            if (towerLocation.y > maxTowerY) {
//                maxTowerY = towerLocation.y;
//            }
//        }
//        for (MapLocation towerLocation : enemyTowerLocations) {
//            if (towerLocation.x < minETowerX) {
//                minETowerX = towerLocation.x;
//            }
//            if (towerLocation.x > maxETowerX) {
//                maxETowerX = towerLocation.x;
//            }
//            if (towerLocation.y < minETowerY) {
//                minETowerY = towerLocation.y;
//            }
//            if (towerLocation.y > maxETowerY) {
//                maxETowerY = towerLocation.y;
//            }
//        }
//
//        // The width of the map must be at least as big as the distance between
//        // the largest X and smallest X of opposing sides
//        int dx1 = maxTowerX - minETowerX;
//        int dx2 = maxETowerX - minETowerX;
//        maxTowerDistX = (dx1 - dx2 > 0) ? dx1 : dx2;
//        int dy1 = maxTowerY - minETowerY;
//        int dy2 = maxETowerY - minETowerY;
//        maxTowerDistY = (dy1 - dy2 > 0) ? dy1 : dy2;
//    }

}
