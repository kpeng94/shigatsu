package launcherDroneShieldv2;

import battlecode.common.*;

public abstract class UnitHandler extends Handler {

	protected static void initUnit(RobotController rcon) throws GameActionException {
		initGeneral(rcon);
	}
	
	protected static void executeUnit() throws GameActionException {
		executeGeneral();
		Count.incrementBuffer(getRobotId(typ));
	}

	public static int getRobotId(RobotType type) throws GameActionException {
		if (type == RobotType.BASHER)
			return Comm.getBasherId();
		else if (type == RobotType.BEAVER)
			return Comm.getBeaverId();
		else if (type == RobotType.COMMANDER)
			return Comm.getCommanderId();
		else if (type == RobotType.COMPUTER)
			return Comm.getComputerId();
		else if (type == RobotType.DRONE)
			return Comm.getDroneId();
		else if (type == RobotType.LAUNCHER)
			return Comm.getLauncherId();
		else if (type == RobotType.MINER)
			return Comm.getMinerId();
		else if (type == RobotType.MISSILE)
			return Comm.getMissileId();
		else if (type == RobotType.SOLDIER)
			return Comm.getSoldierId();
		else if (type == RobotType.TANK)
			return Comm.getTankId();
		else
			return -1;
	}
}
