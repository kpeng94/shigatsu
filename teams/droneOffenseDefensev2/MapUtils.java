package droneOffenseDefensev2;

import battlecode.common.*;

public class MapUtils {
	public static final Direction[] dirs = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	
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
	
	public static int directionToInt(Direction d) {
		switch (d) {
		case NORTH:
			return 0;
		case NORTH_EAST:
			return 1;
		case EAST:
			return 2;
		case SOUTH_EAST:
			return 3;
		case SOUTH:
			return 4;
		case SOUTH_WEST:
			return 5;
		case WEST:
			return 6;
		case NORTH_WEST:
			return 7;
		default:
			return -1;
	}
}
	
}