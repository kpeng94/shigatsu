package launcherDroneShield;

import battlecode.common.*;

public class UBeaverHandler extends UnitHandler {
	private static RobotType[] buildTyps = {RobotType.MINERFACTORY, RobotType.HELIPAD, RobotType.AEROSPACELAB, RobotType.SUPPLYDEPOT};
	private static int[] buildChans;
	
	public static RobotType curBuilding;
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
		
		buildChans = new int[]{Comm.getMinerfactId(), Comm.getHeliId(), Comm.getAeroId(), Comm.getSupplyId()};

		BuildRadial.init();
	}

	protected static void execute() throws GameActionException {
		executeUnit();
		if (rc.isBuildingSomething()) {
			Count.incrementBuffer(curBuildingChan);
		} else {
			curBuildingChan = 0;
		}
		
		if (rc.isCoreReady()) {
			if (curBuildingChan > 0 && Count.getCount(curBuildingChan) < Count.getLimit(curBuildingChan)) {
				BuildRadial.build(curBuilding);
			} else {
				for (int i = 0; i < buildTyps.length; i++) {
					if (Count.getCount(buildChans[i]) < Count.getLimit(buildChans[i])) {
						curBuildingChan = buildChans[i];
						curBuilding = buildTyps[i];
						BuildRadial.build(curBuilding);
						break;
					}
				}
			}
		}
		
		Supply.spreadSupplies(Supply.DEFAULT_THRESHOLD);
	}
}
