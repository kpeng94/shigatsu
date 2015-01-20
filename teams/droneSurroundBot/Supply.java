package droneSurroundBot;

import battlecode.common.*;

public class Supply {
	public static final int DEFAULT_THRESHOLD = 25;

	public static void spreadSupplies(int threshold) throws GameActionException {
		double totalSupply = Handler.rc.getSupplyLevel();
		int numBots = 1;
		double minSupply = totalSupply;
		MapLocation minLocation = Handler.myLoc;
		
		RobotInfo[] nearbyRobots = Handler.rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, Handler.myTeam);
		for (int i = nearbyRobots.length; --i >= 0;) {
			RobotInfo robot = nearbyRobots[i];
			if (robot.type == RobotType.MISSILE || robot.type.isBuilding) continue;
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
	
	/*-------------------------------- SUPPLIER FUNCTIONS --------------------------------*/
	
	private static int NUM_SUPPLIER_OFF = 0;
	private static int LAST_UPDATED_OFF = 1;
	private static int LOC_X_OFF = 2;
	private static int LOC_Y_OFF = 3;
	private static int TAR_ACTIVE = 10;
	private static int TAR_X_OFF = 11;
	private static int TAR_Y_OFF = 12;
	private static int TAR_TYPE_OFF = 13;
	
	private static MapLocation dumpLoc;
	private static RobotType dumpTarget;
	
	/* The following functions should be called by HQ */
	
	public static void incExpiration() throws GameActionException {
		int age = Comm.readBlock(getSupplierId(), LAST_UPDATED_OFF);
		Comm.writeBlock(getSupplierId(), LAST_UPDATED_OFF, age + 1);
	}

	public static void distributeHQSupply() throws GameActionException {
		Supply.spreadSupplies(Supply.DEFAULT_THRESHOLD);
		Handler.rc.setIndicatorString(0, "Supplier x: " + Comm.readBlock(getSupplierId(), LOC_X_OFF));
		Handler.rc.setIndicatorString(1, "Supplier y: " + Comm.readBlock(getSupplierId(), LOC_Y_OFF));
		
		if (!supplierNeeded()) {
			MapLocation supplierLoc = new MapLocation(Comm.readBlock(getSupplierId(), LOC_X_OFF),
				Comm.readBlock(getSupplierId(), LOC_Y_OFF));
			if (Handler.myLoc.distanceSquaredTo(supplierLoc) < GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED && Handler.rc.senseRobotAtLocation(supplierLoc) != null) {
				Handler.rc.transferSupplies((int) Handler.rc.getSupplyLevel(), supplierLoc);
			}
		}
	}
	
	/* This function can be called by anyone */
	
	public static void dumpSupplyTo(MapLocation loc, RobotType unit) throws GameActionException {
		Comm.writeBlock(getSupplierId(), TAR_ACTIVE, 1);
		Comm.writeBlock(getSupplierId(), TAR_X_OFF, loc.x);
		Comm.writeBlock(getSupplierId(), TAR_Y_OFF, loc.y);
		Comm.writeBlock(getSupplierId(), TAR_TYPE_OFF, unit.ordinal());
	}
	
	public static void deactivateDump() throws GameActionException {
		Comm.writeBlock(getSupplierId(), TAR_ACTIVE, 0);
	}
	
	/* The remaining functions should be called by drones */
	
	public static boolean supplierNeeded() throws GameActionException {
		return Comm.readBlock(getSupplierId(), NUM_SUPPLIER_OFF) < 1 || Comm.readBlock(getSupplierId(), LAST_UPDATED_OFF) > 5;
	}
	
	public static void initSupplier() throws GameActionException {
		Comm.writeBlock(getSupplierId(), NUM_SUPPLIER_OFF, 1);
	}
	
	public static void execSupplier() throws GameActionException {
		int mySupply = (int) Handler.rc.getSupplyLevel();
		
		checkDumpDetails();
		
		if (Handler.rc.isCoreReady()) {
			if (mySupply < 2000 || dumpLoc == null || dumpTarget == null) {
				Handler.rc.move(NavSuperSafeBug.dirToBugIn(Handler.myHQ));
			} else {
				Handler.rc.move(NavSuperSafeBug.dirToBugIn(dumpLoc));
			}
		}
		
		dumpIfPossible(mySupply);
		update();
	}
	
	private static void checkDumpDetails() throws GameActionException {
		if (Comm.readBlock(getSupplierId(), TAR_ACTIVE) == 1) {
			dumpLoc = new MapLocation(
					Comm.readBlock(getSupplierId(), TAR_X_OFF),
					Comm.readBlock(getSupplierId(), TAR_Y_OFF));
			dumpTarget = RobotType.values()[Comm.readBlock(getSupplierId(), TAR_TYPE_OFF)];
		} else {
			dumpLoc = null;
			dumpTarget = null;
		}
	}
	
	private static void dumpIfPossible(int supplyLevel) throws GameActionException {
		if (dumpLoc != null && dumpTarget != null && Handler.myLoc.distanceSquaredTo(dumpLoc) < GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED) {
			RobotInfo[] allies = Handler.rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, Handler.myTeam);
			for (RobotInfo ally: allies) {
				if (ally.type == dumpTarget) {
					Handler.rc.transferSupplies(supplyLevel, ally.location);
					return;
				}
			}
		}
	}
	
	private static void update() throws GameActionException {
		Comm.writeBlock(getSupplierId(), LAST_UPDATED_OFF, 0);
		Comm.writeBlock(getSupplierId(), LOC_X_OFF, Handler.myLoc.x);
		Comm.writeBlock(getSupplierId(), LOC_Y_OFF, Handler.myLoc.y);
	}
	
	/*-------------------------------- COMM FUNCTIONS --------------------------------*/
	
	public static final int SUPPLIER_BLOCK = 222;
	public static int supplierBlockId = 0;
	
	public static int getSupplierId() throws GameActionException {
		if (supplierBlockId == 0) {
			supplierBlockId = Handler.rc.readBroadcast(SUPPLIER_BLOCK);
			if (supplierBlockId == 0) {
				supplierBlockId = Comm.requestBlock(true);
				Handler.rc.broadcast(SUPPLIER_BLOCK, supplierBlockId);
			}
		}
		return supplierBlockId;
	}
}
