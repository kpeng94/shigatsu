package qualifyingBotv3;

import battlecode.common.*;

public class Mining {
    public static final int FRONTIER_OFFSET = 100;
    public static final int FRONTIER_RND_NUM = 101;

    public static final int ADJ_THRESHOLD = 2;
    public static final int ORE_THRESHOLD = 2;

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
                ore = Handler.rc.senseOre(currentLocation);
                if (!occupied[MapUtils.encode(currentLocation)] && NavSafeBug.safeTile(currentLocation)) {
                    totalOre += ore;
                    tilesSeen++;
                    if (ore > threshold && Handler.rc.canMove(Handler.myLoc.directionTo(currentLocation)) && distance < bestDistance) {
                        bestLocation = currentLocation;
                        bestDistance = distance;
                    }
                }
            }

            if (step > 2 && bestDistance < 2000000) {
                if (tilesSeen != 0)
                    updateFrontier(Handler.myLoc, totalOre / tilesSeen);
                return bestLocation;
            }
            currentDirection = currentDirection.rotateLeft();
            currentDirection = currentDirection.rotateLeft();
            for (int i = step; --i >= 0;) {
                currentLocation = currentLocation.add(currentDirection);
                ore = Handler.rc.senseOre(currentLocation);
                if (!occupied[MapUtils.encode(currentLocation)] && NavSafeBug.safeTile(currentLocation)) {
                    totalOre += ore;
                    tilesSeen++;
                    int distance = currentLocation.distanceSquaredTo(Handler.myHQ);
                    if (ore > threshold && Handler.rc.canMove(Handler.myLoc.directionTo(currentLocation))
                            && distance < bestDistance) {
                        bestLocation = currentLocation;
                        bestDistance = distance;
                    }
                }
            }
            currentDirection = currentDirection.rotateLeft();
            currentDirection = currentDirection.rotateLeft();
            step++;
        }

        if (tilesSeen != 0)
            updateFrontier(Handler.myLoc, totalOre / tilesSeen);
        return null;
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

    public static void updateFrontier(MapLocation location, double heuristic) throws GameActionException {
        if (heuristic < ORE_THRESHOLD) {
            return;
        }

        int minerBlockId = Comm.getMinerId();
        int frontierBlock = Comm.readBlock(minerBlockId, FRONTIER_OFFSET);
        int frontierRoundNum = Comm.readBlock(minerBlockId, FRONTIER_RND_NUM);
        int encodedLoc = MapUtils.encode(location);
        int heuristicInt = (int) (heuristic * 32);
        int encodedInfo = (heuristicInt << 16) | encodedLoc;
        int roundNum = Clock.getRoundNum();
        if (frontierBlock != 0) {
            if (heuristicInt > ((frontierBlock >>> 16) & 0x7FFF) || roundNum >= frontierRoundNum + 3) {
                Comm.writeBlock(minerBlockId, FRONTIER_OFFSET, encodedInfo);
                Comm.writeBlock(minerBlockId, FRONTIER_RND_NUM, roundNum);
            }
        } else {
            Comm.writeBlock(minerBlockId, FRONTIER_OFFSET, encodedInfo);
            Comm.writeBlock(minerBlockId, FRONTIER_RND_NUM, roundNum);
        }
    }

    /** Tower equivalent of updating frontier. Sets the 31st bit in the frontier offset block to 1 to indicate that 
     * a tower set the frontier. 
     * 
     * @param location
     * @param heuristic
     * @throws GameActionException
     */
    public static void updateFrontierTowerToggle(MapLocation location, double heuristic) throws GameActionException {
        if (heuristic < ORE_THRESHOLD) {
            return;
        }

        int minerBlockId = Comm.getMinerId();
        int frontierBlock = Comm.readBlock(minerBlockId, FRONTIER_OFFSET);
        int encodedLoc = MapUtils.encode(location);
        int heuristicInt = (int) (heuristic * 32);
        int encodedInfo = (1 << 31) | (heuristicInt << 16) | encodedLoc;
        int roundNum = Clock.getRoundNum();
        if (frontierBlock != 0) {
            MapLocation previousFrontier = MapUtils.decode(frontierBlock & 0xFFFF);
            if (Handler.myHQ.distanceSquaredTo(previousFrontier) > Handler.myHQ.distanceSquaredTo(location)) {
                Comm.writeBlock(minerBlockId, FRONTIER_OFFSET, encodedInfo);
                Comm.writeBlock(minerBlockId, FRONTIER_RND_NUM, roundNum);
            }
        } else {
            Comm.writeBlock(minerBlockId, FRONTIER_OFFSET, encodedInfo);
            Comm.writeBlock(minerBlockId, FRONTIER_RND_NUM, roundNum);
        }
    }

    public static void resetFrontier() throws GameActionException {
        Comm.writeBlock(Comm.getMinerId(), FRONTIER_OFFSET, 0);
    }

    public static MapLocation getFrontierLocation() throws GameActionException {
        int frontier = Comm.readBlock(Comm.getMinerId(), FRONTIER_OFFSET);
        if (frontier != 0) {
            return MapUtils.decode(frontier & 0xFFFF);
        } else {
            return null;
        }
    }
    
    public static boolean isFrontierTower(int frontier){
        return (frontier >>> 31) == 1;
    }

}
