package navBot;

import battlecode.common.*;

public class RobotPlayer {
	public static void run(RobotController rc) {
		switch (rc.getType()) {
		case HQ:
			SHQHandler.loop(rc);
			break;
		case TOWER:
			STowerHandler.loop(rc);
			break;
		case SUPPLYDEPOT:
			SSupplyHandler.loop(rc);
			break;
		case TECHNOLOGYINSTITUTE:
			STechHandler.loop(rc);
			break;
		case BARRACKS:
			SBarrackHandler.loop(rc);
			break;
		case HELIPAD:
			SHeliHandler.loop(rc);
			break;
		case MINERFACTORY:
			SMinerFactoryHandler.loop(rc);
			break;
		case TANKFACTORY:
			STankFactoryHandler.loop(rc);
			break;
		case AEROSPACELAB:
			SAeroHandler.loop(rc);
			break;
		case TRAININGFIELD:
			STrainingHandler.loop(rc);
			break;
		case HANDWASHSTATION:
			SHandwashHandler.loop(rc);
			break;
		case BEAVER:
			UBeaverHandler.loop(rc);
			break;
		case MINER:
			UMinerHandler.loop(rc);
			break;
		case COMPUTER:
			UComputerHandler.loop(rc);
			break;
		case SOLDIER:
			USoldierHandler.loop(rc);
			break;
		case BASHER:
			UBasherHandler.loop(rc);
			break;
		case DRONE:
			UDroneHandler.loop(rc);
			break;
		case TANK:
			UTankHandler.loop(rc);
			break;
		case LAUNCHER:
			ULauncherHandler.loop(rc);
			break;
		case MISSILE:
			UMissileHandler.loop(rc);
			break;
		case COMMANDER:
			UCommanderHandler.loop(rc);
			break;
		}
	}
}
