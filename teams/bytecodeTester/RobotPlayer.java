package bytecodeTester;

import battlecode.common.*;

public class RobotPlayer {
	public static void func1() {
		
	}

	public static void func2() {
		
	}
	
	public static void run(RobotController rc) {
		int start1 = Clock.getBytecodeNum();
		func1();
		int end1 = Clock.getBytecodeNum();
		
		int start2 = Clock.getBytecodeNum();
		func2();
		int end2 = Clock.getBytecodeNum();

		System.out.println("Function 1 runs in: " + (end1 - start1));
		System.out.println("Function 2 runs in: " + (end2 - start2));		
		while(true) {
			
		}
	}
}
