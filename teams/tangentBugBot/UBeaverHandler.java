package tangentBugBot;

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
				e.printStackTrace();
				System.out.println(typ + " Execution Exception");
			}
			rc.yield(); // Yields to save remaining bytecodes
		}
	}

	protected static void init(RobotController rcon) {
		initUnit(rcon);
		NavTangentBug.setDest(enemyHQ);
	}

	protected static void execute() throws GameActionException {
		executeUnit();
		NavTangentBug.calculate(2500);
		if (rc.isCoreReady()) {
			Direction nextMove = NavTangentBug.getNextMove();
			if (nextMove != Direction.NONE) {
				NavSimple.walkTowardsDirected(nextMove);
			}
//			Direction tangentDir = NavDumbTangent.dumbTangentDir(false);
//			if (tangentDir != Direction.NONE) {
//				NavDumbTangent.executeDumbTangent(tangentDir);
//			}
		}
	}
	
}
