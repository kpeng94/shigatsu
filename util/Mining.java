package pusheenBot;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Mining {

	/**
	 * Calculates the closest square with at least the threshold amount of 
	 * ore. The distance is calculated in terms of Manhattan distance and NOT
	 * Euclidean distance. This does NOT factor in the square the robot is currently on.
	 * Ignores squares with other robots on them already
	 * 
	 * @param rc - RobotController for the robot
	 * @param threshold - the minimum amount of ore for the function to return
	 * @param stepLimit - the size of the search outwards (a step limit of n will search in
	 * 					  a [n by n] square, centered about the robot's current location
	 * @return - MapLocation of closest square with ore greater than the threshold, 
	 *           or null if there is none
	 */
	public static MapLocation findClosestMinableOre(RobotController rc, double threshold, int stepLimit, Direction startDirection) {
		int step = 1;
		Direction currentDirection = startDirection;
		MapLocation currentLocation = rc.getLocation();

		while (step < stepLimit) {
			for (int i = 0; i < step; i++) {
				currentLocation = currentLocation.add(currentDirection);
				if (rc.senseOre(currentLocation) > threshold && rc.canMove(currentDirection))
					return currentLocation;
			}
			currentDirection = currentDirection.rotateLeft().rotateLeft();
			for (int i = 0; i < step; i++) {
				currentLocation = currentLocation.add(currentDirection);
				if (rc.senseOre(currentLocation) > threshold && rc.canMove(currentDirection))
					return currentLocation;
			}
			currentDirection = currentDirection.rotateLeft().rotateLeft();
			
			step++;
		}

		return null;
	}
	
}
