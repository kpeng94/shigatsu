package launcherDroneShieldv2;

import battlecode.common.*;

public class Rally {
	
	private static final int RALLY_POINT_SIZE = 3;
	private static final int POINT_ACTIVE_OFFSET = 0;
	private static final int POINT_X_OFFSET = 1;
	private static final int POINT_Y_OFFSET = 2;
	
	public static void deactivate(int rallyNum) throws GameActionException {
		Comm.writeBlock(getRallyId(), RALLY_POINT_SIZE * rallyNum + POINT_ACTIVE_OFFSET, 0);
	}
	
	public static void set(int rallyNum, MapLocation point) throws GameActionException {
		Comm.writeBlock(getRallyId(), RALLY_POINT_SIZE * rallyNum + POINT_ACTIVE_OFFSET, 1);
		Comm.writeBlock(getRallyId(), RALLY_POINT_SIZE * rallyNum + POINT_X_OFFSET, point.x);
		Comm.writeBlock(getRallyId(), RALLY_POINT_SIZE * rallyNum + POINT_Y_OFFSET, point.y);
	}
	
	public static MapLocation get(int rallyNum) throws GameActionException {
		if (Comm.readBlock(getRallyId(),  RALLY_POINT_SIZE * rallyNum + POINT_ACTIVE_OFFSET) == 1) {
			return new MapLocation(Comm.readBlock(
					getRallyId(), RALLY_POINT_SIZE * rallyNum + POINT_X_OFFSET), Comm.readBlock(
					getRallyId(), RALLY_POINT_SIZE * rallyNum + POINT_Y_OFFSET));
		} else {
			return null;
		}
	}

	/*-------------------------------- COMM FUNCTIONS --------------------------------*/
	
	public static final int RALLY_BLOCK = 199;
	public static int rallyBlockId = 0;

	/**
	 * Returns the block id of the dedicated scout block Creates a scout block
	 * if it was not previously
	 */
	public static int getRallyId() throws GameActionException {
		if (rallyBlockId == 0) {
			rallyBlockId = Handler.rc.readBroadcast(RALLY_BLOCK);
			if (rallyBlockId == 0) {
				rallyBlockId = Comm.requestBlock(true);
				Handler.rc.broadcast(RALLY_BLOCK, rallyBlockId);
			}
		}
		return rallyBlockId;
	}
}
