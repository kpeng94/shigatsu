package beaverMineDummy;

import battlecode.common.*;

public class UBeaverHandler extends UnitHandler {

	public static void loop(RobotController rcon) {
		try {
			init(rcon);
		} catch (Exception e) {
			// e.printStackTrace();
			System.out.println(typ + " Initialization Exception");
		}

		while (true) {
			try {
				execute();
			} catch (Exception e) {
				// e.printStackTrace();
				System.out.println(typ + " Execution Exception");
			}
			rc.yield(); // Yields to save remaining bytecodes
		}
	}

	protected static void init(RobotController rcon) {
		initUnit(rcon);
	}

	protected static void execute() throws GameActionException {
		executeUnit();
		if (rc.isCoreReady()) {
			int fate = rand.nextInt(1000);
			if (fate < 600) {
				rc.mine();
			} else if (fate < 900) {
			 	NavSimple.walkRandom();
			} else {
				NavSimple.walkTowards(myHQ.directionTo(rc.getLocation()));
			}
			
		}
	}
	
}
