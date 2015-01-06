package genericPlayer;

import genericPlayer.structures.*;
import genericPlayer.units.*;
import battlecode.common.*;

public class RobotPlayer {
	public static Handler handler;

	public static void run(RobotController rc) {
		try {
			switch(rc.getType()) {
			case HQ:
				handler = new HQHandler(rc);
				break;
			case TOWER:
				handler = new TowerHandler(rc);
				break;
			case SUPPLYDEPOT:
				handler = new SupplyHandler(rc);
				break;
			case TECHNOLOGYINSTITUTE:
				handler = new TechHandler(rc);
				break;
			case BARRACKS:
				handler = new BarrackHandler(rc);
				break;
			case HELIPAD:
				handler = new HeliHandler(rc);
				break;
			case MINERFACTORY:
				handler = new MinerFactoryHandler(rc);
				break;
			case TANKFACTORY:
				handler = new TankFactoryHandler(rc);
				break;
			case AEROSPACELAB:
				handler = new AeroHandler(rc);
				break;
			case TRAININGFIELD:
				handler = new TrainingHandler(rc);
				break;
			case HANDWASHSTATION:
				handler = new HandwashHandler(rc);
				break;
			case BEAVER:
				handler = new BeaverHandler(rc);
				break;
			case MINER:
				handler = new MinerHandler(rc);
				break;
			case COMPUTER:
				handler = new ComputerHandler(rc);
				break;
			case SOLDIER:
				handler = new SoldierHandler(rc);
				break;
			case BASHER:
				handler = new BasherHandler(rc);
				break;
			case DRONE:
				handler = new DroneHandler(rc);
				break;
			case TANK:
				handler = new TankHandler(rc);
				break;
			case LAUNCHER:
				handler = new LauncherHandler(rc);
				break;
			case MISSILE:
				handler = new MissileHandler(rc);
				break;
			case COMMANDER:
				handler = new CommanderHandler(rc);
				break;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			System.out.println(rc.getType() + " Initialization Exception");
		}
		
		while (true) {
			try {
				handler.execute();
			}
			catch (Exception e) {
				e.printStackTrace();
				System.out.println(rc.getType() + " Exception");
			}
			rc.yield(); // Yields to save remaining bytecodes
		}
	}
}
