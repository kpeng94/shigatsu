package ryoutabot;

import copyPusheenBot.Debug;
import copyPusheenBot.MapUtils;
import battlecode.common.*;

public class USoldierHandler extends UnitHandler {
	public static MapLocation rallyPoint = null;

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
	}

	protected static void execute() {
		executeUnit();
		Utils.updateEnemyInfo();
		
		if (rallyPoint == null) { // Check if rally point exists
			int rally = rc.readBroadcast(Comm.SURROUND_RALLY_DEST_CHAN);
			if (rally > 0) {
				rallyPoint = MapUtils.decode(rally);
			}
		}
		
		if (visibleEnemies.length > 0) {
			soldierMicro();
		} else {
			soldierNav();
		}
	}
	
	/**
	 * 
	 * A very high level sketch of the soldier micro. Click on the links to see more details.
	 * This micro is to be used as long as there is a visible enemy in the sight radius of the soldier (TODO: enemy attacking us? probably not).
	 * 
	 * A soldier first takes the list of visible enemies, and looks at the number of enemies attacking it. 
	 * An enemy attacking it is defined to be another unit such that the soldier is in the enemies' attack radius, and the enemy is not building 
	 * anything. Cases that will have to be handled specially are [missiles + launchers, towers and HQ (if the soldier can see them in the sight range, then
	 * the soldier is "getting attacked", commanders (which can flash in and kill the soldier)).
	 * The soldier micro can be broken into a few sections:
	 * 
	 * Counting number of enemies attacking us:
	 *   -Positive number of enemies attacking us
	 *      -Exactly 1
	 *        -1v1
	 *          -against drone
	 *            -pretty even in a 1 on 1
	 *          -kiteable 1v1
	 *          
	 *          -raw strength 1v1
	 *            -beat up a miner, a beaver
	 *          -same unit 1v1
	 *        -
	 *        -0v1
	 *          -
	 *      -We outnumber, but need to calculate the strength of our teammates vs their teammates
	 *      -Movement with teammates around
	 *   -No enemies attacking us (we're not in their attack range)
	 *      -Enemies in our attack range (free hits on the enemy [most of the time])
	 *      -Enemies in our sight range (go on aggressive chase if it's a victorious battle - should only chase 1 enemy)
	 *      --{@link #chaseEnemy(RobotInfo)}
	 *   
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * {@link #do1v1Micro(RobotInfo)}
	 * 
	 * 
	 * 
	 * @throws GameActionException 
	 * 
	 */
	public static void soldierMicro() throws GameActionException {
		if (numEnemiesAttackingUs >= 1) {
			Debug.clockString(0, "There are a positive number of enemies attacking me.");
            // First we find out how our numbers are
            int maxNumAlliesAttackingEnemy = 0;
            for (int i = numEnemiesAttackingUs; --i >= 0;) {
                int numAlliesAttackingEnemy = Utils.numAlliesCanAttackEnemyAtLocation(Handler.enemiesAttackingUs[i].location);
                if (myLoc.distanceSquaredTo(Handler.enemiesAttackingUs[i].location) <= typ.attackRadiusSquared)
                    numAlliesAttackingEnemy++;
                if (numAlliesAttackingEnemy > maxNumAlliesAttackingEnemy)
                    maxNumAlliesAttackingEnemy = numAlliesAttackingEnemy;
            }
            
            if (numEnemiesAttackingUs == 1) {
                Debug.clockString(0, "got some enemies attacking us (1) and " + maxNumAlliesAttackingEnemy + "friends");
                if (maxNumAlliesAttackingEnemy == 0) {
                    // No one around us can attack the enemy, even me. This means that we're getting
                    // outranged by this unit, which is largely difficult. By default, we're just
                    // going to try to run away.
                    tryToRetreat();
                } else if (maxNumAlliesAttackingEnemy == 1) {
                    // This is currently a 1v1 fight. It might not be 1v1 between me and the enemy,
                    // though. The following check will figure that out.
                    RobotInfo enemy = findAnAttackingEnemy(attackableEnemies);
                    if (enemy == null) {
                        // We can't attack the enemy, so the enemy is actually having a 1v1 fight with
                        // someone else. As a result, since we have the numbers advantage, we should go in.
                        // Our count here reveals that the enemy can also attack us though, so the next
                        // line should work.
                        enemy = enemiesAttackingUs[0];
                        if (rc.isCoreReady()) {
                            tryMoveCloserToEnemy(enemy.location, 1, enemy.location != Handler.enemyHQ, true);
                            Debug.clockString(0, "1v1 move");
                            return true;
                        }
                        return true;
                    }
                    // Otherwise, it should be a 1v1.
                    soloMicro();
                } else {
                    // We outnumber the enemy. Great victory. Now, we beat it up.
                    // We try to find the enemy, which may not be in our attack radius.
                    RobotInfo enemy = findAnAttackingEnemy(attackableEnemies);

                    // The enemy can attack us, but we can't attack them. Since we're winning, we
                    // should try to move towards them.
                    if (enemy == null) {
                        enemy = enemiesAttackingUs[0];
                        if (rc.isCoreReady()) {
                            tryMoveCloserToEnemy(enemy.location, 1, enemy.location != Handler.enemyHQ, true);
                            Debug.clockString(0, "Outnumber move vs 1");
                            return true;
                        }
                        return true;
                    }
                    // If our weapon is ready, we should attack.
                    if (rc.isWeaponReady()) {
                        rc.attackLocation(enemy.location);
                        return true;
                    }
//                    } else {
//                        // Otherwise we should check if we're already in the range of the enemy.
//
//                        RobotInfo closestEnemyUnit = Utils.closestNonConstructingUnit(Handler.visibleEnemies);
//                        int numAlliesFighting = numAlliesInAttackRange(closestEnemyUnit.location);
//                        if (closestEnemyUnit != null && rc.isCoreReady()) {
//                            tryMoveCloserToEnemy(closestEnemyUnit.location,
//                                    numAlliesFighting + 1, closestEnemyUnit.location != Handler.enemyHQ,
//                                    false);
//                            Debug.clockString(0, "moving in to the party");
//
//                        } else {
//                            return true;
//                        }
//                    }

                }
            }
		}
	}
	
	/**
	 * Micro for 1v1 fights
	 * 
	 */
//	public static boolean do1v1Micro(RobotInfo enemy) {
//        boolean winning1v1 = isWinning1v1(enemy);
//        if (winning1v1) {
//            // We're winning a 1v1, so we try to fight the enemy
//            if (rc.isWeaponReady()) {
//                rc.attackLocation(enemy.location);
//                return true;
//            } else {
//                return true;
//            }
//        } else {
//            tryToRetreat();
//        }
//		
//	}

	/**
	 * TODO: kiting launchers, missiles
	 * The actual kiting micro
	 * @param enemy
	 * @return
	 * @throws GameActionException
	 */
	public static boolean do1v1Micro(RobotInfo enemy) throws GameActionException {
		Debug.clockString(0, "" + enemy.location);

		// We literally cannot deal with launchers in a 1v1. We should try some kind of running manuever.
		//
		if (enemy.type == RobotType.LAUNCHER) {
			
			return false;
		}
		
		if (enemy.type == RobotType.MISSILE) {
			// If we're right next to a missile with 1 HP, but the missile has a core delay >= 2 (possibly incurred from
			// taking a lot of diagonal moves), then we can make an attempt to distance ourselves before we shoot if we are
			// too close.
			if (enemy.coreDelay >= 2 && enemy.health == 1 && enemy.location.distanceSquaredTo(myLoc) <= 2) {
				
			}
			// If the enemy missile has 1 HP for some reason, shoot it down, as long as we're not right next to it.
			// TODO: update comment.
			if (enemy.health == 1 && rc.isWeaponReady() && rc.canAttackLocation(enemy.location)) {
				rc.attackLocation(enemy.location);
				return true;
			}
			return false;
		}
		
		// If our range is smaller than the enemy range, we can't kite them.
		// (None of the delays are high enough in this game for this to be feasible).
		// [V1.2.1] For soldiers, this is against tanks, and commanders
		if (typ.attackRadiusSquared < enemy.type.attackRadiusSquared) {
			return false;
		}
		
		// [1.2.1] Only Soldier vs soldier
		if (typ.attackRadiusSquared == enemy.type.attackRadiusSquared) {
			Debug.logEnemyInfo(0, enemy);
			Debug.logMyInfo(1);

			// Punish the enemy for having delay.
			if (enemy.weaponDelay >= 2.0 && rc.isWeaponReady() && rc.canAttackLocation(enemy.location)) {
				rc.attackLocation(enemy.location);
				return true;
			}
			// Check supply. Can only kite if the enemy doesn't have enough supply, so
			// the delays are down half as fast.
			// TODO: check this calculation
			if (enemy.supplyLevel < enemy.type.supplyUpkeep) {
				if (enemy.weaponDelay >= 1.5 && rc.isWeaponReady() && rc.canAttackLocation(enemy.location)) {
					rc.attackLocation(enemy.location);
					return true;
				}
			}
			
			// Enemy is not sufficiently delayed, so we should try moving out of their range if possible.
			if (rc.isCoreReady()) {
				Direction dirToEnemy = myLoc.directionTo(enemy.location);
				Direction[] dirsAroundRev = MapUtils.dirsAroundRev(dirToEnemy);
				int minDistance = Integer.MAX_VALUE;
				Direction dirToMove = null;
				for (int i = dirsAroundRev.length; --i >= 0;) {
					if (myLoc.add(dirsAroundRev[i]).distanceSquaredTo(enemy.location) > enemy.type.attackRadiusSquared &&
						rc.canMove(dirsAroundRev[i]) && !inRangeOfOtherTowers(dirsAroundRev[i], myLoc)) {
						int sumDxDy = dirsAroundRev[i].dx + dirsAroundRev[i].dy;
						if (sumDxDy < minDistance) {
							dirToMove = dirsAroundRev[i];
							minDistance = sumDxDy;
						}
					}
				}
				Debug.clockString(2, "" + minDistance + " direction chosen: " + dirToMove);
				// Got a direction to move, so move.
				if (dirToMove != null) {
					rc.move(dirToMove);
					return true;
				}
			}
			
			// We couldn't move for some reason, so we should try to shoot back if we still can.
			if (rc.isWeaponReady() && rc.canAttackLocation(enemy.location)) {
				rc.attackLocation(enemy.location);
				return true;
			}			
			return false;
		}
		
		// [1.2.1] Against units with less range
		// This means drones, bashers, miners.
		// We handle bashers separately first.
		if (enemy.type == RobotType.BASHER) {
			Debug.clockString(0, "BASHERS!!!");
			Debug.logEnemyInfo(1, enemy);
			int distanceToEnemy = enemy.location.distanceSquaredTo(myLoc);

			// This is our retreat case: If the enemy has a small enough core delay to move next turn
			// and it is in a radius that can hurt us.
			// We check for a core delay < 2 for this case (< 1 does not work well at all: if the enemy moves in that turn,
			// the core delay will never be less than 1).
			// TODO: supply check
			if (enemy.coreDelay < 2 && distanceToEnemy <= 8) {
				Direction dirToEnemy = myLoc.directionTo(enemy.location);
				Direction[] dirsAroundRev = MapUtils.dirsAroundRev(dirToEnemy);
				int minDistance = Integer.MAX_VALUE;
				Direction dirToMove = null;
				Debug.clockString(2, "Dir to enemy: " + dirToEnemy);
				for (int i = dirsAroundRev.length; --i >= 0;) {
					// [8] represents the effective range of the basher, meaning that if the basher moved in the 
					// next turn, it would be able to attack you up to [8] squares away.
					if (myLoc.add(dirsAroundRev[i]).distanceSquaredTo(enemy.location) > 8 &&
							rc.canMove(dirsAroundRev[i])) {
						int sumDxDy = dirsAroundRev[i].dx + dirsAroundRev[i].dy;
						if (sumDxDy < minDistance) {
							dirToMove = dirsAroundRev[i];
							minDistance = sumDxDy;
						}
					}
				}
				// Got a direction to move, so move.
				if (dirToMove != null) {
					rc.move(dirToMove);
					return true;
				}
			}
			
			if (rc.isWeaponReady() && rc.canAttackLocation(enemy.location)) {
				rc.attackLocation(enemy.location);
				return true;
			}
			return false;
		}
		
		// Vs miners, drones, beavers
		// Our priority here is to always get out of the way of the enemy.
		int distanceToEnemy = enemy.location.distanceSquaredTo(myLoc);
		if (enemy.coreDelay < 2 && distanceToEnemy <= enemy.type.attackRadiusSquared) {
			Direction dirToEnemy = myLoc.directionTo(enemy.location);
			Direction[] dirsAroundRev = MapUtils.dirsAroundRev(dirToEnemy);
			int minDistance = Integer.MAX_VALUE;
			Direction dirToMove = null;
			Debug.clockString(2, "Dir to enemy: " + dirToEnemy);
			for (int i = dirsAroundRev.length; --i >= 0;) {
				// [8] represents the effective range of the basher, meaning that if the basher moved in the 
				// next turn, it would be able to attack you up to [8] squares away.
				if (myLoc.add(dirsAroundRev[i]).distanceSquaredTo(enemy.location) > 8 &&
						rc.canMove(dirsAroundRev[i])) {
					int sumDxDy = dirsAroundRev[i].dx + dirsAroundRev[i].dy;
					if (sumDxDy < minDistance) {
						dirToMove = dirsAroundRev[i];
						minDistance = sumDxDy;
					}
				}
			}
			// Got a direction to move, so move.
			if (dirToMove != null) {
				rc.move(dirToMove);
				return true;
			}
		}
		
		if (rc.isWeaponReady() && rc.canAttackLocation(enemy.location)) {
			rc.attackLocation(enemy.location);
			return true;
		}
		return false;
	}
	
	/**
	 * This is the attack micro in a 1v1 against structures with no attack.
	 * It sounds simple, it is simple:
	 *   -If our weapon is ready and we can attack the enemy location, attack it.
	 *   -Otherwise, try walking towards the enemy location.
	 * 
	 * @return
	 * @throws GameActionException
	 */
	public static boolean attackStructure(RobotInfo enemy) throws GameActionException {
		if (rc.isWeaponReady() && rc.canAttackLocation(enemy.location)) {
			rc.attackLocation(enemy.location);
			return true;
		} else {
			if (rc.isCoreReady() && myLoc.distanceSquaredTo(enemy.location) > typ.attackRadiusSquared) {
				NavSimple.walkTowards(myLoc.directionTo(enemy.location));
			}
		}
		return false;
	}
	
	/**
	 * Assumes that only one enemy is visible and that the fight is a winning fight
	 * @param enemy
	 * @throws GameActionException 
	 * @return true if chase happened
	 */
	public static boolean chaseMicro(RobotInfo enemy) throws GameActionException {
		boolean isEnemyMissile = enemy.type == RobotType.MISSILE;
		boolean isEnemyLauncher = enemy.type == RobotType.LAUNCHER;
		int enemyAttackRadius = enemy.type.attackRadiusSquared;
		// Don't chase enemy if they are launchers or missiles
		if (isEnemyMissile || isEnemyLauncher) {
			return false;
		}
		// Don't chase enemy if their attack radius is larger than mine
		if (typ.attackRadiusSquared < enemyAttackRadius) {
			return false;
		}
		Direction dirToEnemy = myLoc.directionTo(enemy.location);
		Direction[] dirs = {dirToEnemy.rotateLeft(), dirToEnemy.rotateRight(), dirToEnemy};
		for (int i = dirs.length; --i >= 0;) {
			// If we can move in the direction and we won't risk ourselves getting shot by the enemy.
			if (rc.canMove(dirs[i]) && myLoc.add(dirs[i]).distanceSquaredTo(enemy.location) > enemy.type.attackRadiusSquared) {
				rc.move(dirs[i]);
				return true;
			}
			
		}
		return false;
	}
	public static void teamFightMicro() {
		
	}
	

    /**
     * Figures out whether we are winning a 1v1 fight. This breaks down into
     * a few cases.
     *
     * 1. If the two robots are the same type, whoever can kill the enemy first
     * is the winner.
     *
     * 2. If the two robots are not the same type, but have the same range, we
     * take into account a different number of factors that come from
     * MD, CD, LD, and AD.
     *
     * 3. If you have a smaller range, but the enemy is within your attack radius,
     * same as 2.
     *
     * 4. If you are being outranged, we constitute this as an automatic defeat.
     *
     * 5. You outrange the enemy. You should be kiting if possible.
     *
     * @param enemy
     * @return
     */
    public static boolean isWinning1v1(RobotInfo enemy) {
        // If we are the same type, calculate the rate of death for each of
        // the units
        if (typ == enemy.type) {
            double myWeaponDelay = rc.getWeaponDelay();
            double enemyWeaponDelay = enemy.weaponDelay;
            double mySupply = rc.getSupplyLevel();
            double enemySupply = enemy.supplyLevel;
            double myHealth = rc.getHealth();
            double enemyHealth = enemy.health;
            int numOfHitsUntilEnemyDeath = (int) (enemyHealth / typ.attackPower);
            int numOfHitsUntilMyDeath = (int) (myHealth / typ.attackPower);
            // Does not yet account for supply. Assuming equal supply.
            // In this case, no matter what we will lose because
            // at most we can get one shot off and then it will be even, but
            // the enemy definitely goes first.
            if (numOfHitsUntilMyDeath < numOfHitsUntilEnemyDeath) {
                return false;
            }
            if (numOfHitsUntilMyDeath > numOfHitsUntilEnemyDeath) {
                return true;
            }
            // Here the number of hits is the same, so whoever has the lower action delay wins,
            // unless we lose in execution order and the
            if (myWeaponDelay - enemyWeaponDelay <= -1) {
                return true;
            }
            if (enemyWeaponDelay - myWeaponDelay <= -1) {
                return true;
            }
            return false;
        }
        if (typ.attackRadiusSquared < enemy.type.attackRadiusSquared) {
            if (myLoc.distanceSquaredTo(enemy.location) <= typ.attackRadiusSquared) {
                return true;
            }
            return true;
        }
        return false;
    }	
}
