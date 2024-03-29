package ryoutabot;

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

    protected static void init(RobotController rcon) throws GameActionException {
        initStructure(rcon);
    }

    protected static void execute() throws GameActionException {
        executeStructure();
        Count.incrementBuffer(Comm.getTowerId());
        if (rc.isWeaponReady()) {
            Attack.tryAttackClosestButKillIfPossible(rc.senseNearbyRobots(typ.attackRadiusSquared, otherTeam));
        }
    }
	
}
