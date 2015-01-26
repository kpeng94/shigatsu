package genghisPostSeedRework;

import battlecode.common.*;

public class LauncherWave {

    private static final int[] WAVE_OFFSETS = { 100, 101, 102, 103, 104, 105 };
    public static int squadronNumber = -1;

    private static final int ODD_COUNT_MASK  = 0x003F0000;
    private static final int EVEN_COUNT_MASK = 0x0FC00000;
    private static final int COMBINE_MASK    = 0x70000000;
    private static final int LOC_MASK        = 0x0000FFFF;

    public static void setWave(int waveNum, int data) throws GameActionException {
        Comm.writeBlock(Comm.getLauncherId(), WAVE_OFFSETS[waveNum], data);
    }

    public static int getWave(int waveNum) throws GameActionException {
        return Comm.readBlock(Comm.getLauncherId(), WAVE_OFFSETS[waveNum]);
    }

    public static void launcherRoleCall() throws GameActionException {
        if(squadronNumber == -1)
            return;
        int data = Comm.readBlock(Comm.getLauncherId(), WAVE_OFFSETS[squadronNumber]);
        if (Clock.getRoundNum() % 2 == 0) {
            int role = (data & EVEN_COUNT_MASK) >>> 22;
            Comm.writeBlock(Comm.getLauncherId(), WAVE_OFFSETS[squadronNumber], (data & ~EVEN_COUNT_MASK) | (role + 1) << 22);
        } else {
            int role = (data & ODD_COUNT_MASK) >>> 16;
            Comm.writeBlock(Comm.getLauncherId(), WAVE_OFFSETS[squadronNumber], (data & ~ODD_COUNT_MASK) | (role + 1) << 16);
        }
    }

    public static void launcherRoleCall(int data) throws GameActionException {
        if(squadronNumber == -1)
            return;
        if (Clock.getRoundNum() % 2 == 0) {
            int role = (data & EVEN_COUNT_MASK) >>> 22;
            Comm.writeBlock(Comm.getLauncherId(), WAVE_OFFSETS[squadronNumber], (data & ~EVEN_COUNT_MASK) | (role + 1) << 22);
        } else {
            int role = (data & ODD_COUNT_MASK) >>> 16;
            Comm.writeBlock(Comm.getLauncherId(), WAVE_OFFSETS[squadronNumber], (data & ~ODD_COUNT_MASK) | (role + 1) << 16);
        }
    }

    public static void setSquadNumber(int num) {
        squadronNumber = num;
    }

    public static int getSquadCount(int squadNum) throws GameActionException {
        int data = Comm.readBlock(Comm.getLauncherId(), WAVE_OFFSETS[squadNum]);
        if (Clock.getRoundNum() % 2 == 0) {
            return (data & ODD_COUNT_MASK) >>> 16;
        }
        return (data & EVEN_COUNT_MASK) >>> 22;
    }

    public static void resetCounts() throws GameActionException {
        int rnd = Clock.getRoundNum();
        for (int i = 6; --i >= 0;) {
            int data = Comm.readBlock(Comm.getLauncherId(), WAVE_OFFSETS[i]);
            if (data != 0) {
                if (rnd % 2 == 0) {
                    Comm.writeBlock(Comm.getLauncherId(), WAVE_OFFSETS[i], data & ~EVEN_COUNT_MASK);
                } else {
                    Comm.writeBlock(Comm.getLauncherId(), WAVE_OFFSETS[i], data & ~ODD_COUNT_MASK);
                }
            }
        }
    }

}
