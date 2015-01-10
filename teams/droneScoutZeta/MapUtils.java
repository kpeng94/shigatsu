package droneScoutZeta;

import battlecode.common.*;

public class MapUtils {
	public static final Direction[] dirs = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	private static final int[] discBoundOffsets = {0, 1, 2, 3};
	
	public static MapLocation pointSection(MapLocation point1, MapLocation point2, double weight){
		int newX = (int)(point1.x * weight + point2.x * (1.0-weight));
		int newY = (int)(point1.y * weight + point2.y * (1.0-weight));
		return new MapLocation(newX, newY);
	}
	
	public static MapLocation convertMapLocation(MapLocation base) {
		int newx = base.x % GameConstants.MAP_MAX_WIDTH;
		if (newx < 0) newx += GameConstants.MAP_MAX_WIDTH;
		int newy = base.y % GameConstants.MAP_MAX_HEIGHT;
		if (newy < 0) newy += GameConstants.MAP_MAX_HEIGHT;
		return new MapLocation(newx, newy);
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
	
	// Bounds are ordered minX, maxX, minY, maxY
	public static void initBounds() throws GameActionException {
		Comm.writeBlock(Comm.getMapId(), discBoundOffsets[0], Integer.MAX_VALUE);
		Comm.writeBlock(Comm.getMapId(), discBoundOffsets[1], 0);
		Comm.writeBlock(Comm.getMapId(), discBoundOffsets[2], Integer.MAX_VALUE);
		Comm.writeBlock(Comm.getMapId(), discBoundOffsets[3], 0);
	}
	
	public static int[] getDiscoveredBounds() throws GameActionException {
		int[] bounds = new int[4];
		for (int i = 0; i < 4; i++) {
			bounds[i] = Comm.readBlock(Comm.getMapId(), discBoundOffsets[i]);
		}
		return bounds;
	}
	
	public static void updateBounds(MapLocation loc) throws GameActionException {
		int[] bounds = getDiscoveredBounds();
		if (loc.x < bounds[0]) {
			bounds[0] = loc.x;
		}
		if (loc.x > bounds[1]) {
			bounds[1] = loc.x;
		}
		if (loc.y < bounds[2]) {
			bounds[2] = loc.y;
		}
		if (loc.y > bounds[3]) {
			bounds[3] = loc.y;
		}
		writeBounds(bounds);
	}
	
	public static void writeBounds(int[] bounds) throws GameActionException {
		for (int i = 0; i < 4; i++) {
			Comm.writeBlock(Comm.getMapId(), discBoundOffsets[i], bounds[i]);
		}
	}

}