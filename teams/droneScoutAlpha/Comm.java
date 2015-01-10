package droneScoutAlpha;

import battlecode.common.*;

/*
 *  Communication memory is allocated in blocks of 256
 *  The first block is reserved as a metadata block
 *  Free blocks are stored as a hybrid linked list and array
 *  FREEBLOCCK_CHAN points to first free block, if offset 0 is 0 then the rest are free, else its a linked list of block ids
 *  inactive blocks are garbage collected
 *  active blocks begin with a metadata channel holding the roundnumber and block mask
 */
public class Comm {
	public static final int PERM_BLOCK_MASK = 1000000000;
	public static final int ACTIVE_BLOCK_MASK = 1000000;
	public static final int BLOCK_SIZE = 256;
	public static final int LAST_BLOCK = 255;
	public static final int GARBAGE_THRESHOLD = 50;
	
	// Metadata channels
	public static final int ID_CHAN = 0;
	public static final int FREEBLOCK_CHAN = 1;
	public static final int GARBAGE_CHAN = 2;
	public static final int LAST_GARBAGE_TURN_CHAN = 3;
	public static final int TASK_CHAN = 4;
	
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
	// Map
	public static final int MAP_BLOCK = 200;
	
	public static boolean initialized = false;

	public static void initComm() throws GameActionException {
		Handler.rc.broadcast(FREEBLOCK_CHAN, 1);
	}
	
	public static int getId() throws GameActionException {
		int id = Handler.rc.readBroadcast(ID_CHAN);
		Handler.rc.broadcast(ID_CHAN, id + 1);
		return id;
	}
	
	// Returns -1 is all blocks used
	public static int requestBlock(boolean isPermanent) throws GameActionException {
		int free = Handler.rc.readBroadcast(FREEBLOCK_CHAN);
		if (free > LAST_BLOCK) return -1;

		int next = Handler.rc.readBroadcast(free * BLOCK_SIZE);
		if (next == 0) {
			Handler.rc.broadcast(FREEBLOCK_CHAN, free + 1);
		} else {
			Handler.rc.broadcast(FREEBLOCK_CHAN, next);
		}

		Handler.rc.broadcast(free * BLOCK_SIZE, (isPermanent ? PERM_BLOCK_MASK : ACTIVE_BLOCK_MASK) + Clock.getRoundNum());
		return free;
	}
	
	// Only pass in active blocks that you own, there is no checks in place
	public static void releaseBlock(int blockId) throws GameActionException {
		Handler.rc.broadcast(blockId * BLOCK_SIZE, Handler.rc.readBroadcast(FREEBLOCK_CHAN));
		Handler.rc.broadcast(FREEBLOCK_CHAN, blockId);
	}
	
	// Refreshes turn count on block
	public static void refreshBlock(int blockId) throws GameActionException {
		int meta = Handler.rc.readBroadcast(blockId * BLOCK_SIZE);
		Handler.rc.broadcast(blockId * BLOCK_SIZE, (meta & 0xffff0000) + Clock.getRoundNum());
	}
	
	// Garbage collector
		public static void garbageCollect(int bytecodelimit) throws GameActionException {
			int lastGarbageCollected = Handler.rc.readBroadcast(LAST_GARBAGE_TURN_CHAN);
			if (Clock.getRoundNum() - lastGarbageCollected < GARBAGE_THRESHOLD) {
				return;
			}
			int start = Handler.rc.readBroadcast(GARBAGE_CHAN);
			int cur = start;
			while (Clock.getRoundNum() < bytecodelimit) {
				cur++;
				int meta = Handler.rc.readBroadcast(cur * BLOCK_SIZE);
				if (meta < PERM_BLOCK_MASK && meta >= ACTIVE_BLOCK_MASK && Clock.getRoundNum() - (meta & 0x0000ffff) > GARBAGE_THRESHOLD) {
					releaseBlock(cur);
				}
				if (cur == LAST_BLOCK) {
					cur = 0;
					Handler.rc.broadcast(LAST_GARBAGE_TURN_CHAN, Clock.getRoundNum());
					break;
				}
			}
			if (cur != start) {
				Handler.rc.broadcast(GARBAGE_CHAN, cur);
			}
		}
	
	/****** Read/Write *****/
	
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
	
	/****** Cached Getters *****/
	
	public static int hqBlockId = 0;
	public static int towerBlockId = 0;
	public static int supplyBlockId = 0;
	public static int techBlockId = 0;
	public static int barrackBlockId = 0;
	public static int heliBlockId = 0;
	public static int minerfactBlockId = 0;
	public static int tankfactBlockId = 0;
	public static int aeroBlockId = 0;
	public static int trainingBlockId = 0;
	public static int handwashBlockId = 0;

	public static int beaverBlockId = 0;
	public static int minerBlockId = 0;
	public static int computerBlockId = 0;
	public static int soldierBlockId = 0;
	public static int basherBlockId = 0;
	public static int droneBlockId = 0;
	public static int tankBlockId = 0;
	public static int launcherBlockId = 0;
	public static int missileBlockId = 0;
	public static int commanderBlockId = 0;
	public static int mapBlockId = 0;

	public static int getHqId() throws GameActionException {
		if (hqBlockId == 0) {
			hqBlockId = Handler.rc.readBroadcast(HQ_BLOCK);
			if (hqBlockId == 0) {
				hqBlockId = requestBlock(true);
				Handler.rc.broadcast(HQ_BLOCK, hqBlockId);
			}
		}
		return hqBlockId;
	}
	public static int getTowerId() throws GameActionException {
		if (towerBlockId == 0) {
			towerBlockId = Handler.rc.readBroadcast(TOWER_BLOCK);
			if (towerBlockId == 0) {
				towerBlockId = requestBlock(true);
				Handler.rc.broadcast(TOWER_BLOCK, towerBlockId);
			}
		}
		return towerBlockId;
	}
	public static int getSupplyId() throws GameActionException {
		if (supplyBlockId == 0) {
			supplyBlockId = Handler.rc.readBroadcast(SUPPLY_BLOCK);
			if (supplyBlockId == 0) {
				supplyBlockId = requestBlock(true);
				Handler.rc.broadcast(SUPPLY_BLOCK, supplyBlockId);
			}
		}
		return supplyBlockId;
	}
	public static int getTechId() throws GameActionException {
		if (techBlockId == 0) {
			techBlockId = Handler.rc.readBroadcast(TECH_BLOCK);
			if (techBlockId == 0) {
				techBlockId = requestBlock(true);
				Handler.rc.broadcast(TECH_BLOCK, techBlockId);
			}
		}
		return techBlockId;
	}
	public static int getBarrackId() throws GameActionException {
		if (barrackBlockId == 0) {
			barrackBlockId = Handler.rc.readBroadcast(BARRACK_BLOCK);
			if (barrackBlockId == 0) {
				barrackBlockId = requestBlock(true);
				Handler.rc.broadcast(BARRACK_BLOCK, barrackBlockId);
			}
		}
		return barrackBlockId;
	}
	public static int getHeliId() throws GameActionException {
		if (heliBlockId == 0) {
			heliBlockId = Handler.rc.readBroadcast(HELI_BLOCK);
			if (heliBlockId == 0) {
				heliBlockId = requestBlock(true);
				Handler.rc.broadcast(HELI_BLOCK, heliBlockId);
			}
		}
		return heliBlockId;
	}
	public static int getMinerfactId() throws GameActionException {
		if (minerfactBlockId == 0) {
			minerfactBlockId = Handler.rc.readBroadcast(MINERFACT_BLOCK);
			if (minerfactBlockId == 0) {
				minerfactBlockId = requestBlock(true);
				Handler.rc.broadcast(MINERFACT_BLOCK, minerfactBlockId);
			}
		}
		return minerfactBlockId;
	}
	public static int getTankfactId() throws GameActionException {
		if (tankfactBlockId == 0) {
			tankfactBlockId = Handler.rc.readBroadcast(TANKFACT_BLOCK);
			if (tankfactBlockId == 0) {
				tankfactBlockId = requestBlock(true);
				Handler.rc.broadcast(TANKFACT_BLOCK, tankfactBlockId);
			}
		}
		return tankfactBlockId;
	}
	public static int getAeroId() throws GameActionException {
		if (aeroBlockId == 0) {
			aeroBlockId = Handler.rc.readBroadcast(AERO_BLOCK);
			if (aeroBlockId == 0) {
				aeroBlockId = requestBlock(true);
				Handler.rc.broadcast(AERO_BLOCK, aeroBlockId);
			}
		}
		return aeroBlockId;
	}
	public static int getTrainingId() throws GameActionException {
		if (trainingBlockId == 0) {
			trainingBlockId = Handler.rc.readBroadcast(TRAINING_BLOCK);
			if (trainingBlockId == 0) {
				trainingBlockId = requestBlock(true);
				Handler.rc.broadcast(TRAINING_BLOCK, trainingBlockId);
			}
		}
		return trainingBlockId;
	}
	public static int getHandwashId() throws GameActionException {
		if (handwashBlockId == 0) {
			handwashBlockId = Handler.rc.readBroadcast(HANDWASH_BLOCK);
			if (handwashBlockId == 0) {
				handwashBlockId = requestBlock(true);
				Handler.rc.broadcast(HANDWASH_BLOCK, handwashBlockId);
			}
		}
		return handwashBlockId;
	}

	public static int getBeaverId() throws GameActionException {
		if (beaverBlockId == 0) {
			beaverBlockId = Handler.rc.readBroadcast(BEAVER_BLOCK);
			if (beaverBlockId == 0) {
				beaverBlockId = requestBlock(true);
				Handler.rc.broadcast(BEAVER_BLOCK, beaverBlockId);
			}
		}
		return beaverBlockId;
	}
	public static int getMinerId() throws GameActionException {
		if (minerBlockId == 0) {
			minerBlockId = Handler.rc.readBroadcast(MINER_BLOCK);
			if (minerBlockId == 0) {
				minerBlockId = requestBlock(true);
				Handler.rc.broadcast(MINER_BLOCK, minerBlockId);
			}
		}
		return minerBlockId;
	}
	public static int getComputerId() throws GameActionException {
		if (computerBlockId == 0) {
			computerBlockId = Handler.rc.readBroadcast(COMPUTER_BLOCK);
			if (computerBlockId == 0) {
				computerBlockId = requestBlock(true);
				Handler.rc.broadcast(COMPUTER_BLOCK, computerBlockId);
			}
		}
		return computerBlockId;
	}
	public static int getSoldierId() throws GameActionException {
		if (soldierBlockId == 0) {
			soldierBlockId = Handler.rc.readBroadcast(SOLDIER_BLOCK);
			if (soldierBlockId == 0) {
				soldierBlockId = requestBlock(true);
				Handler.rc.broadcast(SOLDIER_BLOCK, soldierBlockId);
			}
		}
		return soldierBlockId;
	}
	public static int getBasherId() throws GameActionException {
		if (basherBlockId == 0) {
			basherBlockId = Handler.rc.readBroadcast(BASHER_BLOCK);
			if (basherBlockId == 0) {
				basherBlockId = requestBlock(true);
				Handler.rc.broadcast(BASHER_BLOCK, basherBlockId);
			}
		}
		return basherBlockId;
	}
	public static int getDroneId() throws GameActionException {
		if (droneBlockId == 0) {
			droneBlockId = Handler.rc.readBroadcast(DRONE_BLOCK);
			if (droneBlockId == 0) {
				droneBlockId = requestBlock(true);
				Handler.rc.broadcast(DRONE_BLOCK, droneBlockId);
			}
		}
		return droneBlockId;
	}
	public static int getTankId() throws GameActionException {
		if (tankBlockId == 0) {
			tankBlockId = Handler.rc.readBroadcast(TANK_BLOCK);
			if (tankBlockId == 0) {
				tankBlockId = requestBlock(true);
				Handler.rc.broadcast(TANK_BLOCK, tankBlockId);
			}
		}
		return tankBlockId;
	}
	public static int getLauncherId() throws GameActionException {
		if (launcherBlockId == 0) {
			launcherBlockId = Handler.rc.readBroadcast(LAUNCHER_BLOCK);
			if (launcherBlockId == 0) {
				launcherBlockId = requestBlock(true);
				Handler.rc.broadcast(LAUNCHER_BLOCK, launcherBlockId);
			}
		}
		return launcherBlockId;
	}
	public static int getMissileId() throws GameActionException {
		if (missileBlockId == 0) {
			missileBlockId = Handler.rc.readBroadcast(MISSILE_BLOCK);
			if (missileBlockId == 0) {
				missileBlockId = requestBlock(true);
				Handler.rc.broadcast(MISSILE_BLOCK, missileBlockId);
			}
		}
		return missileBlockId;
	}
	public static int getCommanderId() throws GameActionException {
		if (commanderBlockId == 0) {
			commanderBlockId = Handler.rc.readBroadcast(COMMANDER_BLOCK);
			if (commanderBlockId == 0) {
				commanderBlockId = requestBlock(true);
				Handler.rc.broadcast(COMMANDER_BLOCK, commanderBlockId);
			}
		}
		return commanderBlockId;
	}
	public static int getMapId() throws GameActionException {
		if (mapBlockId == 0) {
			mapBlockId = Handler.rc.readBroadcast(MAP_BLOCK);
			if (mapBlockId == 0) {
				mapBlockId = requestBlock(true);
				Handler.rc.broadcast(MAP_BLOCK, mapBlockId);
			}
		}
		return mapBlockId;
	}

}
