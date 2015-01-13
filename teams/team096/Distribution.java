package team096;

import battlecode.common.*;

public class Distribution {
	public static final int LAST_CHAN = 255;
	public static final int META_CHAN = 1;
	public static final int FIRST_QUEUE = 2;
	
	public static final int BFS_TASK_ID = 1;
	
	public static int taskBlockId = 0;
	
	public static void initTasks() throws GameActionException {
		taskBlockId = Handler.rc.readBroadcast(Comm.TASK_CHAN);
		if (taskBlockId == 0) {
			taskBlockId = Comm.requestBlock(true);
			Handler.rc.broadcast(Comm.TASK_CHAN, taskBlockId);
			Comm.writeBlock(taskBlockId, META_CHAN, (FIRST_QUEUE << 8) + FIRST_QUEUE); // Free block points to 2
		}
	}
	
	// Runs through tasks round robin
	// Never register more than 254 tasks
	public static void spendBytecodesCalculating(int bytecodelimit) throws GameActionException {
		int headTail = Comm.readBlock(taskBlockId, META_CHAN); // 8 bits head, 8 heads tail
		int head = (headTail >> 8) & 0x000000ff;
		int tail = headTail & 0x000000ff;
		if (head != tail) { // Task available
			int task = Comm.readBlock(taskBlockId, head);
			if (executeTask(bytecodelimit, task)) { // Task completed
				head = (head + 1 > LAST_CHAN) ? FIRST_QUEUE : head + 1;
				Comm.writeBlock(taskBlockId, META_CHAN, (head << 8) + tail);
			} else { // Task not completed
				Comm.writeBlock(taskBlockId, tail, task);
				head = (head + 1 > LAST_CHAN) ? FIRST_QUEUE : head + 1;
				tail = (tail + 1 > LAST_CHAN) ? FIRST_QUEUE : tail + 1;
				Comm.writeBlock(taskBlockId, META_CHAN, (head << 8) + tail);
			}
			
		} else {
			Comm.garbageCollect(bytecodelimit);
		}
	}
	
	public static boolean executeTask(int bytecodelimit, int task) throws GameActionException {
		if (task >> 24 == BFS_TASK_ID) { // BFS task
			return NavBFS.calculate(bytecodelimit, task & 0x000000ff);
		}
		return false;
	}
	
	// Adds a tag encoded in at most 24 bits
	public static boolean addTask(int task, int taskId) throws GameActionException {
		int headTail = Comm.readBlock(taskBlockId, META_CHAN);
		int head = (headTail >> 8) & 0x000000ff;
		int tail = headTail & 0x000000ff;
		int next = (tail + 1 > LAST_CHAN) ? FIRST_QUEUE : tail + 1;
		if (next == head) return false; // No more free channels
		
		Comm.writeBlock(taskBlockId, next, task + (taskId << 24));
		Comm.writeBlock(taskBlockId, META_CHAN, (head << 8) + next);
		return true;
	}
	
}