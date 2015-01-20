package tankBotv4;

import battlecode.common.*;

public class Count {
	public static final int COUNT_BUFFER_START = 1;
	public static final int COUNT_BUFFER_END = 2;
	public static final int COUNT_LIMIT = 3;

	public static int getCount(int blockId) throws GameActionException {
		return Comm.readBlock(blockId, COUNT_BUFFER_START + (Clock.getRoundNum() % 2));
	}

	public static int getLimit(int blockId) throws GameActionException {
		return Comm.readBlock(blockId, COUNT_LIMIT);
	}

	public static void setLimit(int blockId, int limit) throws GameActionException {
		Comm.writeBlock(blockId, COUNT_LIMIT, limit);
	}

	public static void incrementBuffer(int blockId) throws GameActionException {
		Comm.writeBlock(blockId, COUNT_BUFFER_END - (Clock.getRoundNum() % 2), 1 + Comm.readBlock(blockId, COUNT_BUFFER_END - (Clock.getRoundNum() % 2)));
	}

	public static void incrementBoth(int blockId) throws GameActionException {
		Comm.writeBlock(blockId, COUNT_BUFFER_START + (Clock.getRoundNum() % 2), 1 + Comm.readBlock(blockId, COUNT_BUFFER_START + (Clock.getRoundNum() % 2)));
		Comm.writeBlock(blockId, COUNT_BUFFER_END - (Clock.getRoundNum() % 2), 1 + Comm.readBlock(blockId, COUNT_BUFFER_END - (Clock.getRoundNum() % 2)));
	}

	public static void resetBuffer(int blockId) throws GameActionException {
		Comm.writeBlock(blockId, COUNT_BUFFER_END - (Clock.getRoundNum() % 2), 0);
	}

	public static void incrementAtRallyPoint(int blockId, int group) throws GameActionException {
	    Comm.writeBlock(blockId, COUNT_BUFFER_END - (Clock.getRoundNum() % 2) + group * 8,
	            1 + Comm.readBlock(blockId, COUNT_BUFFER_END - (Clock.getRoundNum() % 2) + group * 8));
	}

	public static int getCountAtRallyPoint(int blockId, int group) throws GameActionException {
        return Comm.readBlock(blockId, COUNT_BUFFER_START + (Clock.getRoundNum() % 2) + group * 8);
	}

	public static void resetBufferForGroup(int blockId, int group) throws GameActionException {
        Comm.writeBlock(blockId, COUNT_BUFFER_END - (Clock.getRoundNum() % 2) + group * 8, 0);
	}

	public static void incrementWaveNum(int blockId) throws GameActionException {
	    Comm.writeBlock(blockId, Comm.WAVENUM_OFFSET, 1 + Comm.readBlock(blockId, Comm.WAVENUM_OFFSET));
	}
}
