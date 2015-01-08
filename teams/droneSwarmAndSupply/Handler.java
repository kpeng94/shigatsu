package droneSwarmAndSupply;

import battlecode.common.*;

public class Handler {
	
	// Channel constants
	public static final int NUM_BEAVER_CHANNEL = 0;
	public static final int NUM_DRONE_CHANNEL = 1;
	public static final int NUM_COURIER_CHANNEL = 3;
	public static final int NUM_HELIPAD_CHANNEL = 100;
	public static final int WAVE_NUMBER_CHANNEL = 2;
	public static final int DRONE_PROMOTION_CHANNEL = 10;
	public static final int NEXT_RALLY_X_CHANNEL = 11;
	public static final int NEXT_RALLY_Y_CHANNEL = 12;
	public static final int NEXT_TARGET_X_CHANNEL = 13;
	public static final int NEXT_TARGET_Y_CHANNEL = 15;

	public static final int INTERWAVE_TIME = 400;
	
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

}
