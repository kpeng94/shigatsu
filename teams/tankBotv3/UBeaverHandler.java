package tankBotv3;

import battlecode.common.*;

public class UBeaverHandler extends UnitHandler {
	private static BeaverState state = BeaverState.NEW;
	private static BeaverState nextBeaverState;
	
	private static int numberOfMiningFactories;
	private static int numberOfHelipads;
	private static int numberOfAerospaceLabs;
	private static int numberOfTankFactories;
	private static int numberOfBarracks;
	private static int numberOfTechInstitutes;
	private static int numberOfTrainingFields;
	private static int numberOfSupplyDepots;
	
	private static MapLocation nextTargetLoc;
	private static boolean atTargetDest = true;
	private static boolean targetDestSet = false;
	private static double oreAmount;
	
	private static int directionStep = 2;
	// private static Direction buildDir = Direction.NORTH;
	// Build dirs assumes a north east direction to the enemy tower.
	private static Direction[] buildDirs = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.EAST, Direction.NORTH, Direction.WEST};
	private static int directionIndex = 0;
	
	private enum BeaverState {
		/** Constructing Mining Factories at the beginning **/
		NEW,
		BUILDING_BARRACKS,
		BUILDING_MININGFACTORIES,
		BUILDING_TANKFACTORIES,
		BUILDING_HELIPADS,
		BUILDING_AEROSPACELABORATORIES,
		BUILDING_SUPPLYDEPOTS,
		COMPUTING,
		MINING,
		FIGHTING
	}
	
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
		NavTangentBug.setDest(enemyHQ);
	}

	protected static void execute() throws GameActionException {
		
		executeUnit();
		readBroadcasts();
		oreAmount = rc.getTeamOre();
		switch (state) {
			case NEW:
				newCode();
				break;
			case BUILDING_BARRACKS:
				buildingBarracksCode();
				break;
			case BUILDING_MININGFACTORIES:
				buildingMiningFactoriesCode();
				break;
			case BUILDING_HELIPADS:
				buildingHelipadsCode();
				break;
			case BUILDING_TANKFACTORIES:
				buildingTankFactoriesCode();
				break;
			case BUILDING_AEROSPACELABORATORIES:
				buildingAerospaceLaboratoriesCode();
				break;
			case BUILDING_SUPPLYDEPOTS:
				buildingSupplyDepotsCode();
				break;
			case COMPUTING:
				computingCode();
				break;
			case MINING:
				miningCode();
				break;
			case FIGHTING:
				fightingCode();
				break;
		}
		
		/**
		 * State transition and clear for next iteration of loop.
		 */
		if (nextBeaverState != null) {
			state = nextBeaverState;
			nextBeaverState = null;
		}
		
		Supply.spreadSupplies(Supply.DEFAULT_THRESHOLD);
	}

	private static void fightingCode() {
		// TODO Auto-generated method stub
		
	}

	private static void miningCode() {
		// TODO Auto-generated method stub
		
	}

	private static void computingCode() {
		// TODO Auto-generated method stub
		
	}

	private static void buildingAerospaceLaboratoriesCode() throws GameActionException {
//		if (numberOfAerospaceLabs >= Constants.NUM_OF_AEROSPACELABS) {
//			nextBeaverState = BeaverState.BUILDING_HELIPADS;
//		}
//		if (!targetDestSet) {
//			directionStep = (directionIndex % 3 == 0) ? 2 : (directionIndex / 3 + 1) * 2;
//			if (nextTargetLoc == null) {
//				getLastBuildingLoc();
//			}
//			nextTargetLoc = nextTargetLoc.add(buildDirs[(directionIndex + directionToInt(myHQToEnemyHQ) / 2 * 2) % buildDirs.length], directionStep);
//			directionIndex++;
//			
//			NavTangentBug.setDest(nextTargetLoc);
//			targetDestSet = true;
//			atTargetDest = false;			
//		}
//		if (!atTargetDest && targetDestSet) {
//			NavTangentBug.calculate(2500);
//			if (rc.isCoreReady()) {
//				Direction nextMove = NavTangentBug.getNextMove();
//				if (nextMove != Direction.NONE) {
//					NavSimple.walkTowardsDirected(nextMove);
//				}
//			}
//			if (nextTargetLoc.distanceSquaredTo(myLoc) <= 2) {
//				atTargetDest = true;
//			}
//		}
//		if (atTargetDest) {
//			if (rc.isCoreReady()) {
//				tryBuild(myLoc.directionTo(nextTargetLoc), RobotType.AEROSPACELAB, oreAmount);
//				broadcastLastBuildingLoc(nextTargetLoc);
//				targetDestSet = false;
//				atTargetDest = false;				
//			}
//		}

	}

	private static void buildingHelipadsCode() throws GameActionException {
		// TODO Auto-generated method stub
//		if (numberOfHelipads >= Constants.NUM_OF_INITIAL_HELIPADS && numberOfAerospaceLabs < Constants.NUM_OF_AEROSPACELABS) {
//			nextBeaverState = BeaverState.BUILDING_AEROSPACELABORATORIES;
//		} else if (numberOfHelipads >= Constants.NUM_OF_HELIPADS) {
//			nextBeaverState = BeaverState.MINING;
//		} else {
//			if (!targetDestSet) {
//				nextTargetLoc = myHQ.add(myHQToEnemyHQ, 2);
//				NavTangentBug.setDest(nextTargetLoc);
//				targetDestSet = true;
//				atTargetDest = false;
//			}
//			if (!atTargetDest && targetDestSet) {
//				NavTangentBug.calculate(Clock.getBytecodesLeft() - 100);
//				if (rc.isCoreReady()) {
//					Direction nextMove = NavTangentBug.getNextMove();
//					if (nextMove != Direction.NONE) {
//						NavSimple.walkTowardsDirected(nextMove);
//					}
//				}
//				if (nextTargetLoc.distanceSquaredTo(myLoc) <= 2) {
//					atTargetDest = true;
//				}
//			}
//			if (atTargetDest) {
//				if (rc.isCoreReady()) {
//					tryBuild(myLoc.directionTo(nextTargetLoc), RobotType.HELIPAD, oreAmount);
//					broadcastLastBuildingLoc(nextTargetLoc);
//					nextBeaverState = BeaverState.BUILDING_AEROSPACELABORATORIES;
//					targetDestSet = false;
//					atTargetDest = false;
//				}
//			}
//			rc.yield();
//		}	
	}

	private static void buildingSupplyDepotsCode() {
		if (numberOfSupplyDepots <= Constants.NUM_OF_SUPPLYDEPOTS) {
			
		}
	}

	private static void buildingBarracksCode() throws GameActionException {
		if (numberOfBarracks >= Constants.NUM_OF_INITIAL_BARRACKS && numberOfTankFactories <= Constants.NUM_OF_TANKFACTORIES) {
			nextBeaverState = BeaverState.BUILDING_TANKFACTORIES;
		} else if (numberOfBarracks < Constants.NUM_OF_INITIAL_BARRACKS) {
			if (!targetDestSet) {
				rc.setIndicatorString(0, "Built barracks");
				nextTargetLoc = myHQ.add(myHQToEnemyHQ, 2);
				NavTangentBug.setDest(nextTargetLoc);
				targetDestSet = true;
				atTargetDest = false;
			}
			if (!atTargetDest && targetDestSet) {
				NavTangentBug.calculate(Clock.getBytecodesLeft() - 100);
				if (rc.isCoreReady()) {
					Direction nextMove = NavTangentBug.getNextMove();
					if (nextMove != Direction.NONE) {
						NavSimple.walkTowardsDirected(nextMove);
					}
				}
				if (nextTargetLoc.distanceSquaredTo(myLoc) <= 2) {
					atTargetDest = true;
				}
			}
			if (atTargetDest) {
				if (rc.isCoreReady()) {
					tryBuild(myLoc.directionTo(nextTargetLoc), RobotType.BARRACKS, oreAmount);
					broadcastLastBuildingLoc(nextTargetLoc);
					nextBeaverState = BeaverState.BUILDING_TANKFACTORIES;
					targetDestSet = false;
					atTargetDest = false;
				}
			}
		} else {
			if (!targetDestSet) {
				directionStep = (directionIndex % 3 == 0) ? 2 : (directionIndex / 3 + 1) * 2;
				if (nextTargetLoc == null) {
					getLastBuildingLoc();
				}
				nextTargetLoc = nextTargetLoc.add(buildDirs[(directionIndex + directionToInt(myHQToEnemyHQ) / 2 * 2) % buildDirs.length], directionStep);
				directionIndex++;
				
				NavTangentBug.setDest(nextTargetLoc);
				targetDestSet = true;
				atTargetDest = false;		
			}
			if (!atTargetDest && targetDestSet) {
				NavTangentBug.calculate(Clock.getBytecodesLeft() - 100);
				if (rc.isCoreReady()) {
					Direction nextMove = NavTangentBug.getNextMove();
					if (nextMove != Direction.NONE) {
						NavSimple.walkTowardsDirected(nextMove);
					}
				}
				if (nextTargetLoc.distanceSquaredTo(myLoc) <= 2) {
					atTargetDest = true;
				}
			}
			if (atTargetDest) {
				if (rc.isCoreReady()) {
					tryBuild(myLoc.directionTo(nextTargetLoc), RobotType.BARRACKS, oreAmount);
					broadcastLastBuildingLoc(nextTargetLoc);
					targetDestSet = false;
					atTargetDest = false;
				}
			}			
		}
	}
	
	private static void buildingTankFactoriesCode() throws GameActionException {
		if (numberOfBarracks < Constants.NUM_OF_INITIAL_BARRACKS) {
			nextBeaverState = BeaverState.BUILDING_BARRACKS;
		} else if (numberOfTankFactories < Constants.NUM_OF_TANKFACTORIES) {
			if (!targetDestSet) {
				directionStep = (directionIndex % 3 == 0) ? 2 : (directionIndex / 3 + 1) * 2;
				if (nextTargetLoc == null) {
					getLastBuildingLoc();
				}
				nextTargetLoc = nextTargetLoc.add(buildDirs[(directionIndex + directionToInt(myHQToEnemyHQ) / 2 * 2) % buildDirs.length], directionStep);
				directionIndex++;
				
				NavTangentBug.setDest(nextTargetLoc);
				targetDestSet = true;
				atTargetDest = false;		
			}
			if (!atTargetDest && targetDestSet) {
				NavTangentBug.calculate(Clock.getBytecodesLeft() - 100);
				if (rc.isCoreReady()) {
					Direction nextMove = NavTangentBug.getNextMove();
					if (nextMove != Direction.NONE) {
						NavSimple.walkTowardsDirected(nextMove);
					}
				}
				if (nextTargetLoc.distanceSquaredTo(myLoc) <= 2) {
					atTargetDest = true;
				}
			}
			if (atTargetDest) {
				if (rc.isCoreReady()) {
					tryBuild(myLoc.directionTo(nextTargetLoc), RobotType.TANKFACTORY, oreAmount);
					broadcastLastBuildingLoc(nextTargetLoc);
					targetDestSet = false;
					atTargetDest = false;
				}
			}				
		}
		
	}
	
	private static void buildingMiningFactoriesCode() throws GameActionException {
		dirFromHQ = myHQ.directionTo(myLoc);
		if (numberOfMiningFactories >= 2) {
			nextBeaverState = BeaverState.BUILDING_BARRACKS;
		} else {
			if (numberOfMiningFactories == 0) {
				tryBuild(dirFromHQ.rotateRight(), RobotType.MINERFACTORY, oreAmount);		
			} else if (numberOfMiningFactories == 1) {
				tryBuild(dirFromHQ.rotateLeft(), RobotType.MINERFACTORY, oreAmount);		
			}
		}
		
	}

	private static void newCode() throws GameActionException {
		if (numberOfMiningFactories < Constants.NUM_OF_MININGFACTORIES) {
			nextBeaverState = BeaverState.BUILDING_MININGFACTORIES;
			buildingMiningFactoriesCode();
		} else if (numberOfBarracks < Constants.NUM_OF_INITIAL_BARRACKS) {
			nextBeaverState = BeaverState.BUILDING_BARRACKS;
		}
			else if (numberOfTankFactories < Constants.NUM_OF_TANKFACTORIES) {
			nextBeaverState = BeaverState.BUILDING_TANKFACTORIES;
			buildingTankFactoriesCode();
		} 
//		if (numberOfMiningFactories < 2) {
//			nextBeaverState = BeaverState.BUILDING_MININGFACTORIES;
//			buildingMiningFactoriesCode();
//		} else if (numberOfHelipads >= 1 && numberOfAerospaceLabs <= Constants.NUM_OF_AEROSPACELABS) {
//			nextBeaverState = BeaverState.BUILDING_AEROSPACELABORATORIES;
//		} else if (numberOfHelipads < 1) {
//			nextBeaverState = BeaverState.BUILDING_HELIPADS;
//		}
	}
	
	private static void readBroadcasts() throws GameActionException {
		numberOfMiningFactories = Comm.readBlock(Comm.getMinerfactId(), 1);
		numberOfHelipads = Comm.readBlock(Comm.getHeliId(), 1);
		numberOfAerospaceLabs = Comm.readBlock(Comm.getAeroId(), 1);
		numberOfTankFactories = Comm.readBlock(Comm.getTankId(), 1);
		numberOfBarracks = Comm.readBlock(Comm.getBarrackId(), 1);
		numberOfTechInstitutes = Comm.readBlock(Comm.getTechId(), 1);
		numberOfTrainingFields = Comm.readBlock(Comm.getTrainingId(), 1);
		rc.setIndicatorString(0, "" + numberOfBarracks);
	}
	
	private static void getLastBuildingLoc() throws GameActionException {
		int encodedMapCode = Comm.readBlock(Comm.getBeaverId(), 4);
		int x = (encodedMapCode / 256) % 256;
		if (x > 128) {
			x = x - 256;
		}
		int y = encodedMapCode % 256;
		if (y > 128) {
			y = y - 256;
		}
		nextTargetLoc = new MapLocation(myHQ.x + x, myHQ.y + y);
	}
	private static void broadcastLastBuildingLoc(MapLocation ml) throws GameActionException {
		int mlx = (ml.x - myHQ.x + 256) % 256;
		int mly = (ml.y - myHQ.y + 256) % 256;
		Comm.writeBlock(Comm.getBeaverId(), 4, mlx * 256 + mly);
	}
	
	static void tryBuild(Direction d, RobotType type, double oreAmount) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = {0,1,-1,2,-2,3,-3,4};
		int dirint = directionToInt(d);
		while (offsetIndex < 8 && !rc.canMove(directions[(dirint+offsets[offsetIndex]+8)%8])) {
			offsetIndex++;
		}
		if (offsetIndex < 8 && oreAmount >= type.oreCost) {
			rc.build(directions[(dirint+offsets[offsetIndex]+8)%8], type);
			targetDestSet = false;
		}
	}	
}
