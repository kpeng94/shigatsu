package soldierRush;

import battlecode.common.*;

public class STowerHandler extends StructureHandler {
	
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
		initStructure(rcon);
	}

	protected static void execute() throws GameActionException {
		executeStructure();
		if(rc.isWeaponReady()){
			attackSomething();
		}
	}
	
	// This method will attack an enemy in sight, if there is one
	static void attackSomething() throws GameActionException {
		RobotInfo[] enemies = rc.senseNearbyRobots(typ.attackRadiusSquared, rc.getTeam().opponent());
		if (enemies.length > 0) {
			rc.attackLocation(enemies[0].location);
		}
	}
	
}
