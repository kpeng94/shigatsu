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
		int dirInt = dir.ordinal();
		Direction[] d = {dir, dirs[(dirInt + 1) % 8], dirs[(dirInt + 7) % 8], dirs[(dirInt + 2) % 8],
						dirs[(dirInt + 6) % 8], dirs[(dirInt + 3) % 8], dirs[(dirInt + 5) % 8],
						dirs[(dirInt + 4) % 8]};
		return d;
	}
	
	public static Direction[] directionsTowardsRev(Direction dir) {
		int dirInt = dir.ordinal();
		Direction[] d = {dirs[(dirInt + 4) % 8], dirs[(dirInt + 5) % 8],
						dirs[(dirInt + 3) % 8], dirs[(dirInt + 6) % 8], dirs[(dirInt + 2) % 8],
						dirs[(dirInt + 7) % 8], dirs[(dirInt + 1) % 8], dir};
		return d;
	}

}