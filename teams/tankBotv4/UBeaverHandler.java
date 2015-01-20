package tankBotv4;

import battlecode.common.*;

public class UBeaverHandler extends UnitHandler {
    private static RobotType[] buildTyps = {RobotType.MINERFACTORY, RobotType.BARRACKS, RobotType.TANKFACTORY, RobotType.SUPPLYDEPOT};
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
        NEW, BUILDING_BARRACKS, BUILDING_MININGFACTORIES, BUILDING_TANKFACTORIES, BUILDING_HELIPADS, BUILDING_AEROSPACELABORATORIES, BUILDING_SUPPLYDEPOTS, COMPUTING, MINING, FIGHTING
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
        buildChans = new int[]{Comm.getMinerfactId(), Comm.getBarrackId(), Comm.getTankfactId(), Comm.getSupplyId()};
    }

    protected static void execute() throws GameActionException {
        executeUnit();
        Count.incrementBuffer(Comm.getBeaverId());
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

    private static void buildingAerospaceLaboratoriesCode()
            throws GameActionException {
        // if (numberOfAerospaceLabs >= Constants.NUM_OF_AEROSPACELABS) {
        // nextBeaverState = BeaverState.BUILDING_HELIPADS;
        // }
        // if (!targetDestSet) {
        // directionStep = (directionIndex % 3 == 0) ? 2 : (directionIndex / 3 +
        // 1) * 2;
        // if (nextTargetLoc == null) {
        // getLastBuildingLoc();
        // }
        // nextTargetLoc = nextTargetLoc.add(buildDirs[(directionIndex +
        // directionToInt(myHQToEnemyHQ) / 2 * 2) % buildDirs.length],
        // directionStep);
        // directionIndex++;
        //
        // NavTangentBug.setDest(nextTargetLoc);
        // targetDestSet = true;
        // atTargetDest = false;
        // }
        // if (!atTargetDest && targetDestSet) {
        // NavTangentBug.calculate(2500);
        // if (rc.isCoreReady()) {
        // Direction nextMove = NavTangentBug.getNextMove();
        // if (nextMove != Direction.NONE) {
        // NavSimple.walkTowardsDirected(nextMove);
        // }
        // }
        // if (nextTargetLoc.distanceSquaredTo(myLoc) <= 2) {
        // atTargetDest = true;
        // }
        // }
        // if (atTargetDest) {
        // if (rc.isCoreReady()) {
        // tryBuild(myLoc.directionTo(nextTargetLoc), RobotType.AEROSPACELAB,
        // oreAmount);
        // broadcastLastBuildingLoc(nextTargetLoc);
        // targetDestSet = false;
        // atTargetDest = false;
        // }
        // }

    }

    private static void buildingHelipadsCode() throws GameActionException {
        // TODO Auto-generated method stub
        // if (numberOfHelipads >= Constants.NUM_OF_INITIAL_HELIPADS &&
        // numberOfAerospaceLabs < Constants.NUM_OF_AEROSPACELABS) {
        // nextBeaverState = BeaverState.BUILDING_AEROSPACELABORATORIES;
        // } else if (numberOfHelipads >= Constants.NUM_OF_HELIPADS) {
        // nextBeaverState = BeaverState.MINING;
        // } else {
        // if (!targetDestSet) {
        // nextTargetLoc = myHQ.add(myHQToEnemyHQ, 2);
        // NavTangentBug.setDest(nextTargetLoc);
        // targetDestSet = true;
        // atTargetDest = false;
        // }
        // if (!atTargetDest && targetDestSet) {
        // NavTangentBug.calculate(Clock.getBytecodesLeft() - 100);
        // if (rc.isCoreReady()) {
        // Direction nextMove = NavTangentBug.getNextMove();
        // if (nextMove != Direction.NONE) {
        // NavSimple.walkTowardsDirected(nextMove);
        // }
        // }
        // if (nextTargetLoc.distanceSquaredTo(myLoc) <= 2) {
        // atTargetDest = true;
        // }
        // }
        // if (atTargetDest) {
        // if (rc.isCoreReady()) {
        // tryBuild(myLoc.directionTo(nextTargetLoc), RobotType.HELIPAD,
        // oreAmount);
        // broadcastLastBuildingLoc(nextTargetLoc);
        // nextBeaverState = BeaverState.BUILDING_AEROSPACELABORATORIES;
        // targetDestSet = false;
        // atTargetDest = false;
        // }
        // }
        // rc.yield();
        // }
    }

    private static void buildingSupplyDepotsCode() throws GameActionException {
        if (Count.getCount(Comm.getSupplyId()) <= Constants.NUM_OF_SUPPLYDEPOTS) {

        }
    }

    private static void buildingBarracksCode() throws GameActionException {
        numberOfBarracks = Count.getCount(Comm.getBarrackId());
        numberOfTankFactories = Count.getCount(Comm.getTankfactId());
        if (numberOfBarracks >= Constants.NUM_OF_INITIAL_BARRACKS
                && numberOfTankFactories <= Constants.NUM_OF_TANKFACTORIES) {
            nextBeaverState = BeaverState.BUILDING_TANKFACTORIES;
        } else if (numberOfBarracks < Constants.NUM_OF_INITIAL_BARRACKS) {
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
        if (numberOfBarracks < Constants.NUM_OF_INITIAL_BARRACKS) {
            nextBeaverState = BeaverState.BUILDING_BARRACKS;
        } else if (numberOfTankFactories < Constants.NUM_OF_TANKFACTORIES) {
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
        if (numberOfMiningFactories >= 2) {
            nextBeaverState = BeaverState.BUILDING_BARRACKS;
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
        if (numberOfMiningFactories < Constants.NUM_OF_MININGFACTORIES) {
            nextBeaverState = BeaverState.BUILDING_MININGFACTORIES;
            buildingMiningFactoriesCode();
        } else if (numberOfBarracks < Constants.NUM_OF_INITIAL_BARRACKS) {
            nextBeaverState = BeaverState.BUILDING_BARRACKS;
        } else if (numberOfTankFactories < Constants.NUM_OF_TANKFACTORIES) {
            nextBeaverState = BeaverState.BUILDING_TANKFACTORIES;
            buildingTankFactoriesCode();
        }
        // if (numberOfMiningFactories < 2) {
        // nextBeaverState = BeaverState.BUILDING_MININGFACTORIES;
        // buildingMiningFactoriesCode();
        // } else if (numberOfHelipads >= 1 && numberOfAerospaceLabs <=
        // Constants.NUM_OF_AEROSPACELABS) {
        // nextBeaverState = BeaverState.BUILDING_AEROSPACELABORATORIES;
        // } else if (numberOfHelipads < 1) {
        // nextBeaverState = BeaverState.BUILDING_HELIPADS;
        // }
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
