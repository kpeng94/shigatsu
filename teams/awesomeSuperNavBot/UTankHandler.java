package awesomeSuperNavBot;

import battlecode.common.*;

public class UTankHandler extends UnitHandler {
	public static final int ATTACK_THRESHOLD = 10;
	public static MapLocation rallyPoint = null;
	
	private static boolean isAttacking;
	private static boolean newRallyFound;

	public static void loop(RobotController rcon) {
		try {
			init(rcon);
		} catch (Exception e) {
			e.printStackTrace();
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

	protected static void init(RobotController rcon) throws GameActionException {
		initUnit(rcon);
	}

	protected static void execute() throws GameActionException {
		executeUnit();
	}
	
}
