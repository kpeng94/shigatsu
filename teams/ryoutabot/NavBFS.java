package ryoutabot;

import battlecode.common.*;

public class NavBFS {
    public static final int META_CHAN = 9;
    public static final int FIRST_CHAN = META_CHAN + 1;
    public static final int LAST_CHAN = 1023;
    public static final int MAP_SIZE = 7200;
    public static final int MAX_WRITES = 40;
    public static final int MAX_BUFFER = 10;
    public static final int UNKNOWN_THRESHOLD = 5;
    
    private static int[] cache = new int[GameConstants.BROADCAST_MAX_CHANNELS];
    
    // Maplocation task encoded in 16 bits followed by BFS id
    public static int newBFSTask(MapLocation center) throws GameActionException {
        int baseBlock = Comm.requestBlock(true);
        Distribution.addTask(baseBlock, Distribution.BFS_TASK_ID);
        for (int i = 8; --i >= 0;) { // Get the 32 base channels
            Comm.writeBlock(baseBlock, i + 1, (Comm.requestBlock(true) << 24) + (Comm.requestBlock(true) << 16) + (Comm.requestBlock(true) << 8) + Comm.requestBlock(true));
        }
        Comm.writeBlock(baseBlock, FIRST_CHAN, MapUtils.encode(center));
        Comm.writeBlock(baseBlock, META_CHAN, (FIRST_CHAN << 16) + FIRST_CHAN + 1);
        
        return baseBlock;
    }
    
    // Calculates BFS for a single map location
    public static boolean calculate(int bytecodelimit, int baseBlock) throws GameActionException {
        int headTail = Comm.readBlock(baseBlock, META_CHAN);
        int head = (headTail >>> 16) & 0x0000ffff;
        int tail = headTail & 0x0000ffff;
        int[] localCache = new int[MAP_SIZE];
        boolean[] dirty = new boolean[MAP_SIZE];
        int[] dirtyQueue = new int[MAX_WRITES]; // first index is numDirty
        int numUnknown = 0;
        
        while (Clock.getBytecodeNum() < bytecodelimit - (dirtyQueue[0] * 50) - 500 && head != tail && dirtyQueue[0] < MAX_WRITES - MAX_BUFFER) {
            int encodedPos = readFromQueue(baseBlock, head);
            MapLocation pos = MapUtils.decode(encodedPos);
            TerrainTile tile = Handler.rc.senseTerrainTile(pos);
            if (tile == TerrainTile.UNKNOWN) { // Requeue current tile
                writeToQueue(baseBlock, tail, encodedPos);
                head = nextInQueue(head);
                tail = nextInQueue(tail);
                numUnknown++;
                if (numUnknown == UNKNOWN_THRESHOLD) {
                    break;
                }
            } else if (tile == TerrainTile.NORMAL) {
                int dist = readMapData(baseBlock, encodedPos, localCache) >>> 3;
                if (dist == 0) { // source
                    writeToMapCache(encodedPos, 1 << 3, localCache, dirty, dirtyQueue);
                }

                for (int i = MapUtils.dirsDiagFirst.length; --i >= 0;) {
                    MapLocation check = pos.add(MapUtils.dirsDiagFirst[i]);
                    TerrainTile checkTile = Handler.rc.senseTerrainTile(check);
                    int encodedCheck = MapUtils.encode(check);
                    if (checkTile == TerrainTile.UNKNOWN || checkTile == TerrainTile.NORMAL) {
                        int oldDist = readMapData(baseBlock, encodedCheck, localCache) >>> 3;
                        if (oldDist == 0 || dist + 1 < oldDist) {
                            Handler.rc.setIndicatorLine(pos, check, 255, 0, 0);
                            writeToMapCache(encodedCheck, ((dist + 1) << 3) + MapUtils.dirsDiagFirst[i].opposite().ordinal(), localCache, dirty, dirtyQueue);
                            writeToQueue(baseBlock, tail, encodedCheck);
                            tail = nextInQueue(tail);
                        }
                    }
                }
                head = nextInQueue(head);
            } else {
                head = nextInQueue(head);
            }
        }

        Comm.writeBlock(baseBlock, META_CHAN, (head << 16) + tail);
        writeCache(baseBlock, localCache, dirtyQueue);
        return head == tail;
    }
    
    public static MapLocation[] backtrace(int baseBlock, MapLocation dest) throws GameActionException {
        int[] localCache = new int[MAP_SIZE];
        MapLocation pos = dest;
        int distDir = readMapData(baseBlock, MapUtils.encode(pos), localCache);
        MapLocation[] path = new MapLocation[distDir >>> 3];
        path[path.length - 1] = pos;
        for (int i = path.length - 1; --i >= 0;) {
            pos = pos.add(MapUtils.dirs[distDir & 0x00000007]);
            distDir = readMapData(baseBlock, MapUtils.encode(pos), localCache);
            path[i] = pos;
        }
        return path;
    }
    
    // Reads a distance/dir a maplocation is to the source
    public static int readMapData(int baseBlock, int pos, int[] localCache) throws GameActionException {
        pos = MapUtils.unsignEncoding(pos);
        int posIndex = pos / 2;
        if (localCache[posIndex] == 0) {
            int metaBlockNum = posIndex < Comm.LAST_BLOCK ? 1 : 2 + (posIndex / Comm.LAST_BLOCK - 1) / 4;
            int metaBlockOffset = posIndex < Comm.LAST_BLOCK ? 0 : (posIndex / Comm.LAST_BLOCK - 1) % 4;
            int mapBlockNum = (readCached(baseBlock * Comm.BLOCK_SIZE + metaBlockNum) >>> (8 * metaBlockOffset)) & 0x000000ff;
            localCache[posIndex] = Comm.readBlock(mapBlockNum, 1 + (posIndex % Comm.LAST_BLOCK));
        }
        
        return pos % 2 == 0 ? (localCache[posIndex] >>> 16) & 0x0000ffff : localCache[posIndex] & 0x0000ffff;
    }
    
    public static int readMapDataUncached(int baseBlock, int pos) throws GameActionException {
        pos = MapUtils.unsignEncoding(pos);
        int posIndex = pos / 2;
        int metaBlockNum = posIndex < Comm.LAST_BLOCK ? 1 : 2 + (posIndex / Comm.LAST_BLOCK - 1) / 4;
        int metaBlockOffset = posIndex < Comm.LAST_BLOCK ? 0 : (posIndex / Comm.LAST_BLOCK - 1) % 4;
        int mapBlockNum = (readCached(baseBlock * Comm.BLOCK_SIZE + metaBlockNum) >>> (8 * metaBlockOffset)) & 0x000000ff;
        int data = Comm.readBlock(mapBlockNum, 1 + (posIndex % Comm.LAST_BLOCK));
        return pos % 2 == 0 ? (data >>> 16) & 0x0000ffff : data & 0x0000ffff;
    }
    
    private static void writeToMapCache(int pos, int data, int[] localCache, boolean[] dirty, int[] dirtyQueue) {
        pos = MapUtils.unsignEncoding(pos);
        int posIndex = pos / 2;
        localCache[posIndex] = pos % 2 == 0 ? (localCache[posIndex] & 0x0000ffff) + (data << 16) : (localCache[posIndex] & 0xffff0000) + data;
        if (!dirty[posIndex]) { // new dirty
            dirty[posIndex] = true;
            dirtyQueue[dirtyQueue[0] + 1] = posIndex;
            dirtyQueue[0]++;
        }
    }
    
    private static void writeCache(int baseBlock, int[] localCache, int[] dirtyQueue) throws GameActionException {
        for (int i = dirtyQueue[0]; --i >= 0;) {
            int posIndex = dirtyQueue[i + 1];
            int data = localCache[posIndex];
            
            int metaBlockNum = posIndex < Comm.LAST_BLOCK ? 1 : 2 + (posIndex / Comm.LAST_BLOCK - 1) / 4;
            int metaBlockOffset = posIndex < Comm.LAST_BLOCK ? 0 : (posIndex / Comm.LAST_BLOCK - 1) % 4;
            int mapBlockNum = (readCached(baseBlock * Comm.BLOCK_SIZE + metaBlockNum) >>> (8 * metaBlockOffset)) & 0x000000ff;
            Comm.writeBlock(mapBlockNum, 1 + (posIndex % Comm.LAST_BLOCK), data);
        }
    }
    
    // increments queue index
    private static int nextInQueue(int val) {
        val++;
        if (val > LAST_CHAN) return FIRST_CHAN;
        if (val % Comm.BLOCK_SIZE == 0) return val + 1;
        return val;
    }
    
    // Reads queue index from multi block queue
    private static int readFromQueue(int baseBlock, int head) throws GameActionException {
        int queueNum = head / Comm.BLOCK_SIZE;
        if (queueNum == 0) {
            return Comm.readBlock(baseBlock, head);
        } else {
            int queueMeta = readCached(baseBlock * Comm.BLOCK_SIZE + 1);
            int queueContId = (queueMeta >>> (8 * queueNum)) & 0x000000ff;
            return Comm.readBlock(queueContId, head % Comm.BLOCK_SIZE);
        }
    }
    
    // Writes queue index to multi block queue
    private static void writeToQueue(int baseBlock, int tail, int data) throws GameActionException {
        int queueNum = tail / Comm.BLOCK_SIZE;
        if (queueNum == 0) {
            Comm.writeBlock(baseBlock, tail, data);
        } else {
            int queueMeta = readCached(baseBlock * Comm.BLOCK_SIZE + 1);
            int queueContId = (queueMeta >>> (8 * queueNum)) & 0x000000ff;
            Comm.writeBlock(queueContId, (tail % Comm.BLOCK_SIZE), data);
        }
    }
    
    // Reads a channel and caches the result (only to be used for constant metadata values
    private static int readCached(int chan) throws GameActionException {
        if (cache[chan] == 0) {
            cache[chan] = Handler.rc.readBroadcast(chan);
        }
        return cache[chan];
    }
    
}

