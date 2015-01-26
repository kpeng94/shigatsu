package qualifyingBotv2;

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

    public static MapLocation myLoc;
    public static MapLocation[] enemyTowers;

    protected static void initGeneral(RobotController rcon) throws GameActionException {
        rc = rcon;
        id = Comm.getId();
        rand = new Rand(rc.getID());

        typ = rc.getType();
        myTeam = rc.getTeam();
        otherTeam = myTeam.opponent();
        myHQ = rc.senseHQLocation();
        enemyHQ = rc.senseEnemyHQLocation();
        myLoc = rc.getLocation();

        if (typ == RobotType.HQ) {
            Comm.initComm();
        }
        Distribution.initTasks();
    }

    protected static void executeGeneral() {
        enemyTowers = rc.senseEnemyTowerLocations();
    }

}
