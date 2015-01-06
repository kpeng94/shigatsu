package genericPlayer;

import battlecode.common.*;

public abstract class Handler {
	public static RobotController rc;

	public Handler(RobotController rcon) {
		rc = rcon;
	}
	
	public void execute() {
		
	}
	
}
