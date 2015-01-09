package navBot;

import battlecode.common.*;

public class Distribution {
	
	public static void spendBytecodesCalculating(int bytecodelimit) throws GameActionException {
		Comm.garbageCollect(bytecodelimit);
	}
	
}