package launcherBotv2;

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

    public static MapLocation myLoc;
    public static MapLocation[] enemyTowers;
    public static Direction[] directions = { Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH,
            Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST };

    protected static void initGeneral(RobotController rcon) throws GameActionException {
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

    static int directionToInt(Direction d) {
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

    // This method will attempt to move in Direction d (or as close to it as
    // possible)
    static void tryMove(Direction d) throws GameActionException {
        int offsetIndex = 0;
        int[] offsets = { 0, 1, -1, 2, -2 };
        int dirint = directionToInt(d);
        while (offsetIndex < 5 && !rc.canMove(directions[(dirint + offsets[offsetIndex] + 8) % 8])) {
            offsetIndex++;
        }
        if (offsetIndex < 5) {
            rc.move(directions[(dirint + offsets[offsetIndex] + 8) % 8]);
        }
    }

}
