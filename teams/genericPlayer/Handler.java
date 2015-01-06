package genericPlayer;

import battlecode.common.*;

public class Handler {
	public static RobotController rc;
	public static Rand rand;
	public static RobotType typ;

	protected static void initGeneral(RobotController rcon) {
		rc = rcon;
		typ = rc.getType();
		rand = new Rand(rc.getID());
	}

	protected static void executeGeneral() {

	}

}
