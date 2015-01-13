package pusheenBot;

import battlecode.common.*;

public class Mining {

    public static MapLocation findClosestMinableOreWithRespectToHQ(double threshold, int stepLimit){
        int step = 1;
        MapLocation currentLocation = Handler.myLoc;
        Direction currentDirection = currentLocation.directionTo(Handler.myHQ);
        if(currentDirection.isDiagonal())
            currentDirection = currentDirection.rotateRight();
        int bestDistance = 2000000;
        MapLocation bestLocation = null;

        while (step < stepLimit) {
            for (int i = step; --i >= 0;) {
                currentLocation = currentLocation.add(currentDirection);
                int distance = currentLocation.distanceSquaredTo(Handler.myHQ);
                if (Handler.rc.senseOre(currentLocation) > threshold && distance < bestDistance && Handler.rc.senseNearbyRobots(currentLocation, 0, Handler.myTeam).length == 0){
                    bestLocation = currentLocation;
                    bestDistance = distance;
                }
            }

            if(step > 2 && bestDistance < 2000000){
                return bestLocation;
            }
            currentDirection = currentDirection.rotateLeft().rotateLeft();
            
            for (int i = step; --i >= 0;) {
                currentLocation = currentLocation.add(currentDirection);                
                int distance = currentLocation.distanceSquaredTo(Handler.myHQ);
                if (Handler.rc.senseOre(currentLocation) > threshold && distance < bestDistance && Handler.rc.senseNearbyRobots(currentLocation, 0, Handler.myTeam).length == 0){
                    bestLocation = currentLocation;
                    bestDistance = distance;
                }
            }
            currentDirection = currentDirection.rotateLeft().rotateLeft();
            step++;
        }
        
        return null;
    }
	
	public static MapLocation findRangeNOre(double threshold, int range, boolean[][] occupied) {
		MapLocation[] locations = MapLocation.getAllMapLocationsWithinRadiusSq(Handler.myLoc, range);
		int minDist = 99999;
		MapLocation minLoc = null;
		for (int i = locations.length; --i >= 0;) {
			MapLocation loc = locations[i];
			if (loc.equals(Handler.myLoc)) continue;
			int dist = loc.distanceSquaredTo(Handler.myHQ);
			if (dist < minDist) {
				MapLocation convertedLoc = MapUtils.encodeMapLocation(loc);
				if (!occupied[convertedLoc.x][convertedLoc.y] && Handler.rc.senseOre(loc) > threshold) {
					minDist = dist;
					minLoc = loc;
				}
			}
		}
		return minLoc;
	}
	
	public static boolean[][] getOccupiedTiles(int range) {
		RobotInfo[] robots = Handler.rc.senseNearbyRobots(range, Handler.myTeam);
		boolean[][] occupied = new boolean[GameConstants.MAP_MAX_WIDTH][GameConstants.MAP_MAX_HEIGHT];
		for (int i = robots.length; --i >= 0;) {
			MapLocation convertedLocation = MapUtils.encodeMapLocation(robots[i].location);
			occupied[convertedLocation.x][convertedLocation.y] = true;
		}
		return occupied;
	}
	
}
