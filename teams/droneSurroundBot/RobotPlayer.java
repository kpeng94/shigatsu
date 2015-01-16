package droneSurroundBot;

import battlecode.common.*;

public class RobotPlayer {
	public static void run(RobotController rc) {
		RobotType typ = rc.getType();
		if (typ == RobotType.MISSILE) { // Missile needs to be the cheapest
			UMissileHandler.loop(rc);
		} else if (typ == RobotType.TOWER) {
			STowerHandler.loop(rc);
		} else if (typ == RobotType.HQ) {
			SHQHandler.loop(rc);
		} else if (typ == RobotType.BEAVER) {
			UBeaverHandler.loop(rc);
		} else if (typ == RobotType.MINER) {
			UMinerHandler.loop(rc);
		} else if (typ == RobotType.DRONE) {
			UDroneHandler.loop(rc);
		} else if (typ == RobotType.LAUNCHER) {
			ULauncherHandler.loop(rc);
		} else if (typ == RobotType.MINERFACTORY) {
			SMinerFactoryHandler.loop(rc);
		} else if (typ == RobotType.HELIPAD) {
			SHeliHandler.loop(rc);
		} else if (typ == RobotType.AEROSPACELAB) {
			SAeroHandler.loop(rc);
		} else if (typ == RobotType.SUPPLYDEPOT) {
			SSupplyHandler.loop(rc);
		} else if (typ == RobotType.TANK) {
			UTankHandler.loop(rc);
		} else if (typ == RobotType.COMMANDER) {
			UCommanderHandler.loop(rc);
		} else if (typ == RobotType.BASHER) {
			UBasherHandler.loop(rc);
		} else if (typ == RobotType.SOLDIER) {
			USoldierHandler.loop(rc);
		} else if (typ == RobotType.COMPUTER) {
			UComputerHandler.loop(rc);
		} else if (typ == RobotType.TANKFACTORY) {
			STankFactoryHandler.loop(rc);
		} else if (typ == RobotType.BARRACKS) {
			SBarrackHandler.loop(rc);
		} else if (typ == RobotType.TRAININGFIELD) {
			STrainingHandler.loop(rc);
		} else if (typ == RobotType.TECHNOLOGYINSTITUTE) {
			STechHandler.loop(rc);
		} else if (typ == RobotType.HANDWASHSTATION) {
			SHandwashHandler.loop(rc);
		}
	}
}
