package genericPlayer;

import battlecode.common.*;

public class Handler {
	public static RobotController rc;
	public static RobotType typ;

	protected static void initGeneral(RobotController rcon) {
		rc = rcon;
		typ = rc.getType();
	}

	protected static void executeGeneral() {

	}

}
