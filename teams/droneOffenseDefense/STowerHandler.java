package droneOffenseDefense;

import battlecode.common.*;

public class STowerHandler extends StructureHandler {
	public static RobotInfo[] inRangeEnemies;

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
		if (rc.isWeaponReady()) {
			inRangeEnemies = rc.senseNearbyRobots(typ.attackRadiusSquared, otherTeam);
			tryAttack();
		}
		supplyDrones();
	}
	
	protected static void tryAttack() throws GameActionException {
		if (inRangeEnemies.length > 0) {
			MapLocation minLoc = inRangeEnemies[0].location;
			int minRange = myLoc.distanceSquaredTo(minLoc);
			for (int i = inRangeEnemies.length - 1; i > 0; i--) { // Get minimum in array
				RobotInfo enemy = inRangeEnemies[i];
				MapLocation enemyLoc = enemy.location;
				int enemyRange = myLoc.distanceSquaredTo(enemyLoc);
				if (enemyRange < minRange) {
					minRange = enemyRange;
					minLoc = enemyLoc;
				}
			}
			
			rc.attackLocation(minLoc);
		}
	}

	protected static void supplyDrones() throws GameActionException {
		// Give supply to every drone in the vicinity if they need
		// some
		RobotInfo[] allies = rc.senseNearbyRobots(rc.getLocation(),
				15, myTeam);
		for (int i = 0; i < allies.length; i++) {
			if (allies[i].type == RobotType.DRONE
					&& allies[i].supplyLevel == 0) {
				rc.transferSupplies(
						Math.max((int) rc.getSupplyLevel(), 300),
						allies[i].location);
			}
		}

		// Supply drones if about to attack
		if (Clock.getRoundNum() % INTERWAVE_TIME == INTERWAVE_TIME - 1) {
			System.out.println(allies.length);
			for (int i = 0; i < allies.length; i++) {
				if (allies[i].type == RobotType.DRONE
						&& allies[i].supplyLevel == 0) {
					rc.transferSupplies(Math.min(
							(int) rc.getSupplyLevel(), 500),
							allies[i].location);
				}
			}
		}
	}
}
