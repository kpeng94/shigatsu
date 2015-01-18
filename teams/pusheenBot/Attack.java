package pusheenBot;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Attack {

	public static void tryAttackClosestButKillIfPossible() throws GameActionException {
		RobotInfo[] enemies = Handler.rc.senseNearbyRobots(Handler.typ.attackRadiusSquared, Handler.otherTeam);
		if (enemies.length == 0) return;
		
		int minDist = 999999;
		int minHP = 999999;
		MapLocation minLoc = null;
		
		int minMissileDist = 999999;
		int minMissileHP = 999999;
		MapLocation minMissileLoc = null;
		for (int i = enemies.length; --i >= 0;) {
			RobotInfo enemy = enemies[i];
			if (enemies[i].type == RobotType.MISSILE) { // Focus lower hp missiles, followed by closer missiles
				if (enemy.health == 1) { // insta kill low hp missile
					Handler.rc.attackLocation(enemy.location);
					return;
				}
				int dist = enemy.location.distanceSquaredTo(Handler.myLoc);
				if (enemy.health < minMissileHP || (enemy.health == minMissileHP && dist < minMissileDist)) {
					minMissileDist = dist;
					minMissileHP = (int) enemy.health;
					minMissileLoc = enemy.location;
				}
			} else {
				if (enemy.health < Handler.typ.attackPower) { // insta kill low hp enemy
					Handler.rc.attackLocation(enemy.location);
					return;
				}
				int dist = enemy.location.distanceSquaredTo(Handler.myLoc);
				if (dist < minDist || (dist == minDist && enemy.health < minHP)) {
					minDist = dist;
					minHP = (int) enemy.health;
					minLoc = enemy.location;
				}
			}
		}
		if (minDist == 999999) { // only missiles 
			Handler.rc.attackLocation(minMissileLoc);
		} else {
			Handler.rc.attackLocation(minLoc);
		}
	}
	
	public static void tryAttackPrioritizeTowers() throws GameActionException {
		if (Handler.rc.canAttackLocation(Handler.enemyHQ)) {
			Handler.rc.attackLocation(Handler.enemyHQ);
			return;
		}
		for (int i = Handler.enemyTowers.length; --i >= 0;) {
			if (Handler.rc.canAttackLocation(Handler.enemyTowers[i])) {
				Handler.rc.attackLocation(Handler.enemyTowers[i]);
				return;
			}
		}
		
		tryAttackClosestButKillIfPossible();
	}
	
}
