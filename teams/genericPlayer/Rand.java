package genericPlayer;

/**
 * 
 * @author Rene
 * 
 * Class for handling random integer generation
 * Initialize with a seed using the constructor
 * 
 * nextInt() returns a random integer
 * nextInt(int max) returns a nonnegative integer less than max
 * nextAnd(int n) returns a nonnegative integer bitwise anded with n
 *
 */
public class Rand {
	static final long a = 0xffffda61L;
	private long x;
	
	public Rand(long seed) {
		x = seed & 0xffffffffL;
	}
	
	public int nextInt() {
		return (int) ((a * (x & 0xffffffffL)) + (x >>> 32));
	}
	
	public int nextInt(int max) {
		return (int) (((a * (x & 0xffffffffL)) + (x >>> 32)) & 0x7fffffff) % max;
	}
	
	public int nextAnd(int n) {
		return (int) (((a * (x & 0xffffffffL)) + (x >>> 32)) & n);
	}
	
}
