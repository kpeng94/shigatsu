package droneTestSimple;

public class IdMap<V> {
	V[] map;
	
	public IdMap() {
		map = (V[]) new Object[10];
	}
	
	public V get(int key) {
		if (key > map.length) {
			return null;
		}
		else {
			return map[key]; 
		}
	}
	
	public void put(int key, V value) {
		if (key > map.length) {
			V[] big_map = (V[]) new Object[key * 2];
			System.arraycopy(map, 0, big_map, 0, map.length);
			map = big_map;
		}
		map[key] = value;
	}
}
