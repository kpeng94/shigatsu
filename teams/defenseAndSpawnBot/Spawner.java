package defenseAndSpawnBot;

import battlecode.common.*;

public class Spawner {

	public static void trySpawn(Direction dir, RobotType typ) throws GameActionException {
		Direction[] dirs = MapUtils.directionsTowardsRev(dir);
		for (int i = dirs.length; --i >= 0;) {
			if (Handler.rc.canSpawn(dirs[i], typ)) {
				Handler.rc.spawn(dirs[i], typ);
				return;
			}
		}
	}
	
}
