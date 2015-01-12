package droneOffenseDefensev2;

import battlecode.common.*;

public class Handler {
	
	// Thresholds
	public static final int MINERFACTORY_THRESHOLD = 3;
	public static final int MINER_THRESHOLD = 10;
	public static final int ORE_THRESHOLD = 10;
	public static final int ORE_THRESHOLD_MINER = 2;
	public static final int HELIPAD_THRESHOLD = 2;
	
	public static RobotController rc;
	public static Rand rand;
	public static RobotType typ;
	public static Team myTeam;
	public static Team otherTeam;
	public static MapLocation myHQ;
	public static MapLocation enemyHQ;
	
	public static MapLocation myLoc;

	protected static void initGeneral(RobotController rcon) {
		rc = rcon;
		rand = new Rand(rc.getID());
		typ = rc.getType();
		myTeam = rc.getTeam();
		otherTeam = myTeam.opponent();
		myHQ = rc.senseHQLocation();
		enemyHQ = rc.senseEnemyHQLocation();
	}

	protected static void executeGeneral() {
		myLoc = rc.getLocation();
	}
	
	protected static boolean isBuilding(RobotInfo robot) {
		return robot.type == RobotType.AEROSPACELAB || robot.type == RobotType.BARRACKS 
				|| robot.type == RobotType.HANDWASHSTATION || robot.type == RobotType.HELIPAD
				|| robot.type == RobotType.HQ || robot.type == RobotType.MINERFACTORY
				|| robot.type == RobotType.SUPPLYDEPOT || robot.type == RobotType.TANKFACTORY
				|| robot.type == RobotType.TECHNOLOGYINSTITUTE || robot.type == RobotType.TOWER
				|| robot.type == RobotType.TRAININGFIELD;
	}

}
