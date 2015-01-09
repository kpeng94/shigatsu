package navBot;

import battlecode.common.*;

/*
 *  Communication memory is allocated in blocks of 256
 *  The first block is reserved as a metadata block
 *  Free blocks are stored as a hybrid linked list and array
 *  FREEBLOCCK_CHAN points to first free block, if offset 0 is 0 then the rest are free, else its a linked list of block ids
 *  inactive blocks are garbage collected
 */
public class Comm {
	public static final int ACTIVE_BLOCK_MASK = 1000000000;
	public static final int BLOCK_SIZE = 256;
	public static final int LAST_BLOCK = 255;
	public static final int GARBAGE_THRESHOLD = 50;
	
	// Metadata channels
	public static final int ID_CHAN = 0;
	public static final int FREEBLOCK_CHAN = 1;
	public static final int GARBAGE_CHAN = 2;
	
	// Structures
	public static final int HQ_BLOCK = 100;
	public static final int TOWER_BLOCK = 101;
	public static final int SUPPLY_BLOCK = 102;
	public static final int TECH_BLOCK = 103;
	public static final int BARRACK_BLOCK = 104;
	public static final int HELI_BLOCK = 105;
	public static final int MINERFACT_BLOCK = 106;
	public static final int TANKFACT_BLOCK = 107;
	public static final int AERO_BLOCK = 108;
	public static final int TRAINING_BLOCK = 109;
	public static final int HANDWASH_BLOCK = 110;
	// Units
	public static final int BEAVER_BLOCK = 111;
	public static final int MINER_BLOCK = 112;
	public static final int COMPUTER_BLOCK = 113;
	public static final int SOLDIER_BLOCK = 114;
	public static final int BASHER_BLOCK = 115;
	public static final int DRONE_BLOCK = 116;
	public static final int TANK_BLOCK = 117;
	public static final int LAUNCHER_BLOCK = 118;
	public static final int MISSILE_BLOCK = 119;
	public static final int COMMANDER_BLOCK = 120;

	public static void initComm() throws GameActionException {
		Handler.rc.broadcast(FREEBLOCK_CHAN, 1);
	}
	
	public static int getId() throws GameActionException {
		int id = Handler.rc.readBroadcast(ID_CHAN);
		Handler.rc.broadcast(ID_CHAN, id + 1);
		return id;
	}
	
	// Returns -1 is all blocks used
	public static int requestBlock() throws GameActionException {
		int free = Handler.rc.readBroadcast(FREEBLOCK_CHAN);
		if (free > LAST_BLOCK) return -1;
		if (free == 0) {
			Handler.rc.broadcast(FREEBLOCK_CHAN, free + 1);
		} else {
			Handler.rc.broadcast(FREEBLOCK_CHAN, Handler.rc.readBroadcast(free * BLOCK_SIZE));
		}
		Handler.rc.broadcast(free * BLOCK_SIZE, ACTIVE_BLOCK_MASK + Clock.getRoundNum());
		return free;
	}
	
	// Garbage collector
	public static void garbageCollect(int bytecodelimit) throws GameActionException {
		int start = Handler.rc.readBroadcast(GARBAGE_CHAN);
		int cur = start;
		while (Clock.getRoundNum() < bytecodelimit) {
			cur++;
			int meta = Handler.rc.readBroadcast(cur * BLOCK_SIZE);
			if (meta > ACTIVE_BLOCK_MASK && (meta & 0x0000ffff) - Clock.getRoundNum() > GARBAGE_THRESHOLD) {
				releaseBlock(cur);
			}
			if (cur == LAST_BLOCK) {
				cur = 0;
				break;
			}
		}
		if (cur != start) {
			Handler.rc.broadcast(GARBAGE_CHAN, cur);
		}
	}
	
	// Only pass in active blocks that you own, there is no checks in place
	public static void releaseBlock(int blockId) throws GameActionException {
		Handler.rc.broadcast(blockId * BLOCK_SIZE, Handler.rc.readBroadcast(FREEBLOCK_CHAN));
		Handler.rc.broadcast(FREEBLOCK_CHAN, blockId);
	}
	
	// Writes a block
	public static void writeBlock(int blockId, int offset, int data) throws GameActionException {
		Handler.rc.broadcast(blockId * BLOCK_SIZE + offset, data);
	}
	
	// Writes a block prepended with current round
	public static void writeBlockWithRound(int blockId, int offset, int data) throws GameActionException {
		Handler.rc.broadcast(blockId * BLOCK_SIZE + offset, data + (Clock.getRoundNum() << 16));
	}
	
	// Reads whole block
	public static int readBlock(int blockId, int offset) throws GameActionException {
		return Handler.rc.readBroadcast(blockId * BLOCK_SIZE + offset);
	}
	
	// Reads the round the block was last written
	public static int readBlockRound(int blockId, int offset) throws GameActionException {
		return Handler.rc.readBroadcast(blockId * BLOCK_SIZE + offset) >> 16;
	}
	
	// Reads only the data of the block
	public static int readBlockData(int blockId, int offset) throws GameActionException {
		return Handler.rc.readBroadcast(blockId * BLOCK_SIZE + offset) & 0x0000ffff;
	}
	
//	public static boolean checkFlag(int val, int offset) {
//		return (val & (1 << offset)) == (1 << offset);
//	}
//	
//	public static int setFlag(int val, int offset) {
//		return val | (1 << offset);
//	}
//	
//	public static int unsetFlag(int val, int offset) {
//		return val & ~(1 << offset);
//	}
	
}
