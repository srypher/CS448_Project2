package heap;

public class Tuple {

	byte[] tuple;
	
	public Tuple() {
		
	}

	public Tuple(byte[] base, int start, int end) {
		tuple = new byte[end - start];
		for (int i = 0, j = start; j < end; i++, j++) {
			tuple[i] = base[j];
		}
	}

	public int getLength() {
		return tuple.length;
	}

	public byte[] getTupleByteArray() {
		return tuple;
	}
}
