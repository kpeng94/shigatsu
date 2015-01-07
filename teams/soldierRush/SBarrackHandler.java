package soldierRush;

import battlecode.common.*;

public class SBarrackHandler extends StructureHandler {

	static Direction[] directions = { Direction.NORTH, Direction.NORTH_EAST,
		Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH,
		Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST };
	
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
		
		int fate = rand.nextAnd(1023);

		// get information broadcasted by the HQ
		int numBeavers = rc.readBroadcast(0);
		int numSoldiers = rc.readBroadcast(1);
		int numBashers = rc.readBroadcast(2);

		if (rc.isCoreReady()
				&& rc.getTeamOre() >= 60
				&& fate < Math.pow(1.2, 15 - numSoldiers
						- numBashers + numBeavers) * 10000) {
			if (rc.getTeamOre() > 80 && fate % 2 == 0) {
				trySpawn(directions[rand.nextAnd(7)],
						RobotType.BASHER);
			} else {
				trySpawn(directions[rand.nextAnd(7)],
						RobotType.SOLDIER);
			}
		}
	}
	
	// This method will attempt to spawn in the given direction (or as close to
	// it as possible)
	static void trySpawn(Direction d, RobotType type)
			throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = { 0, 1, -1, 2, -2, 3, -3, 4 };
		int dirint = directionToInt(d);
		boolean blocked = false;
		while (offsetIndex < 8
				&& !rc.canSpawn(
						directions[(dirint + offsets[offsetIndex] + 8) % 8],
						type)) {
			offsetIndex++;
		}
		if (offsetIndex < 8) {
			rc.spawn(directions[(dirint + offsets[offsetIndex] + 8) % 8], type);
		}
	}
	
	static int directionToInt(Direction d) {
		switch (d) {
		case NORTH:
			return 0;
		case NORTH_EAST:
			return 1;
		case EAST:
			return 2;
		case SOUTH_EAST:
			return 3;
		case SOUTH:
			return 4;
		case SOUTH_WEST:
			return 5;
		case WEST:
			return 6;
		case NORTH_WEST:
			return 7;
		default:
			return -1;
		}
	}
}
