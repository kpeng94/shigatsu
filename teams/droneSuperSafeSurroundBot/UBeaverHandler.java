package droneSuperSafeSurroundBot;

import battlecode.common.*;

public class UBeaverHandler extends UnitHandler {
	private static RobotType[] buildTyps = {RobotType.MINERFACTORY, RobotType.HELIPAD, RobotType.SUPPLYDEPOT};
	private static int[] buildChans;
	
	public static int curBuildingChan;
	
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
		Spawner.HQxMod = myHQ.x % 2;
		Spawner.HQyMod = myHQ.y % 2;
		
		buildChans = new int[]{Comm.getMinerfactId(), Comm.getHeliId(), Comm.getSupplyId()};
	}

	protected static void execute() throws GameActionException {
		executeUnit();
		Count.incrementBuffer(Comm.getBeaverId());
		if (rc.isBuildingSomething()) {
			Count.incrementBuffer(curBuildingChan);
		} else {
			curBuildingChan = 0;
		}
		
		if (rc.isCoreReady()) {
			if (!tryBuild()) {
				if (myLoc.distanceSquaredTo(myHQ) > 15) {
					NavSimple.walkTowards(myLoc.directionTo(myHQ));
				} else {
					NavSimple.walkRandom();
				}
				// Random mining code
			}
		}
		Supply.spreadSupplies(Supply.DEFAULT_THRESHOLD);
		Distribution.spendBytecodesCalculating(Handler.rc.getSupplyLevel() > 50 ? 7500 : 2500);
	}
	
	protected static boolean tryBuild() throws GameActionException {
		Direction dir = Spawner.getBuildDirection(false);
		if (dir != Direction.NONE) {
			for (int i = 0; i < buildTyps.length; i++) {
				if (Count.getCount(buildChans[i]) < Count.getLimit(buildChans[i]) && rc.getTeamOre() >= buildTyps[i].oreCost) {
					Spawner.build(dir, buildTyps[i], buildChans[i]);
					curBuildingChan = buildChans[i];
					return true;
				}
			}
		}
		return false;
	}
	
}
