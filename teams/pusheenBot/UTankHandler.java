package pusheenBot;

import battlecode.common.*;

public class UTankHandler extends UnitHandler {
	private static boolean attackState;

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
				e.printStackTrace();
				System.out.println(typ + " Execution Exception");
			}
			rc.yield(); // Yields to save remaining bytecodes
		}
	}

	protected static void init(RobotController rcon) throws GameActionException {
		initUnit(rcon);
	}

	protected static void execute() throws GameActionException {
		executeUnit();
//		if (Count.getCount(Comm.getTankId()) > 20 || Clock.getRoundNum() > 1750) {
//			attackState = true;
//		}
		Count.incrementBuffer(Comm.getTankId());
		if (attackState) {
			if (rc.isWeaponReady()) {
				Attack.tryAttackPrioritizeTowers();
			}
			if (rc.isCoreReady()) {
				if (enemyTowers.length > 0) {
					if (myLoc.distanceSquaredTo(enemyTowers[0]) > 35) {
						Direction dir = NavSafeBug.dirToBugIn(enemyTowers[0]);
						if (dir != Direction.NONE) {
							rc.move(dir);
						}
					} else {
						NavSimple.walkTowards(myLoc.directionTo(enemyTowers[0]));
					}
				} else {
					if (myLoc.distanceSquaredTo(enemyHQ) > 52) {
						Direction dir = NavSafeBug.dirToBugIn(enemyHQ);
						if (dir != Direction.NONE) {
							rc.move(dir);
						}
					} else {
						NavSimple.walkTowards(myLoc.directionTo(enemyHQ));
					}
				}
			}
		} else {
			if (rc.isWeaponReady()) {
				Attack.tryAttackClosestButKillIfPossible();
			}
			if (rc.isCoreReady()) {
				NavTangentBug.setDest(enemyHQ);
				NavTangentBug.calculate(2500);
				Direction dir = NavTangentBug.getNextMove();
				if (dir != Direction.NONE) {
					NavSimple.walkTowards(dir);
				}
			}
		}
		Supply.spreadSupplies(Supply.DEFAULT_THRESHOLD);
	}
	
}
