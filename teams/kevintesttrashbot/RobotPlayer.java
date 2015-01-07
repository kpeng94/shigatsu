package kevintesttrashbot;

import battlecode.common.*;

public class RobotPlayer {

	public static void run(RobotController rc) throws Exception {
		switch(rc.getType()) {
			case HQ:
				HQRobot.run(rc);
				break;
			case BEAVER:
				BeaverRobot.run(rc);
				break;
			case MINER:
				MinerRobot.run(rc);
				break;
			case SOLDIER:
				SoldierRobot.run(rc);
				break;
			case TANK:
				TankRobot.run(rc);
				break;
			case TOWER:
				TowerRobot.run(rc);
				break;
			case TANKFACTORY:
				TankFactoryRobot.run(rc);
				break;
			case BARRACKS:
				BarracksRobot.run(rc);
				break;
				
			default:
				throw new Exception("Bad robot.");				
		}
	}
}
