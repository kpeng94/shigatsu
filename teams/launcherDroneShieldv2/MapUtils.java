package launcherDroneShieldv2;

import battlecode.common.*;

public class MapUtils {
	private static final int HASH = 120;
	public static final Direction[] dirs = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	public static final Direction[] dirsDiagFirst = {Direction.NORTH_WEST, Direction.SOUTH_WEST, Direction.SOUTH_EAST, Direction.NORTH_EAST, Direction.WEST, Direction.SOUTH, Direction.EAST, Direction.NORTH};
	public static final Direction[] dirsCardinal = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

	public static MapLocation pointSection(MapLocation point1, MapLocation point2, double weight){
		int newX = (int)(point1.x * weight + point2.x * (1.0-weight));
		int newY = (int)(point1.y * weight + point2.y * (1.0-weight));
		return new MapLocation(newX, newY);
	}
	
	public static MapLocation encodeMapLocation(MapLocation pos) {
		int x = ((pos.x - Handler.myHQ.x) + HASH) % HASH;
		int y = ((pos.y - Handler.myHQ.y) + HASH) % HASH;
		return new MapLocation(x, y);
	}
	
	public static int encode(MapLocation pos) {
		int x = pos.x - Handler.myHQ.x;
		int y = pos.y - Handler.myHQ.y;
		x = (x < 0) ? (x + HASH) | 0x00000080 : x;
		y = (y < 0) ? (y + HASH) | 0x00000080 : y;
		
		return (x << 8) + y;
	}
	
	public static int unsignEncoding(int pos) {
		pos = pos & 0x00007f7f;
		return ((pos >>> 8) & 0x000000ff) * HASH + (pos & 0x000000ff);
	}
	
	public static MapLocation decode(int pos) {
		int x = (pos >>> 8) & 0x000000ff;
		int y = pos & 0x000000ff;
		x = ((x & 0x00000080) == 0x00000080) ? (x & 0x0000007f) - HASH : x;
		y = ((y & 0x00000080) == 0x00000080) ? (y & 0x0000007f) - HASH : y;
		return new MapLocation(Handler.myHQ.x + x, Handler.myHQ.y + y);
	}
	
	public static Direction[] dirsAround(Direction dir) {
		return new Direction[]{dir, dir.rotateRight(), dir.rotateLeft(), dir.rotateRight().rotateRight(),
							dir.rotateLeft().rotateLeft(), dir.opposite().rotateLeft(), dir.opposite().rotateRight(),
							dir.opposite()};
	}
	
	public static Direction[] dirsAroundRev(Direction dir) {
		return new Direction[]{dir.opposite(), dir.opposite().rotateRight(), dir.opposite().rotateLeft(),
							dir.rotateLeft().rotateLeft(), dir.rotateRight().rotateRight(), dir.rotateLeft(),
							dir.rotateRight(), dir};
	}
	
	public static Direction[] dirsTowards(Direction dir) {
		return new Direction[]{dir, dir.rotateRight(), dir.rotateLeft()};
	}
	
	public static Direction[] dirsTowardsRev(Direction dir) {
		return new Direction[]{dir.rotateLeft(), dir.rotateRight(), dir};
	}

}