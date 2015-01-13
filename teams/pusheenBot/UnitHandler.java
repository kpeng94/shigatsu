package pusheenBot;

import battlecode.common.*;

public abstract class UnitHandler extends Handler {

	protected static void initUnit(RobotController rcon) throws GameActionException {
		initGeneral(rcon);
	}
	
	protected static void executeUnit() {
		executeGeneral();
	}

}
