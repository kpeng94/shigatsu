package navBot;

import battlecode.common.*;

public class NavBFS {
	public static final int META_CHAN = 5;
	public static final int FIRST_QUEUE = META_CHAN + 1;

	// Maplocation task encoded in 16 bits followed by BFS id
	public static void newBFSTask(MapLocation center) throws GameActionException {
		int baseBlock = Comm.requestBlock(true);
		Distribution.addTask(baseBlock, Distribution.BFS_TASK_ID);
		for (int i = 4; --i >= 0;) { // Get the 16 base channels
			Comm.writeBlock(baseBlock, i + 1, (Comm.requestBlock(true) << 24) + (Comm.requestBlock(true) << 16) + (Comm.requestBlock(true) << 8) + Comm.requestBlock(true));
		}
		Comm.writeBlock(baseBlock, META_CHAN, (FIRST_QUEUE << 8) + FIRST_QUEUE);
	}
	
	public static boolean calculate(int bytecodelimit, int baseBlock) {
		while (Clock.getBytecodeNum() < bytecodelimit) {
			
		}
		return false;
	}
	
}
