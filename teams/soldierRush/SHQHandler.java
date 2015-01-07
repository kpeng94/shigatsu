package soldierRush;

import battlecode.common.*;

public class SHQHandler extends StructureHandler {

	static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	static int myRange = 35;
	
	public static void loop(RobotController rcon) {
		try {
			init(rcon);
		} catch (Exception e) {
			System.out.println(typ + " Initialization Exception");
		}

		while (true) {
			try {
				execute();
			} catch (Exception e) {
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
		if(myRange == 35)
			updateAttackRange();
			
		int fate = rand.nextInt(10000);
		RobotInfo[] myRobots = rc.senseNearbyRobots(999999, rc.getTeam());
		int numSoldiers = 0;
		int numBashers = 0;
		int numBeavers = 0;
		int numBarracks = 0;
		for (RobotInfo r : myRobots) {
			RobotType type = r.type;
			if (type == RobotType.SOLDIER) {
				numSoldiers++;
			} else if (type == RobotType.BASHER) {
				numBashers++;
			} else if (type == RobotType.BEAVER) {
				numBeavers++;
			} else if (type == RobotType.BARRACKS) {
				numBarracks++;
			}
		}
		rc.broadcast(0, numBeavers);
		rc.broadcast(1, numSoldiers);
		rc.broadcast(2, numBashers);
		rc.broadcast(100, numBarracks);

		if (rc.isWeaponReady()) {
			attackSomething();
		}

		if (rc.isCoreReady() && rc.getTeamOre() >= 100
				&& fate < Math.pow(1.2, 12 - numBeavers) * 10000) {
			trySpawn(directions[rand.nextInt(8)], RobotType.BEAVER);
		}
	}
	
	private static void updateAttackRange(){
		int numTowersLeft = rc.senseTowerLocations().length;
		if(numTowersLeft < 2)
			myRange = 24;
	}
	
    // This method will attempt to spawn in the given direction (or as close to it as possible)
	static void trySpawn(Direction d, RobotType type) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = {0,1,-1,2,-2,3,-3,4};
		int dirint = directionToInt(d);
		while (offsetIndex < 8 && !rc.canSpawn(directions[(dirint+offsets[offsetIndex]+8)%8], type)) {
			offsetIndex++;
		}
		if (offsetIndex < 8) {
			rc.spawn(directions[(dirint+offsets[offsetIndex]+8)%8], type);
		}
	}
	
    // This method will attack an enemy in sight, if there is one
	static void attackSomething() throws GameActionException {
		RobotInfo[] enemies = rc.senseNearbyRobots(myRange, rc.getTeam().opponent());
		if (enemies.length > 0) {
			rc.attackLocation(enemies[0].location);
		}
	}
	
	static int directionToInt(Direction d) {
		switch(d) {
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
