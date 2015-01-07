// package here

import battlecode.common.*;

public class MapUtils {

	public static MapLocation pointSection(MapLocation point1, MapLocation point2, double weight){
		int newX = (int)(point1.x * weight + point2.x * (1.0-weight));
		int newY = (int)(point1.y * weight + point2.y * (1.0-weight));
		return new MapLocation(newX, newY);
	}

}