package launcherDroneShieldv2;

import battlecode.common.*;

public class StructureHandler extends Handler {
	
	protected static void initStructure(RobotController rcon) throws GameActionException {
		initGeneral(rcon);
	}
	
	protected static void executeStructure() throws GameActionException {
		executeGeneral();
		Count.incrementBuffer(getRobotId(typ));
	}
	
	public static int getRobotId(RobotType type) throws GameActionException {
		if (type == RobotType.HQ)
			return Comm.getHqId();
		else if (type == RobotType.TOWER)
			return Comm.getTowerId();
		else if (type == RobotType.AEROSPACELAB)
			return Comm.getAeroId();
		else if (type == RobotType.BARRACKS)
			return Comm.getBarrackId();
		else if (type == RobotType.HANDWASHSTATION)
			return Comm.getHandwashId();
		else if (type == RobotType.HELIPAD)
			return Comm.getHeliId();
		else if (type == RobotType.MINERFACTORY)
			return Comm.getMinerfactId();
		else if (type == RobotType.SUPPLYDEPOT)
			return Comm.getSupplyId();
		else if (type == RobotType.TANKFACTORY)
			return Comm.getTankfactId();
		else if (type == RobotType.TECHNOLOGYINSTITUTE)
			return Comm.getTechId();
		else if (type == RobotType.TRAININGFIELD)
			return Comm.getTrainingId();
		else
			return -1;
	}
	
}
