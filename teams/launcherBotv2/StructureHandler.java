package kevintestrashbot;

import battlecode.common.*;

public class StructureHandler extends Handler {
	
	protected static void initStructure(RobotController rcon) throws GameActionException {
		initGeneral(rcon);
		if (id == 0) {
			Comm.initComm();
		}
	}
	
	protected static void executeStructure() {
		executeGeneral();
	}
	
}
