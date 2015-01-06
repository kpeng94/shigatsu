package DefenseBot;

import battlecode.common.*;

public class SHQHandler extends StructureHandler {
	public static final int splashRange = 52;
	
	public static MapLocation[] towerLocs;
	public static int towerNum;

	public static int range;
	public static boolean splash;
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

	protected static void execute() {
		executeStructure();
		updateTowers();
		if (rc.isWeaponReady()) {
			calculateAttackable();
			tryAttack();
		}
	}
	
	protected static void updateTowers() {
		towerLocs = rc.senseTowerLocations();
		towerNum = towerLocs.length;
	}
	
	protected static void calculateAttackable() {
		if (towerNum >= 5) {
			range = GameConstants.HQ_BUFFED_ATTACK_RADIUS_SQUARED;
			splash = true;
			inRangeEnemies = rc.senseNearbyRobots(splashRange, otherTeam);
		} else {
			range = typ.attackRadiusSquared;
			splash = false;
			inRangeEnemies = rc.senseNearbyRobots(range, otherTeam);
		}
	}
	
	protected static void tryAttack() {
		if (inRangeEnemies.length > 0) {
			for (int i = inRangeEnemies.length - 1; i >= 0; i--) {
				RobotInfo enemy = inRangeEnemies[i];
			}
		}
	}

}
