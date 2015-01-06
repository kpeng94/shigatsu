package genericPlayer;

import battlecode.common.*;

public abstract class Handler {
	public static RobotController rc;
	public static Rand rand;

	public Handler(RobotController rcon) {
		rc = rcon;
		rand = new Rand(rc.getID());
	}
	
	public void execute() {
		
	}
	
}
