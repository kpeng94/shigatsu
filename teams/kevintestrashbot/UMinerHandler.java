package kevintestrashbot;

import battlecode.common.*;

public class UMinerHandler extends UnitHandler {

    static RobotInfo[] enemies;
    static MapLocation averagePositionOfMiners = null;
    static Direction lastDirectionMoved = null;

    static boolean movingToFrontier = false;
    static boolean usingBFS = false;
    static int pathIndex = 0;
    static MapLocation[] path;

    public static final int FRONTIER_OFFSET = 100;

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
                e.printStackTrace();
                System.out.println(typ + " Execution Exception");
            }
            rc.yield(); // Yields to save remaining bytecodes
        }
    }

    protected static void init(RobotController rcon) throws GameActionException {
        initUnit(rcon);
    }

    protected static void execute() throws GameActionException {
        executeUnit();
        readBroadcasts();
        if (rc.isWeaponReady() && decideAttack()) {
            attack();
        } else if (rc.isCoreReady()) {
            // Moving to frontier
            miningOverride: if (movingToFrontier) {
                if(rc.senseOre(myLoc) >= Constants.MINER_ORE_THRESHOLD && rc.canMine()){
                    movingToFrontier = false;
                    rc.mine();
                    break miningOverride;
                }
                if (usingBFS) {
                    if (pathIndex < path.length - 1) {
                        while (myLoc.distanceSquaredTo(path[pathIndex]) <= 2) {
                            pathIndex++;
                        }
                    }
                    if (pathIndex == path.length - 1) {
                        movingToFrontier = false;
                    }
                    NavSimple.walkTowardsDirected(myLoc.directionTo(path[pathIndex]));
                } else {
                    NavTangentBug.calculate(2500);
                    Direction nextMove = NavTangentBug.getNextMove();
                    if (nextMove != Direction.NONE) {
                    	NavSimple.walkTowards(nextMove);
                    }
                    if (myLoc.distanceSquaredTo(NavTangentBug.dest) <= 2) {
                        movingToFrontier = false;
                    }
                }
            } else {
                // Check ore tiles around you
                MapLocation ml;
                ml = findClosestMinableOreWithRespectToHQ(Constants.MINER_ORE_THRESHOLD, 6);
                RobotInfo[] nearbyRobots = rc.senseNearbyRobots(myLoc, 1, myTeam);
                int numMiners = 0;
    			for (int i = nearbyRobots.length; --i >= 0;) {
    				if (nearbyRobots[i].type == RobotType.MINER) numMiners++;
    			}
    			if (numMiners > 2) {
                    if (ml != null) {
                        tryMove(myLoc.directionTo(ml));
                    } else {
                        if (averagePositionOfMiners != null) {
                            tryMove(myLoc.directionTo(averagePositionOfMiners));
                        }
                    }
                } else if (rc.senseOre(myLoc) >= Constants.MINER_ORE_THRESHOLD && rc.canMine()) {
                    // System.out.println("Still mining");
                    rc.mine();
                } else {
                    if (ml != null) {
                        tryMove(myLoc.directionTo(ml));
                    } else {
                        int frontier = Comm.readBlock(Comm.getMinerId(), FRONTIER_OFFSET);
                        if (frontier != 0) {
                            movingToFrontier = true;
                            int hqMapBaseBlock = rc.readBroadcast(Comm.HQ_MAP_CHAN);
                            if (myLoc.distanceSquaredTo(myHQ) < 8 && !(NavBFS.readMapDataUncached(hqMapBaseBlock, frontier & 0xFFFF) == 0)) {
                                usingBFS = true;
                                path = NavBFS.backtrace(hqMapBaseBlock, MapUtils.decode(frontier & 0xFFFF));
                            } else {
                                usingBFS = false;
                                NavTangentBug.setDest(MapUtils.decode(frontier & 0xFFFF));
                            }
                        }
                    }
                }
            }
        }

        Supply.spreadSupplies(Supply.DEFAULT_THRESHOLD);
        Distribution.spendBytecodesCalculating(Handler.rc.getSupplyLevel() > 50 ? 7500 : 2500);

    }

    /**
     * Calculates the closest square with at least the threshold amount of ore.
     * The distance is calculated in terms of Manhattan distance and NOT
     * Euclidean distance. This does NOT factor in the square the robot is
     * currently on. Ignores squares with other robots on them already
     * 
     * @param rc
     *            - RobotController for the robot
     * @param threshold
     *            - the minimum amount of ore for the function to return
     * @param stepLimit
     *            - the size of the search outwards (a step limit of n will
     *            search in a [n by n] square, centered about the robot's
     *            current location
     * @param startingDirection
     *            - starting direction the search has
     * @return - MapLocation of closest square with ore greater than the
     *         threshold, or null if there is none
     */
    public static MapLocation findClosestMinableOre(double threshold, int stepLimit, Direction startingDirection) {
        int step = 1;
        Direction currentDirection = startingDirection;
        if (startingDirection.isDiagonal())
            currentDirection = currentDirection.rotateLeft();
        MapLocation currentLocation = rc.getLocation();

        while (step < stepLimit) {
            for (int i = step; --i >= 0;) {
                currentLocation = currentLocation.add(currentDirection);
                if (rc.senseOre(currentLocation) > threshold && rc.canMove(currentDirection) && rc.senseNearbyRobots(currentLocation, 0, myTeam).length == 0)
                    return currentLocation;
            }
            currentDirection = currentDirection.rotateLeft();
            currentDirection = currentDirection.rotateLeft();
            for (int i = step; --i >= 0;) {
                currentLocation = currentLocation.add(currentDirection);
                if (rc.senseOre(currentLocation) > threshold && rc.canMove(currentDirection) && rc.senseNearbyRobots(currentLocation, 0, myTeam).length == 0)
                    return currentLocation;
            }
            currentDirection = currentDirection.rotateLeft();
            currentDirection = currentDirection.rotateLeft();
            step++;
        }

        return null;
    }

    public static MapLocation findClosestMinableOreWithRespectToHQ(double threshold, int stepLimit) throws GameActionException {
        int step = 1;
        MapLocation currentLocation = Handler.myLoc;
        Direction currentDirection = currentLocation.directionTo(Handler.myHQ);
        if (currentDirection.isDiagonal())
            currentDirection = currentDirection.rotateRight();
        int bestDistance = 2000000;
        MapLocation bestLocation = null;

        // OH GOD PLEASE CHANGE THE NUMBER WHEN YOU CHANGE STEPLIMIT
        boolean[] occupied = getOccupiedTiles(18);
        int tilesSeen = 0;
        double totalOre = 0;
        double ore;

        while (step < stepLimit) {
            for (int i = step; --i >= 0;) {
                currentLocation = currentLocation.add(currentDirection);
                int distance = currentLocation.distanceSquaredTo(Handler.myHQ);
                ore = rc.senseOre(currentLocation);
                if (occupied[MapUtils.encode(currentLocation)]) {
                    totalOre += ore;
                    tilesSeen++;
                }
                if (ore > threshold && Handler.rc.canMove(Handler.myLoc.directionTo(currentLocation)) && distance < bestDistance
                        && !occupied[MapUtils.encode(currentLocation)]) {
                    bestLocation = currentLocation;
                    bestDistance = distance;
                }
            }

            if (step > 2 && bestDistance < 2000000) {
                updateFrontier(myLoc, totalOre / tilesSeen);
                return bestLocation;
            }
            currentDirection = currentDirection.rotateLeft();
            currentDirection = currentDirection.rotateLeft();
            for (int i = step; --i >= 0;) {
                currentLocation = currentLocation.add(currentDirection);
                ore = rc.senseOre(currentLocation);
                if (occupied[MapUtils.encode(currentLocation)]) {
                    totalOre += ore;
                    tilesSeen++;
                }
                int distance = currentLocation.distanceSquaredTo(Handler.myHQ);
                if (Handler.rc.senseOre(currentLocation) > threshold && Handler.rc.canMove(Handler.myLoc.directionTo(currentLocation))
                        && distance < bestDistance && !occupied[MapUtils.encode(currentLocation)]) {
                    bestLocation = currentLocation;
                    bestDistance = distance;
                }
            }
            currentDirection = currentDirection.rotateLeft();
            currentDirection = currentDirection.rotateLeft();
            step++;
        }

        updateFrontier(myLoc, totalOre / tilesSeen);
        return null;
    }

    public static void updateFrontier(MapLocation location, double heuristic) throws GameActionException {
        int minerBlockId = Comm.getMinerId();
        int frontierBlock = Comm.readBlock(minerBlockId, FRONTIER_OFFSET);
        int encodedLoc = MapUtils.encode(location);
        int heuristicInt = (int) (heuristic * 100);
        int encodedInfo = heuristicInt << 16 | encodedLoc;
        if (frontierBlock != 0) {
            if (heuristicInt > frontierBlock) {
                Comm.writeBlock(minerBlockId, FRONTIER_OFFSET, encodedInfo);
            }
        } else {
            Comm.writeBlock(minerBlockId, FRONTIER_OFFSET, encodedInfo);
        }
    }

    public static boolean[] getOccupiedTiles(int range) {
        RobotInfo[] robots = Handler.rc.senseNearbyRobots(range, Handler.myTeam);
        boolean[] occupied = new boolean[1 << 16];
        for (int i = robots.length; --i >= 0;) {
            int convertedLocation = MapUtils.encode(robots[i].location);
            occupied[convertedLocation] = true;
        }
        return occupied;
    }

    // This method will attempt to move in Direction d (or as close to it as
    // possible)
    static void tryMove(Direction d) throws GameActionException {
        int offsetIndex = 0;
        int[] offsets = { 0, 1, -1, 2, -2 };
        int dirint = directionToInt(d);
        while (offsetIndex < 5 && !rc.canMove(directions[(dirint + offsets[offsetIndex] + 8) % 8])) {
            offsetIndex++;
        }
        if (offsetIndex < 5) {
            Direction dirToMove = directions[(dirint + offsets[offsetIndex] + 8) % 8];
            lastDirectionMoved = dirToMove;
            rc.move(dirToMove);
        }
    }

    public static boolean decideAttack() {
        enemies = rc.senseNearbyRobots(typ.attackRadiusSquared, otherTeam);
        if (enemies.length > 0) {
            return true;
        }
        return false;
    }

    public static void attack() throws GameActionException {
        rc.attackLocation(enemies[0].location);
    }

    public static void readBroadcasts() throws GameActionException {
        int encodedMapCode = Comm.readBlock(Comm.getMinerId(), 2);
        int x = (encodedMapCode / 256) % 256;
        if (x > 128) {
            x = x - 256;
        }
        int y = encodedMapCode % 256;
        if (y > 128) {
            y = y - 256;
        }
        averagePositionOfMiners = new MapLocation(myHQ.x + x, myHQ.y + y);
    }
}
