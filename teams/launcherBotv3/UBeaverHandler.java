package launcherBotv3;

import battlecode.common.*;

public class UBeaverHandler extends UnitHandler {
    private static RobotType[] buildTyps = {RobotType.MINERFACTORY, RobotType.HELIPAD, RobotType.AEROSPACELAB, RobotType.SUPPLYDEPOT, RobotType.TECHNOLOGYINSTITUTE, RobotType.TRAININGFIELD};
    private static BeaverState state = BeaverState.NEW;
    private static BeaverState nextBeaverState;
    private static int[] buildChans;
    public static int curBuildingChan;

    private static int numberOfMiningFactories;
    private static int numberOfHelipads;
    private static int numberOfAerospaceLabs;
    private static int numberOfTankFactories;
    private static int numberOfBarracks;
    private static int numberOfTechInstitutes;
    private static int numberOfTrainingFields;
    private static int numberOfSupplyDepots;

    private enum BeaverState {
        /** Constructing Mining Factories at the beginning **/
        NEW, BUILDING_BARRACKS, BUILDING_MININGFACTORIES, 
        BUILDING_TANKFACTORIES, BUILDING_HELIPADS, 
        BUILDING_TECHINST, BUILDING_TRAININGFIELD, BUILDING_AEROSPACELABORATORIES, BUILDING_SUPPLYDEPOTS, COMPUTING, MINING, FIGHTING
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
        Spawner.HQxMod = myHQ.x % 2;
        Spawner.HQyMod = myHQ.y % 2;
//        NavTangentBug.setDest(enemyHQ);
        buildChans = new int[]{Comm.getMinerfactId(), Comm.getHeliId(), Comm.getAeroId(), Comm.getSupplyId()};
    }

    protected static void execute() throws GameActionException {
        executeUnit();
        Count.incrementBuffer(Comm.getBeaverId());
        Debug.clockString(0, "My state is: " + state);
        if (rc.isBuildingSomething()) {
            Count.incrementBuffer(curBuildingChan);
        } else {
            curBuildingChan = 0;
        }
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
            case BUILDING_TECHINST:
            	buildingTechInstCode();
            	break;
            case BUILDING_TRAININGFIELD:
            	buildingTrainingFieldCode();
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

    private static void buildingTrainingFieldCode() throws GameActionException {
		numberOfTrainingFields = Count.getCount(Comm.getTrainingId());		
		
	}

	private static void buildingTechInstCode() throws GameActionException {
		numberOfTechInstitutes = Count.getCount(Comm.getTechId());		
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

    private static void buildingAerospaceLaboratoriesCode()
            throws GameActionException {
        numberOfAerospaceLabs = Count.getCount(Comm.getAeroId());
        if (numberOfAerospaceLabs >= Count.getLimit(Comm.getAeroId())) {
        	nextBeaverState = BeaverState.BUILDING_SUPPLYDEPOTS;
        	buildingSupplyDepotsCode();
        } else {
            if (rc.isCoreReady()) {
                if (!tryBuild(RobotType.AEROSPACELAB)) {
                    if (myLoc.distanceSquaredTo(myHQ) > 15) {
                        NavSimple.walkTowards(myLoc.directionTo(myHQ));
                    } else {
                        NavSimple.walkRandom();
                    }
                } 
            }        	
        }

    }

    private static void buildingHelipadsCode() throws GameActionException {
        numberOfHelipads = Count.getCount(Comm.getHeliId());
        if (numberOfHelipads >= Count.getLimit(Comm.getHeliId())) {
        	nextBeaverState = BeaverState.BUILDING_AEROSPACELABORATORIES;
        	buildingAerospaceLaboratoriesCode();
        } else {
            if (rc.isCoreReady()) {
                if (!tryBuild(RobotType.HELIPAD)) {
                    if (myLoc.distanceSquaredTo(myHQ) > 15) {
                        NavSimple.walkTowards(myLoc.directionTo(myHQ));
                    } else {
                        NavSimple.walkRandom();
                    }
                } 
            }        	
        }        
    }

    private static void buildingSupplyDepotsCode() throws GameActionException {
    	Debug.clockString(2, "" + Count.getLimit(Comm.getAeroId()));
        numberOfSupplyDepots = Count.getCount(Comm.getSupplyId());
        if (numberOfSupplyDepots < Count.getLimit(Comm.getSupplyId())) {
	        if (rc.isCoreReady()) {
	            if (!tryBuild(RobotType.SUPPLYDEPOT)) {
	                if (myLoc.distanceSquaredTo(myHQ) > 15) {
	                    NavSimple.walkTowards(myLoc.directionTo(myHQ));
	                } else {
	                    NavSimple.walkRandom();
	                }
	            } 
	        }
        } else if (numberOfAerospaceLabs < Count.getLimit(Comm.getAeroId())) {
        	nextBeaverState = BeaverState.BUILDING_AEROSPACELABORATORIES;
        	buildingAerospaceLaboratoriesCode();       	
        }
    }

    private static void buildingBarracksCode() throws GameActionException {
        numberOfBarracks = Count.getCount(Comm.getBarrackId());
        numberOfTankFactories = Count.getCount(Comm.getTankfactId());
        if (numberOfBarracks >= Count.getLimit(Comm.getBarrackId())
                && numberOfTankFactories <= Count.getLimit(Comm.getTankfactId())) {
            nextBeaverState = BeaverState.BUILDING_TANKFACTORIES;
        } else if (numberOfBarracks < Count.getLimit(Comm.getBarrackId())) {
            if (rc.isCoreReady()) {
                if (!tryBuild(RobotType.BARRACKS)) {
                    if (myLoc.distanceSquaredTo(myHQ) > 15) {
                        NavSimple.walkTowards(myLoc.directionTo(myHQ));
                    } else {
                        NavSimple.walkRandom();
                    }
                } else {
                    nextBeaverState = BeaverState.BUILDING_TANKFACTORIES;
                }
            }
        } else {
            if (!tryBuild(RobotType.BARRACKS)) {
                if (myLoc.distanceSquaredTo(myHQ) > 15) {
                    NavSimple.walkTowards(myLoc.directionTo(myHQ));
                } else {
                    NavSimple.walkRandom();
                }
            }
        }
    }

    private static void buildingTankFactoriesCode() throws GameActionException {
        numberOfBarracks = Count.getCount(Comm.getBarrackId());
        numberOfTankFactories = Count.getCount(Comm.getTankfactId());
        if (numberOfBarracks < Count.getLimit(Comm.getBarrackId())) {
            nextBeaverState = BeaverState.BUILDING_BARRACKS;
        } else if (numberOfTankFactories < Count.getLimit(Comm.getTankfactId())) {
            if (rc.isCoreReady()) {
                if (!tryBuild(RobotType.TANKFACTORY)) {
                    if (myLoc.distanceSquaredTo(myHQ) > 15) {
                        NavSimple.walkTowards(myLoc.directionTo(myHQ));
                    } else {
                        NavSimple.walkRandom();
                    }
                }
            }
        }
    }

    private static void buildingMiningFactoriesCode()
            throws GameActionException {
        dirFromHQ = myHQ.directionTo(myLoc);
        numberOfMiningFactories = Count.getCount(Comm.getMinerfactId());
        if (numberOfMiningFactories >= Count.getLimit(Comm.getMinerfactId())) {
            nextBeaverState = BeaverState.BUILDING_HELIPADS;
            buildingHelipadsCode();
        } else {
            if (rc.isCoreReady()) {
                if (!tryBuild(RobotType.MINERFACTORY)) {
                    if (myLoc.distanceSquaredTo(myHQ) > 15) {
                        NavSimple.walkTowards(myLoc.directionTo(myHQ));
                    } else {
                        NavSimple.walkRandom();
                    }
                }
            }
        }

    }

    private static void newCode() throws GameActionException {
        numberOfMiningFactories = Count.getCount(Comm.getMinerfactId());
//        numberOfBarracks = Count.getCount(Comm.getBarrackId());
//        numberOfTankFactories = Count.getCount(Comm.getTankfactId());
        numberOfHelipads = Count.getCount(Comm.getHeliId());
        numberOfAerospaceLabs = Count.getCount(Comm.getAeroId());
        numberOfSupplyDepots = Count.getCount(Comm.getSupplyId());

        if (numberOfMiningFactories < Count.getLimit(Comm.getMinerfactId())) {
            nextBeaverState = BeaverState.BUILDING_MININGFACTORIES;
            buildingMiningFactoriesCode();
        } else if (numberOfHelipads < Count.getLimit(Comm.getHeliId())) {
        	nextBeaverState = BeaverState.BUILDING_HELIPADS;
        	buildingHelipadsCode();
        } else if (numberOfAerospaceLabs < Count.getLimit(Comm.getAeroId())) {
            nextBeaverState = BeaverState.BUILDING_AEROSPACELABORATORIES;
            buildingAerospaceLaboratoriesCode();
        } else {
            nextBeaverState = BeaverState.BUILDING_SUPPLYDEPOTS;
            buildingSupplyDepotsCode();
        }
    }

    protected static boolean tryBuild(RobotType type) throws GameActionException {
        Direction dir = Spawner.getBuildDirection(false);
        if (dir != Direction.NONE) {
            for (int i = 0; i < buildTyps.length; i++) {
                if (type == buildTyps[i] && rc.canBuild(dir, buildTyps[i])) {
                    Spawner.build(dir, buildTyps[i], buildChans[i]);
                    curBuildingChan = buildChans[i];
                    return true;
                }
            }
        }
        return false;
    }

}
