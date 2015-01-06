package DefenseBot;

import battlecode.common.*;

public class Handler {
	public static RobotController rc;
	public static Rand rand;
	public static RobotType typ;
	public static Team myTeam;
	public static Team otherTeam;
	
	public static MapLocation myLoc;

	protected static void initGeneral(RobotController rcon) {
		rc = rcon;
		rand = new Rand(rc.getID());
		typ = rc.getType();
		myTeam = rc.getTeam();
		otherTeam = myTeam.opponent();
	}

	protected static void executeGeneral() {
		myLoc = rc.getLocation();
	}

}
