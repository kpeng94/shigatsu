package genericPlayer;

import battlecode.common.*;

public abstract class UnitHandler extends Handler {

	protected static void initUnit(RobotController rcon) {
		initGeneral(rcon);
	}
	
	protected static void executeUnit() {
		executeGeneral();
	}

}
