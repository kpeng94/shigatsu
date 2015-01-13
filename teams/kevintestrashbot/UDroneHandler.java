package kevintestrashbot;

import droneScoutRecall.Scout;
import battlecode.common.*;

public class UDroneHandler extends UnitHandler {
	
	private enum DroneType {
		SCOUT, COURIER;
	}
	
	private static DroneType myType;

	public static void loop(RobotController rcon) {
		try {
			init(rcon);
		} catch (Exception e) {
			// e.printStackTrace();
			System.out.println(typ + " Initialization Exception");
		}

		while (true) {
			try {
				execute();
			} catch (Exception e) {
				// e.printStackTrace();
				System.out.println(typ + " Execution Exception");
			}
			rc.yield(); // Yields to save remaining bytecodes
		}
	}

	protected static void init(RobotController rcon) throws GameActionException {
		initUnit(rcon);
		if (Scout.needed()) {
			myType = DroneType.SCOUT;
			Scout.init();
		} else {
			myType = DroneType.COURIER;
		}
	}

	protected static void execute() throws GameActionException {
		executeUnit();
		if (myType == DroneType.SCOUT)
			Scout.execute();
		else {
			if (rc.getSupplyLevel() > 4000) {
				RobotInfo[] allies = rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, myTeam);
				int numNearbyLaunchers = 0;
				for (RobotInfo ally: allies) {
					if (ally.type == RobotType.LAUNCHER) {
						numNearbyLaunchers++;
						rc.transferSupplies((int) rc.getSupplyLevel(), ally.location);
					}
				}
				if (numNearbyLaunchers == 0) {
					int rushing = Comm.readBlock(Comm.getLauncherId(), 4);
					if (rushing == 1) {
						int minDistance = Integer.MAX_VALUE;
						MapLocation closestLocation = null;
						if (enemyTowers.length == 0) {
							closestLocation = enemyHQ;
						}
						for (int i = enemyTowers.length; --i >= 0;) {
							int distanceSquared = myHQ.distanceSquaredTo(enemyTowers[i]);
							if (distanceSquared <= minDistance) {
								closestLocation = enemyTowers[i];
								minDistance = distanceSquared;
							}
						}
						NavSimple.walkTowards(myLoc.directionTo(closestLocation.add(myHQToEnemyHQ, -4)));
					} else {
						NavSimple.walkTowards(myLoc.directionTo(MapUtils.pointSection(myHQ, enemyHQ, 0.75)));
					}
				}
			} else {
				NavSimple.walkTowards(myLoc.directionTo(myHQ));
			}
		}
	}
	
}
