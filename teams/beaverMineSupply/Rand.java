package beaverMineSupply;

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
	
	public int next() {
		x = (a * (x & 0xffffffffL)) + (x >>> 32);
		return (int) x;
	}
	
	public int nextInt(int max) {
		x = ((a * (x & 0xffffffffL)) + (x >>> 32)) & 0x7fffffff;
		return (int) (x % max);
	}
	
	public int nextAnd(int n) {
		x = (a * (x & 0xffffffffL)) + (x >>> 32);
		return (int) (x & n);
	}
	
}
