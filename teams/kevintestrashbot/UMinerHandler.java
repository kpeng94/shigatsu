package kevintestrashbot;

import battlecode.common.*;

public class UMinerHandler extends UnitHandler {

    static RobotInfo[] enemies;
    static MapLocation averagePositionOfMiners = null;
    static Direction lastDirectionMoved = null;

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
            MapLocation ml;
            ml = findClosestMinableOreWithRespectToHQ(Constants.MINER_ORE_THRESHOLD, 6);
            if (ml != null)
                rc.setIndicatorString(2, Clock.getRoundNum() + ": " + ml.toString());
            else
                rc.setIndicatorString(2, Clock.getRoundNum() + ": " + "null");
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots(myLoc, 2, myTeam);
            if (nearbyRobots.length > 2) {
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
                    if (averagePositionOfMiners != null) {
                        // rc.setIndicatorString(1, "averagePositionOfMiners: " + averagePositionOfMiners + " myLoc: " + myLoc);
                        // System.out.println("" +
                        // myLoc.directionTo(averagePositionOfMiners));
                        tryMove(myLoc.directionTo(averagePositionOfMiners));
                    } else {
                        // System.out.println("It's null though?" +
                        // averagePositionOfMiners);
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
        if(startingDirection.isDiagonal())
            currentDirection = currentDirection.rotateLeft();
        MapLocation currentLocation = rc.getLocation();

        while (step < stepLimit) {
            for (int i = step; --i >= 0;) {
                currentLocation = currentLocation.add(currentDirection);
                if (rc.senseOre(currentLocation) > threshold && rc.canMove(currentDirection)
                        && rc.senseNearbyRobots(currentLocation, 0, myTeam).length == 0)
                    return currentLocation;
            }
            currentDirection = currentDirection.rotateLeft();
            currentDirection = currentDirection.rotateLeft();
            for (int i = step; --i >= 0;) {
                currentLocation = currentLocation.add(currentDirection);
                if (rc.senseOre(currentLocation) > threshold && rc.canMove(currentDirection)
                        && rc.senseNearbyRobots(currentLocation, 0, myTeam).length == 0)
                    return currentLocation;
            }
            currentDirection = currentDirection.rotateLeft();
            currentDirection = currentDirection.rotateLeft();
            step++;
        }

        return null;
    }
    
    public static MapLocation findClosestMinableOreWithRespectToHQ(double threshold, int stepLimit){
        int step = 1;
        MapLocation currentLocation = Handler.myLoc;
        Direction currentDirection = currentLocation.directionTo(Handler.myHQ);
        if(currentDirection.isDiagonal())
            currentDirection = currentDirection.rotateRight();
        int bestDistance = 2000000;
        MapLocation bestLocation = null;

        while (step < stepLimit) {
            for (int i = step; --i >= 0;) {
                currentLocation = currentLocation.add(currentDirection);
                int distance = currentLocation.distanceSquaredTo(Handler.myHQ);
                if (Handler.rc.senseOre(currentLocation) > threshold && Handler.rc.canMove(Handler.myLoc.directionTo(currentLocation)) && distance < bestDistance
                        && Handler.rc.senseNearbyRobots(currentLocation, 0, Handler.myTeam).length == 0){
                    bestLocation = currentLocation;
                    bestDistance = distance;
                }
            }

            if(step > 2 && bestDistance < 2000000){
                return bestLocation;
            }
            currentDirection = currentDirection.rotateLeft();
            currentDirection = currentDirection.rotateLeft();
            for (int i = step; --i >= 0;) {
                currentLocation = currentLocation.add(currentDirection);                
                int distance = currentLocation.distanceSquaredTo(Handler.myHQ);
                if (Handler.rc.senseOre(currentLocation) > threshold && Handler.rc.canMove(Handler.myLoc.directionTo(currentLocation)) && distance < bestDistance
                        && Handler.rc.senseNearbyRobots(currentLocation, 0, Handler.myTeam).length == 0){
                    bestLocation = currentLocation;
                    bestDistance = distance;
                }
            }
            currentDirection = currentDirection.rotateLeft();
            currentDirection = currentDirection.rotateLeft();
            step++;
        }
        Handler.rc.setIndicatorString(2, "myLoc: " + Handler.myLoc + " " + Clock.getRoundNum() + ": " + "null");

        return null;
    }

	public static boolean[][] getOccupiedTiles(int range) {
		RobotInfo[] robots = Handler.rc.senseNearbyRobots(range, Handler.myTeam);
		boolean[][] occupied = new boolean[GameConstants.MAP_MAX_WIDTH][GameConstants.MAP_MAX_HEIGHT];
		for (int i = robots.length; --i >= 0;) {
			MapLocation convertedLocation = MapUtils.encodeMapLocation(robots[i].location);
			occupied[convertedLocation.x][convertedLocation.y] = true;
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
