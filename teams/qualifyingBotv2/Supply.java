package qualifyingBotv2;

import battlecode.common.*;

public class Supply {
	public static final int DEFAULT_THRESHOLD = 25;

	public static void spreadSupplies(int threshold) throws GameActionException {
		double totalSupply = Handler.rc.getSupplyLevel();
		int numBots = 1;
		double minSupply = totalSupply;
		MapLocation minLocation = Handler.myLoc;
		RobotInfo commander = null;
		
		RobotInfo[] nearbyRobots = Handler.rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, Handler.myTeam);
		for (int i = nearbyRobots.length; --i >= 0;) {
			RobotInfo robot = nearbyRobots[i];
			if (robot.type == RobotType.MISSILE || robot.type.isBuilding || robot.type == RobotType.DRONE) continue;
			if (robot.type == RobotType.COMMANDER) {
				commander = robot;
				continue;
			}
			totalSupply += robot.supplyLevel;
			numBots++;
			if (robot.supplyLevel < minSupply) {
				minSupply = robot.supplyLevel;
				minLocation = robot.location;
			}
		}
		
		double avgSupply = totalSupply / numBots;
		if (Handler.rc.getSupplyLevel() > avgSupply) { // Should transfer supply
			if (commander != null) {
				int neededSupply = (int) (20 * (Handler.rc.getRoundLimit() - Clock.getRoundNum()) - commander.supplyLevel);
				int availableSupply = (int) (Handler.rc.getSupplyLevel() - avgSupply);
				if (neededSupply > 0) {
					Handler.rc.transferSupplies((availableSupply > neededSupply ? neededSupply : availableSupply), commander.location);
				}
			}
		}
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
	
	// This method is faster because nearbyRobots is passed in
	public static void spreadSupplies(int threshold, RobotInfo[] nearbyRobots) throws GameActionException {
		double totalSupply = Handler.rc.getSupplyLevel();
		int numBots = 1;
		double minSupply = totalSupply;
		MapLocation minLocation = Handler.myLoc;
		RobotInfo commander = null;
		
		for (int i = nearbyRobots.length; --i >= 0;) {
			RobotInfo robot = nearbyRobots[i];
			if (robot.type == RobotType.MISSILE || robot.type.isBuilding || robot.type == RobotType.DRONE) continue;
			if (robot.type == RobotType.COMMANDER) {
				commander = robot;
				continue;
			}
			totalSupply += robot.supplyLevel;
			numBots++;
			if (robot.supplyLevel < minSupply) {
				minSupply = robot.supplyLevel;
				minLocation = robot.location;
			}
		}
		
		double avgSupply = totalSupply / numBots;
		if (Handler.rc.getSupplyLevel() > avgSupply) { // Should transfer supply
			if (commander != null) {
				int neededSupply = (int) (20 * (Handler.rc.getRoundLimit() - Clock.getRoundNum()) - commander.supplyLevel);
				int availableSupply = (int) (Handler.rc.getSupplyLevel() - avgSupply);
				if (neededSupply > 0) {
					Handler.rc.transferSupplies((availableSupply > neededSupply ? neededSupply : availableSupply), commander.location);
				}
			}
		}
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
//	private static int LAST_UPDATED_OFF = 1;
	private static int LOC_OFF = 2;
	private static int TAR_ACTIVE = 10;
	private static int TAR_LOC_OFF = 11;
	private static int TAR_TYPE_OFF = 13;
	
	private static MapLocation dumpLoc;
	private static RobotType dumpTarget;
	private static int supplyBeforeLeaving;
	
	/* The following functions should be called by HQ */
	
	/**
	 * Increments the time since the supplier last checked in
	 */
	public static void incExpiration() throws GameActionException {
//		int age = Comm.readBlock(getSupplierId(), LAST_UPDATED_OFF);
//		Comm.writeBlock(getSupplierId(), LAST_UPDATED_OFF, age + 1);
		Comm.writeBlock(getSupplierId(), NUM_SUPPLIER_OFF, 0);
	}

	/**
	 * Averages supply with nearby units
	 * Dump all supply onto supplier unit if in range
	 */
	public static void distributeHQSupply() throws GameActionException {
		Supply.spreadSupplies(Supply.DEFAULT_THRESHOLD);
		
		if (!supplierNeeded() && Comm.readBlock(getSupplierId(), TAR_ACTIVE) == 1) {
			MapLocation supplierLoc = MapUtils.decode(Comm.readBlock(getSupplierId(), LOC_OFF));
			int mySupply = (int) Handler.rc.getSupplyLevel();
			if (Handler.myLoc.distanceSquaredTo(supplierLoc) < GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED && Handler.rc.senseRobotAtLocation(supplierLoc) != null
					&& mySupply > 1000) {
				Handler.rc.transferSupplies(mySupply - 1000, supplierLoc);
			}
		}
	}
	
	/* This function can be called by anyone */
	
	/**
	 * Writes the dump criteria (location and type of robot to receive supply)
	 */
	public static void dumpSupplyTo(MapLocation loc, RobotType unit) throws GameActionException {
		if (Comm.readBlock(getSupplierId(), TAR_ACTIVE) == 0) {
			Comm.writeBlock(getSupplierId(), TAR_ACTIVE, 1);
			Comm.writeBlock(getSupplierId(), TAR_LOC_OFF, MapUtils.encode(loc));
			Comm.writeBlock(getSupplierId(), TAR_TYPE_OFF, unit.ordinal());
		}
	}
	
	/**
	 * Disable the previous dumpSupplyTo call 
	 * The supplier unit will remain close to HQ
	 */
	public static void deactivateDump() throws GameActionException {
		Comm.writeBlock(getSupplierId(), TAR_ACTIVE, 0);
	}
	
	/* The remaining functions should be called by drones */
	
	/**
	 * Returns true if there are no suppliers or 
	 * it's been a while since the supplier checked in
	 */
	public static boolean supplierNeeded() throws GameActionException {
		return Comm.readBlock(getSupplierId(), NUM_SUPPLIER_OFF) < 1;
	}
	
	/**
	 * Initialize variables associated with the supplier
	 */
	public static void initSupplier() throws GameActionException {
		Comm.writeBlock(getSupplierId(), NUM_SUPPLIER_OFF, 1);
		supplyBeforeLeaving = 0;
	}
	
	/**
	 * Execute code for the supplier
	 * If there's dump criteria, and supply is above a threshold, move to the target location
	 * Otherwise, move back to the HQ
	 */
	public static void execSupplier() throws GameActionException {
		int mySupply = (int) Handler.rc.getSupplyLevel();
		if (mySupply > supplyBeforeLeaving) {
			supplyBeforeLeaving = mySupply;
		}
		
		checkDumpDetails();
		
		if (Handler.rc.isCoreReady()) {
			if (mySupply < 2000 || dumpLoc == null || dumpTarget == null) {
				Direction nextDir = NavSuperSafeBug.dirToBugIn(Handler.myHQ);
				if (nextDir != null && nextDir != Direction.NONE)
					Handler.rc.move(nextDir);
			} else {
				Direction nextDir = NavSuperSafeBug.dirToBugIn(dumpLoc);
				if (nextDir != null && nextDir != Direction.NONE)
					Handler.rc.move(nextDir);
				dumpIfPossible(mySupply);
			}
		}
		update();
	}
	
	/** Reads the dump target criteria for the supplier */
	private static void checkDumpDetails() throws GameActionException {
		if (Comm.readBlock(getSupplierId(), TAR_ACTIVE) == 1) {
			dumpLoc = MapUtils.decode(Comm.readBlock(getSupplierId(), TAR_LOC_OFF));
			dumpTarget = RobotType.values()[Comm.readBlock(getSupplierId(), TAR_TYPE_OFF)];
		} else {
			dumpLoc = null;
			dumpTarget = null;
		}
	}
	
	/**
	 * Supplier dumps on any unit nearby that satisfies the dump target criteria
	 */
	private static void dumpIfPossible(int supplyLevel) throws GameActionException {
		if (dumpLoc != null && dumpTarget != null && Handler.myLoc.distanceSquaredTo(dumpLoc) < GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED) {
			RobotInfo[] allies = Handler.rc.senseNearbyRobots(GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, Handler.myTeam);
			for (RobotInfo ally: allies) {
				int toTransfer = 2 * supplyLevel - supplyBeforeLeaving;
				if (ally.type == dumpTarget && toTransfer > 0) {
					Handler.rc.transferSupplies(toTransfer, ally.location);
					deactivateDump();
					supplyBeforeLeaving = 0;
					return;
				}
			}
		}
	}
	
	/** Supplier checks in */
	private static void update() throws GameActionException {
//		Comm.writeBlock(getSupplierId(), LAST_UPDATED_OFF, 0);
		int numSuppliers = Comm.readBlock(getSupplierId(), NUM_SUPPLIER_OFF);
		Comm.writeBlock(getSupplierId(), NUM_SUPPLIER_OFF, numSuppliers + 1);
		Comm.writeBlock(getSupplierId(), LOC_OFF, MapUtils.encode(Handler.myLoc));
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
