package tankBotv3;

import battlecode.common.*;

public class Supply {
	public static final int DEFAULT_THRESHOLD = 100;

	public static void spreadSupplies(int threshold) throws GameActionException {
		double totalSupply = Handler.rc.getSupplyLevel();
		int numBots = 1;
		double minSupply = totalSupply;
		MapLocation minLocation = Handler.myLoc;
		
		RobotInfo[] nearbyRobots = Handler.rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, Handler.myTeam);
		for (int i = nearbyRobots.length; --i >= 0;) {
			RobotInfo robot = nearbyRobots[i];
			if (robot.type == RobotType.MISSILE) continue;
			totalSupply += robot.supplyLevel;
			numBots++;
			if (robot.supplyLevel < minSupply) {
				minSupply = robot.supplyLevel;
				minLocation = robot.location;
			}
		}
		
		double avgSupply = totalSupply / numBots;
		if (Handler.rc.getSupplyLevel() > avgSupply) { // Should transfer supply
			double over = Handler.rc.getSupplyLevel() - avgSupply;
			double under = avgSupply - minSupply;
			if (over > threshold && under >= over) {
				Handler.rc.transferSupplies((int) over, minLocation);
			} else if (under > threshold && over > under) {
				Handler.rc.transferSupplies((int) under, minLocation);
			}
		}
	}
	
}
