package launcherBotv3;

import battlecode.common.*;

public class Handler {
    public static RobotController rc;
    public static int id;
    public static Rand rand;

    public static RobotType typ;
    public static Team myTeam;
    public static Team otherTeam;
    public static MapLocation myHQ;
    public static MapLocation enemyHQ;
    public static Direction myHQToEnemyHQ;
    public static Direction dirFromHQ;
    public static int distFromHQ;

    public static double health;
    public static MapLocation myLoc;
    public static MapLocation[] enemyTowers;
    public static Direction[] directions = { Direction.NORTH,
            Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
            Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST,
            Direction.NORTH_WEST };

    // This represents attacking robots that we can see.
    public static RobotInfo[] visibleEnemies;
    public static int numVisibleEnemyAttackers;

    // This represents attacking robots for which we are in their attack radius.
    public static RobotInfo[] enemiesAttackingUs;
    public static int numEnemiesAttackingUs;

    // This represents robots that we can attack
    public static RobotInfo[] attackableEnemies;
    public static int numAttackableEnemies;

    public static int prevOre = 0;

    public static double oreAmount = 0;

    protected static void initGeneral(RobotController rcon)
            throws GameActionException {
        rc = rcon;
        id = Comm.getId();
        rand = new Rand(rc.getID());

        typ = rc.getType();
        myTeam = rc.getTeam();
        otherTeam = myTeam.opponent();
        myHQ = rc.senseHQLocation();
        enemyHQ = rc.senseEnemyHQLocation();
        myHQToEnemyHQ = myHQ.directionTo(enemyHQ);
        if (typ == RobotType.HQ) {
            Comm.initComm();
        }
        Distribution.initTasks();
    }

    protected static void executeGeneral() {
        myLoc = rc.getLocation();
        enemyTowers = rc.senseEnemyTowerLocations();
    }



    // This method will attempt to move in Direction d (or as close to it as
    // possible)
    static void tryMove(Direction d) throws GameActionException {
        int offsetIndex = 0;
        int[] offsets = { 0, 1, -1, 2, -2 };
        int dirint = Utils.directionToInt(d);
        while (offsetIndex < 5
                && !rc.canMove(directions[(dirint + offsets[offsetIndex] + 8) % 8])) {
            offsetIndex++;
        }
        if (offsetIndex < 5) {
            rc.move(directions[(dirint + offsets[offsetIndex] + 8) % 8]);
        }
    }



    /**
     * Tries to move closer to enemy location to increase exposure of enemies to
     * our units.
     *
     * @param location
     *            Enemy location
     * @param maxEnemyExposure
     * @param avoidHQ
     *            Should I move to avoid HQ
     * @param avoidOtherTowers
     *            Should I move to avoid other towers
     * @return
     * @throws GameActionException
     */
    public static boolean tryMoveCloserToEnemy(MapLocation location,
            int maxEnemyExposure, boolean avoidHQ, boolean avoidOtherTowers)
            throws GameActionException {

        Direction toEnemy = myLoc.directionTo(location);
        Direction[] tryDirs = new Direction[] {
                toEnemy.rotateRight().rotateRight(),
                toEnemy.rotateLeft().rotateLeft(), toEnemy.rotateRight(),
                toEnemy.rotateLeft(), toEnemy };
        for (int i = tryDirs.length; --i >= 0;) {
            Direction tryDir = tryDirs[i];
            if (!rc.canMove(tryDir))
                continue;
            if (Utils.inRangeOfOtherTowers(tryDir, location) && avoidOtherTowers)
                continue;
            int newX = myLoc.x + tryDir.dx;
            int newY = myLoc.y + tryDir.dy;
            int deltaX = Math.abs(newX - location.x)
                    - Math.abs(myLoc.x - location.x);
            int deltaY = Math.abs(newY - location.y)
                    - Math.abs(myLoc.y - location.y);
            MapLocation newLoc = new MapLocation(newX, newY);

            // Don't move to spaces that are strictly farther away
            if (deltaY >= 0 && deltaX >= 0) {
                continue;
            }
            if (Utils.inRangeOfEnemyHQ(myLoc) && avoidHQ)
                continue;
            rc.move(tryDir);
            return true;
        }
        return false;
    }

    private static void retreat() {
        // TODO Auto-generated method stub

    }

    public static void attackEnemy() throws GameActionException {
        // TODO Auto-generated method stub
        RobotInfo[] enemiesInMyAttackRadius = rc.senseNearbyRobots(myLoc,
                typ.attackRadiusSquared, otherTeam);
        RobotInfo target = chooseTarget(enemiesInMyAttackRadius);
        rc.attackLocation(target.location);

    }

    private static RobotInfo chooseTarget(RobotInfo[] enemiesInMyAttackRadius) {
        // TODO Auto-generated method stub
        return null;
    }



    public static RobotInfo findAnAttackingEnemy(RobotInfo[] infos) {
        for (int i = infos.length; i-- > 0;) {
            RobotInfo info = infos[i];
            if (info.type.canAttack() && info.buildingLocation == null)
                return info;
        }
        return null;
    }



}
