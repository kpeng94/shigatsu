package ryoutabot;

import battlecode.common.*;

public class Debug {

    public static void checkHealthOfNearbyAllies(int ind) {
        RobotInfo[] allies = Handler.rc.senseNearbyRobots(Handler.myLoc, 24, Handler.myTeam);
        int healthSum = 0;
        for (int i = 0; i < allies.length; i++) {
            healthSum += allies[i].health;
        }
        healthSum += Handler.rc.getHealth();
        Handler.rc.setIndicatorString(ind, "On clock turn: " + Clock.getRoundNum() +
                ", my allies have a total of " + healthSum + " health. numAllies: " + (allies.length + 1));
    }

    public static void logEnemyNumberData(int ind) {
        Handler.rc.setIndicatorString(ind, "On clock round: " + Clock.getRoundNum() + " numEnemiesAttackingUs: " + Handler.numEnemiesAttackingUs +
                " numEnemiesVisibleArr: " + Handler.visibleEnemies.length + " numEnemiesVisibleArr: " +
                Handler.numVisibleEnemyAttackers);

    }
}
