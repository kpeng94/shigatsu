package genghisPostSeedRework;

import battlecode.common.*;

public class STowerHandler extends StructureHandler {

    public static MapLocation[] oreLocations;
    public static int oreLocationIndex;
    public static boolean oreIndexIncreasing;

    public static MapLocation[] nearbyTowers;

    public static int commMinerId;

    public static void loop(RobotController rcon) {
        try {
            init(rcon);
        } catch (Exception e) {
            e.printStackTrace();
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
        initStructure(rcon);

        // Get all the locations a tower can see around it for ore purposes
        oreLocations = MapLocation.getAllMapLocationsWithinRadiusSq(Handler.myLoc, 35);
        // Try to start with locations closer to your HQ
        if (Handler.myHQ.y < Handler.myLoc.y) {
            oreLocationIndex = 0;
            oreIndexIncreasing = true;
        } else {
            oreLocationIndex = oreLocations.length - 1;
            oreIndexIncreasing = false;
        }

        commMinerId = Comm.getMinerId();

        // Get the towers that are within your sight range (excluding yourself)
        nearbyTowers = Handler.rc.senseTowerLocations();
        pruneTowerList();
    }

    protected static void execute() throws GameActionException {
        executeStructure();
        Count.incrementBuffer(Comm.getTowerId());

        if (rc.isWeaponReady()) {
            Attack.tryAttackClosestButKillIfPossible(rc.senseNearbyRobots(typ.attackRadiusSquared, otherTeam));
        }

        // Update ore frontier if possible
        if (oreIndexIncreasing)
            broadcastOreLocationIncreasing();
        else
            broadcastOreLocationDecreasing();
    }

    private static final int ORE_UPDATE = 50;

    // If the frontier hasn't been updated for ORE_UPDATE rounds, or is 0, the
    // tower will look at its nearby
    // locations and broadcast if any of them have over ORE_THRESHOLD ore. The
    // order in which the tiles are
    // scanned is based of the HQ y-coordinate.
    protected static void broadcastOreLocationDecreasing() throws GameActionException {
        // All ore around the tower has been mined
        if (oreLocationIndex == -1)
            return;

        // Not enough bytecode to continue
        if (Clock.getBytecodesLeft() < 500)
            return;

        // No frontier
        int frontier = Comm.readBlock(commMinerId, Mining.FRONTIER_OFFSET);
        if (frontier != 0) {
            // Frontier has been updated recently
            int roundNum = Comm.readBlock(commMinerId, Mining.FRONTIER_RND_NUM);
            
            if (Mining.isFrontierTower(frontier) && roundNum != Clock.getRoundNum()) {
                return;
            } else if(!Mining.isFrontierTower(frontier)){
                if (roundNum > Clock.getRoundNum() - ORE_UPDATE) {
                    return;
                }
            }
        }

        while (oreLocationIndex > -1 && Clock.getBytecodesLeft() >= 500) {
            double oreAmount = rc.senseOre(oreLocations[oreLocationIndex]);
            MapLocation spotToCheck = oreLocations[oreLocationIndex];
            if (!spotToCheck.equals(Handler.myLoc) && rc.senseOre(spotToCheck) > Mining.ORE_THRESHOLD) {
                System.out.println("Trying to update frontier with " + spotToCheck + ", " + oreAmount + " ore");
                Mining.updateFrontierTowerToggle(spotToCheck, oreAmount);
                break;
            } else {
                oreLocationIndex--;
            }
        }
    }

    // If the frontier hasn't been updated for ORE_UPDATE rounds, or is 0, the
    // tower will look at its nearby
    // locations and broadcast if any of them have over ORE_THRESHOLD ore. The
    // order in which the tiles are
    // scanned is based of the HQ y-coordinate.
    protected static void broadcastOreLocationIncreasing() throws GameActionException {
        // All ore around the tower has been mined
        if (oreLocationIndex == oreLocations.length)
            return;

        // Not enough bytecode to continue
        if (Clock.getBytecodesLeft() < 500)
            return;

        // No frontier
        int frontier = Comm.readBlock(commMinerId, Mining.FRONTIER_OFFSET);
        if (frontier != 0) {
            // Frontier has been updated recently
            int roundNum = Comm.readBlock(commMinerId, Mining.FRONTIER_RND_NUM);
            
            if (Mining.isFrontierTower(frontier) && roundNum != Clock.getRoundNum()) {
                return;
            } else if(!Mining.isFrontierTower(frontier)){
                if (roundNum > Clock.getRoundNum() - ORE_UPDATE) {
                    return;
                }
            }
        }

        while (oreLocationIndex < oreLocations.length && Clock.getBytecodesLeft() >= 500) {
            double oreAmount = rc.senseOre(oreLocations[oreLocationIndex]);
            MapLocation spotToCheck = oreLocations[oreLocationIndex];
            if (rc.senseOre(spotToCheck) > Mining.ORE_THRESHOLD && !spotToCheck.equals(Handler.myLoc) && !isTower(spotToCheck)
                    && NavSafeBug.safeTile(spotToCheck)) {
                System.out.println("Trying to update frontier with " + spotToCheck + ", " + oreAmount + " ore");
                Mining.updateFrontierTowerToggle(spotToCheck, oreAmount);
                break;
            } else {
                oreLocationIndex++;
            }
        }
    }

    /**
     * Prunes the allied tower list to only include towers that are within your
     * sight range. This also does not include the tower itself.
     */
    private static void pruneTowerList() {
        int count = 0;
        int[] indices = new int[6];
        for (int i = nearbyTowers.length; --i >= 0;) {
            if (nearbyTowers[i].distanceSquaredTo(Handler.myLoc) <= 35 && !nearbyTowers[i].equals(Handler.myLoc)) {
                indices[count] = i;
                count++;
            }
        }
        MapLocation[] newTowerList = new MapLocation[count];
        for (int i = count; --i >= 0;) {
            newTowerList[i] = nearbyTowers[indices[i]];
        }
        nearbyTowers = newTowerList;
    }

    /**
     * Is the location an allied tower within my sight range?
     * 
     * @param loc
     * @return
     */
    private static boolean isTower(MapLocation loc) {
        for (int i = nearbyTowers.length; --i >= 0;) {
            if (loc.equals(nearbyTowers[i]))
                return true;
        }
        return false;
    }
}
