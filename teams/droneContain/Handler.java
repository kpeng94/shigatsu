package droneContain;

import battlecode.common.*;

public class Handler {
	
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

}
