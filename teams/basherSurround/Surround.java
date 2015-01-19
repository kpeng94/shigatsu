package basherSurround;

import battlecode.common.*;

public class Surround {

	/*-------------------------------- RECALL FUNCTIONS --------------------------------*/
	
	private static final int NUM_CLUSTER_OFFSET = 0;
	private static final int TARGET_CLUSTER_OFFSET = 1;
	
	private static final int CLUSTER_INFO_SIZE = 4;
	
	private static final int CLUSTER_CENTER_X = 2;
	private static final int CLUSTER_CENTER_Y = 3;
	private static final int CLUSTER_SURROUND_RADIUS = 4;
	private static final int SHOULD_COLLAPSE = 5;
	
	private static MapLocation[] clusterCenters;
	private static int[] clusterWeights;
	private static int[] radii;
	private static int numClusters;
	
	public static void updateClusters() throws GameActionException {
		clusterCenters = new MapLocation[20];
		clusterWeights = new int[20];
		radii = new int[20];
		numClusters = 0;
		
		// Cluster together the enemies
		for (RobotInfo enemy: Handler.rc.senseNearbyRobots(999999, Handler.otherTeam)) {
			
			if (enemy.type.isBuilding) {
				continue;
			}
			
			boolean inCluster = false;
			
			// If the enemy is within a range of a previously determined cluster, 
			// add it to the cluster and perform correction on the safe radii and center
			for (int i = 0; i < numClusters; i++) {
				if (enemy.location.distanceSquaredTo(clusterCenters[i]) < radii[i]) {
					radii[i] += enemy.type.attackRadiusSquared;
					int avgX = (clusterCenters[i].x * clusterWeights[i] + enemy.location.x) / (clusterWeights[i] + 1);
					int avgY = (clusterCenters[i].x * clusterWeights[i] + enemy.location.x) / (clusterWeights[i] + 1);
					clusterCenters[i] = new MapLocation(avgX, avgY);
					clusterWeights[i]++;
				}
			}
			
			// If the enemy isn't in any cluster, make it a new cluster
			if (!inCluster && numClusters < 20) {
				clusterCenters[numClusters] = enemy.location;
				radii[numClusters] = enemy.type.attackRadiusSquared;
				numClusters++;
			}
		}
		
		// Identify the best cluster to move to or attack
		
		// Broadcast the cluster information
		Comm.writeBlock(getSurroundId(), NUM_CLUSTER_OFFSET, numClusters);
		
		for (int i = 0; i < numClusters; i++) {
			Comm.writeBlock(getSurroundId(), i * CLUSTER_INFO_SIZE + CLUSTER_CENTER_X, clusterCenters[i].x);
			Comm.writeBlock(getSurroundId(), i * CLUSTER_INFO_SIZE + CLUSTER_CENTER_Y, clusterCenters[i].y);
			Comm.writeBlock(getSurroundId(), i * CLUSTER_INFO_SIZE + CLUSTER_SURROUND_RADIUS, radii[i]);
		}
		
		
	}
	
	/*-------------------------------- COMM FUNCTIONS --------------------------------*/
	
	public static final int SURROUND_BLOCK = 200;
	public static int surroundBlockId = 0;

	/**
	 * Returns the block id of the dedicated scout block Creates a scout block
	 * if it was not previously
	 */
	public static int getSurroundId() throws GameActionException {
		if (surroundBlockId == 0) {
			surroundBlockId = Handler.rc.readBroadcast(SURROUND_BLOCK);
			if (surroundBlockId == 0) {
				surroundBlockId = Comm.requestBlock(true);
				Handler.rc.broadcast(SURROUND_BLOCK, surroundBlockId);
			}
		}
		return surroundBlockId;
	}
}
