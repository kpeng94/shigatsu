package defenseAndSpawnBot;

import battlecode.common.*;

public class MapUtils {
	public static final Direction[] dirs = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	
	public static MapLocation pointSection(MapLocation point1, MapLocation point2, double weight){
		int newX = (int)(point1.x * weight + point2.x * (1.0-weight));
		int newY = (int)(point1.y * weight + point2.y * (1.0-weight));
		return new MapLocation(newX, newY);
	}
	
	public static Direction[] directionsTowards(Direction dir) {
		Direction[] d = {dir, dir.rotateRight(), dir.rotateLeft(), dir.rotateRight().rotateRight(),
						dir.rotateLeft().rotateLeft(), dir.opposite().rotateLeft(), dir.opposite().rotateRight(),
						dir.opposite()};
		return d;
	}
	
	public static Direction[] directionsTowardsRev(Direction dir) {
		Direction[] d = {dir.opposite(), dir.opposite().rotateRight(), dir.opposite().rotateLeft(),
						dir.rotateLeft().rotateLeft(), dir.rotateRight().rotateRight(), dir.rotateLeft(),
						dir.rotateRight(), dir};
		return d;
	}

}