package droneScoutZeta;

import battlecode.common.*;

public class Distribution {
	public static final int LAST_CHAN = 255;
	
	public static int taskBlockId = 0;
	
	public static void initTasks() throws GameActionException {
		taskBlockId = Handler.rc.readBroadcast(Comm.TASK_CHAN);
		if (taskBlockId == 0) {
			taskBlockId = Comm.requestBlock(true);
			Handler.rc.broadcast(Comm.TASK_CHAN, taskBlockId);
			Comm.writeBlock(taskBlockId, 1, 2); // Free block points to 2
		}
	}
	
	// Runs through tasks round robin
	public static void spendBytecodesCalculating(int bytecodelimit) throws GameActionException {
		int headTailSizeFree = Comm.readBlock(taskBlockId, 1);
		int head = headTailSizeFree >> 24;
		if (head != 0) { // Task available
			int task = Comm.readBlock(taskBlockId, head);
			int size = (headTailSizeFree >> 8) & 0x000000ff;
			int tail = (headTailSizeFree >> 16) & 0x000000ff;
			int free = headTailSizeFree & 0x000000ff;
			int next = task >> 24;
			
			if (executeTask(task)) { // Task completed
				if (size == 1) {
					headTailSizeFree = head;
				} else {
					headTailSizeFree = (next << 24) + (tail << 16) + ((size - 1) << 8) + head;
				}
				Comm.writeBlock(taskBlockId, head, free);
				Comm.writeBlock(taskBlockId, 1, headTailSizeFree);
			} else { // Task not completed
				if (size > 1) {
					headTailSizeFree = (next << 24) + (head << 16) + (size << 8) + free;
					Comm.writeBlock(taskBlockId, tail, Comm.readBlock(taskBlockId, tail) & 0x00ffffff + (free << 24));
					Comm.writeBlock(taskBlockId, 1, headTailSizeFree);
				}
			}
			
		} else {
			Comm.garbageCollect(bytecodelimit);
		}
	}
	
	public static boolean executeTask(int task) throws GameActionException {
		return false;
	}
	
	// Adds a tag encoded in at most 24 bits
	public static boolean addTask(int task) throws GameActionException {
		int headTailSizeFree = Comm.readBlock(taskBlockId, 1);
		int free = headTailSizeFree & 0x000000ff;
		if (free > LAST_CHAN) return false; // No more free channels
		
		int next = Comm.readBlock(taskBlockId, free);
		if (next == 0) {
			next = free + 1;
		}
		
		int size = (headTailSizeFree >> 8) & 0x000000ff;
		int tail = (headTailSizeFree >> 16) & 0x000000ff;
		int head = headTailSizeFree >> 24;
		if (size == 0) {
			head = free;
		} else {
			Comm.writeBlock(taskBlockId, tail, Comm.readBlock(taskBlockId, tail) & 0x00ffffff + (free << 24));
		}
		tail = free;
		size++;
		
		Comm.writeBlock(taskBlockId, free, task);
		Comm.writeBlock(taskBlockId, 1, (head << 24) + (tail << 16) + (size << 8) + next);
		return true;
	}
	
}