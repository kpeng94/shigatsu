package navAndBuildBot;

import battlecode.common.*;

/**
 * Hello
 * 
 * @author Taiga
 *
 */
public class BuildSystemQueue {
	public static final int BLOCK_NUM = Comm.BUILD_BLOCK;
	public static int buildBlockId = 0;

	public static int MINER_QUEUE = 1;
	public static int COMPUTER_QUEUE = 2;
	public static int SOLDIER_QUEUE = 3;
	public static int BASHER_QUEUE = 4;
	public static int DRONE_QUEUE = 5;
	public static int TANK_QUEUE = 6;
	public static int COMMANDER_QUEUE = 7;
	public static int LAUNCHER_QUEUE = 8;

	// At this ore level, we don't care about priorities - just build.
	public static final int ORE_SAFETY_THRESHOLD = 1500;

	// Priorities start from 0 (lowest) to 7 (highest)
	public static int[] PRIORITIES = { 9, 10, 11, 12, 13, 14, 15, 16 };

	public static int FREE_BLOCK = 18;

	/**
	 * Hello
	 */
	public static void init() throws GameActionException {
		buildBlockId = Handler.rc.readBroadcast(Comm.BUILD_BLOCK);
		if (buildBlockId == 0) {
			buildBlockId = Comm.requestBlock(true);
			Handler.rc.broadcast(BLOCK_NUM, buildBlockId);
			Comm.writeBlock(buildBlockId, FREE_BLOCK, FREE_BLOCK + 1);
		}
		return;
	}

	/**
	 * Adds a structure building task to the queue.
	 * 
	 * @param priority
	 *            - priority of task
	 * @param type
	 *            - type of building to be built
	 * @param location
	 *            - location to build the building
	 */
	public static void addStructureTask(int priority, RobotType type, MapLocation location) throws GameActionException {
		int nextFreeBlock = Comm.readBlock(buildBlockId, FREE_BLOCK);
		int freeBlock = Comm.readBlock(buildBlockId, nextFreeBlock);
		if (freeBlock != 0) {
			Comm.writeBlock(buildBlockId, FREE_BLOCK, freeBlock);
		} else {
			Comm.writeBlock(buildBlockId, FREE_BLOCK, nextFreeBlock + 1);
		}
		int numberToBroadcast = buildingTypeToInteger(type) << 19 | priority << 16 | MapUtils.encode(location);

		int priorityQueue = Comm.readBlock(buildBlockId, PRIORITIES[priority]);
		int tail = priorityQueue & 0xFF;
		Comm.writeBlock(buildBlockId, nextFreeBlock, numberToBroadcast);

		if (tail != 0) {
			int tailBlock = Comm.readBlock(buildBlockId, tail);
			Comm.writeBlock(buildBlockId, tail, nextFreeBlock << 23 | tailBlock);
			Comm.writeBlock(buildBlockId, PRIORITIES[priority], (priorityQueue & 0xFF) | nextFreeBlock);
		} else {
			Comm.writeBlock(buildBlockId, PRIORITIES[priority], nextFreeBlock << 8 | nextFreeBlock);
		}
	}

	/**
	 * Overwrites the spawn queue for a specific robot type with the given
	 * number. Also sets a priority (lowest priority is 0, highest priority is
	 * 7).
	 * 
	 * @param priority
	 *            - must be between 0 and 7
	 * @param type
	 *            - must be a valid, spawnable robot type (not a structure)
	 * @param number
	 *            - can be at maximum 2^16 - 1
	 */
	public static void setSpawnRobots(int priority, int number, RobotType type) throws GameActionException {
		int offset = robotTypeToInteger(type);
		int numberToBroadcast = priority << 16 | number;
		Comm.writeBlock(buildBlockId, offset, numberToBroadcast);
	}

	/**
	 * 
	 * @param type
	 * @return 0 if nothing to be built, nonzero otherwise (beavers have to
	 *         manually decode the information)
	 * @throws GameActionException
	 */
	public static int getNextTask() throws GameActionException {
		if (Handler.rc.getTeamOre() > ORE_SAFETY_THRESHOLD) {
			switch (Handler.typ) {
			case AEROSPACELAB:
				return decrementRobotNeededCountIfPossible(RobotType.LAUNCHER);
			case TRAININGFIELD:
				return decrementRobotNeededCountIfPossible(RobotType.COMMANDER);
			case HELIPAD:
				return decrementRobotNeededCountIfPossible(RobotType.DRONE);
			case MINERFACTORY:
				return decrementRobotNeededCountIfPossible(RobotType.MINER);
			case TANKFACTORY:
				return decrementRobotNeededCountIfPossible(RobotType.TANK);
			case BARRACKS:
				int soldierInfo = Comm.readBlock(buildBlockId, SOLDIER_QUEUE);
				int basherInfo = Comm.readBlock(buildBlockId, BASHER_QUEUE);

				if (basherInfo == 0 && soldierInfo == 0) {
					return 0;
				} else if (soldierInfo == 0) {
					decrementRobotNeededCount(BASHER_QUEUE, basherInfo);
					return 1;
				} else if (basherInfo == 0) {
					decrementRobotNeededCount(SOLDIER_QUEUE, soldierInfo);
					return 2;
				} else {
					int soldierPriority = soldierInfo >>> 16;
					int basherPriority = basherInfo >>> 16;
					if (soldierPriority >= basherPriority) {
						decrementRobotNeededCount(SOLDIER_QUEUE, soldierInfo);
						return 2;
					} else {
						decrementRobotNeededCount(BASHER_QUEUE, basherInfo);
						return 1;
					}
				}
			case BEAVER:
				for (int chan = 16; chan >= 9; chan--) {
					int chanInfo = Comm.readBlock(buildBlockId, chan);
					if (chanInfo != 0) {
						return getHeadOfQueue(chan, chanInfo);
					}
				}
				return 0;
			default:
				return 0;
			}
		} else {
			if (Handler.typ == RobotType.BEAVER) {
				int priority;
				int channelInfo = 0;
				for (priority = 16; priority >= 9; priority--) {
					channelInfo = Comm.readBlock(buildBlockId, priority);
					if (channelInfo != 0) {
						break;
					}
				}

				if(priority == 8){
					return 0;
				}
				
				priority -= 9;
				int ore = getOreTotalOfHigherPriorities(priority);
				int head = (channelInfo & 0xFF00) >> 8;
				int tail = (channelInfo & 0xFF);
				int taskInfo = Comm.readBlock(buildBlockId, head);
				if (Handler.rc.getTeamOre() - ore >= BUILDING_ORE_COSTS[(taskInfo & 0x8F0000) >>> 19]) {
					int nextHead = taskInfo >>> 23;
					
					if (nextHead == 0) {
						Comm.writeBlock(buildBlockId, PRIORITIES[priority], 0);
					} else {
						Comm.writeBlock(buildBlockId, PRIORITIES[priority], (nextHead << 8) | tail);
					}

					int nextFreeBlock = Comm.readBlock(buildBlockId, FREE_BLOCK);
					Comm.writeBlock(buildBlockId, head, nextFreeBlock);
					Comm.writeBlock(buildBlockId, FREE_BLOCK, head);
					return taskInfo;
				} else
					return 0;
			} else {
				int channelInfo;
				int channel;
				switch (Handler.typ) {
				case AEROSPACELAB:
					channel = LAUNCHER_QUEUE;
					channelInfo = Comm.readBlock(buildBlockId, LAUNCHER_QUEUE);
					break;
				case TRAININGFIELD:
					channel = COMMANDER_QUEUE;
					channelInfo = Comm.readBlock(buildBlockId, COMMANDER_QUEUE);
					break;
				case HELIPAD:
					channel = DRONE_QUEUE;
					channelInfo = Comm.readBlock(buildBlockId, DRONE_QUEUE);
					break;
				case MINERFACTORY:
					channel = MINER_QUEUE;
					channelInfo = Comm.readBlock(buildBlockId, MINER_QUEUE);
					break;
				case TANKFACTORY:
					channel = TANK_QUEUE;
					channelInfo = Comm.readBlock(buildBlockId, TANK_QUEUE);
					break;
				case BARRACKS:
					channel = SOLDIER_QUEUE;
					channelInfo = Comm.readBlock(buildBlockId, SOLDIER_QUEUE);
					int otherInfo = Comm.readBlock(buildBlockId, BASHER_QUEUE);
					if (otherInfo >>> 16 > channelInfo >>> 16) {
						channelInfo = otherInfo;
						channel = BASHER_QUEUE;
					}
				default:
					return 0;
				}

				if (channelInfo == 0) {
					return 0;
				} else {
					int priority = channelInfo >>> 16;
					int ore = getOreTotalOfHigherPriorities(priority);
					if (Handler.rc.getTeamOre() - ore > ORE_COSTS[channel]) {
						if (channel == SOLDIER_QUEUE)
							return 2;
						else
							return 1;
					} else {
						return 0;
					}
				}
			}

		}
	}

	private static final int[] ORE_COSTS = { 0, 50, 10, 60, 80, 125, 250, 100, 400 };
	private static final int[] BUILDING_ORE_COSTS = { 100, 500, 300, 200, 300, 500, 200, 500, 200 };

	private static int getOreTotalOfHigherPriorities(int priority) throws GameActionException {
		int total = 0;
		for (int i = 1; i < 9; i++) {
			int channelInfo = Comm.readBlock(buildBlockId, PRIORITIES[priority]);
			int channelPriority = channelInfo >>> 16;
			if (channelPriority > priority)
				total += (channelInfo & 0xFFFF) * ORE_COSTS[i];
		}
		for (int i = 16; i > 9 + priority; i--) {
			total += getOreTotalOfPriority(i);
		}
		return total;
	}

	private static int getOreTotalOfPriority(int priority) throws GameActionException {
		int metadata = Comm.readBlock(buildBlockId, PRIORITIES[priority]);
		int total = 0;
		int info = Comm.readBlock(buildBlockId, metadata >> 8);
		while (info != 0) {
			total += BUILDING_ORE_COSTS[(info & 0x8FFFFF) >>> 19];
			info = Comm.readBlock(buildBlockId, info >>> 23);
		}
		return total;
	}

	private static int decrementRobotNeededCountIfPossible(RobotType type) throws GameActionException {
		int channel = robotTypeToInteger(type);
		int info = Comm.readBlock(buildBlockId, channel);
		if (info == 0)
			return 0;
		decrementRobotNeededCount(channel, info);
		return 1;
	}

	private static void decrementRobotNeededCount(int channel, int info) throws GameActionException {
		int numNeeded = info & 0xFFFF;
		Comm.writeBlock(buildBlockId, channel, (info & 0xFFFF0000) | (numNeeded - 1));
	}

	private static int getHeadOfQueue(int chan, int chanInfo) throws GameActionException {
		int head = (chanInfo & 0xFF00) >> 8;
		int tail = (chanInfo & 0xFF);
		int taskInfo = Comm.readBlock(buildBlockId, head);
		int nextHead = taskInfo >>> 23;
		if (nextHead == 0) {
			Comm.writeBlock(buildBlockId, chan, 0);
		} else {
			Comm.writeBlock(buildBlockId, chan, (nextHead << 8) | tail);
		}

		int nextFreeBlock = Comm.readBlock(buildBlockId, FREE_BLOCK);
		Comm.writeBlock(buildBlockId, head, nextFreeBlock);
		Comm.writeBlock(buildBlockId, FREE_BLOCK, head);

		return taskInfo;
	}

	private static int robotTypeToInteger(RobotType type) {
		switch (type) {
		case MINER:
			return MINER_QUEUE;
		case COMPUTER:
			return COMPUTER_QUEUE;
		case SOLDIER:
			return SOLDIER_QUEUE;
		case BASHER:
			return BASHER_QUEUE;
		case DRONE:
			return DRONE_QUEUE;
		case TANK:
			return DRONE_QUEUE;
		case COMMANDER:
			return COMMANDER_QUEUE;
		case LAUNCHER:
			return LAUNCHER_QUEUE;
		default:
			return -1;
		}
	}

	public static int buildingTypeToInteger(RobotType type) {
		switch (type) {
		case SUPPLYDEPOT:
			return 0;
		case AEROSPACELAB:
			return 1;
		case BARRACKS:
			return 2;
		case HANDWASHSTATION:
			return 3;
		case HELIPAD:
			return 4;
		case MINERFACTORY:
			return 5;
		case TECHNOLOGYINSTITUTE:
			return 6;
		case TANKFACTORY:
			return 7;
		case TRAININGFIELD:
			return 8;
		default:
			return -1;
		}
	}
}
