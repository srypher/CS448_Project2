public class HashTable {
    private final static int HTSIZE = 101; //arbitrary size. Change later?
    
    HashEntry[] table;
    
    HashTable() {
        table = new HashEntry[HTSIZE];
        for (int i = 0; i < HTSIZE; i++) {
            table[i] = null;
        }
    }
    
    //get value from hashtable, return -1 if it does not exist
    public int get(int key) {
        int hash = key % HTSIZE;
        while (table[hash] != null && table[hash].getKey() != key)
            hash = (2*hash + 1) % HTSIZE;
        if (table[hash] == null)
            return -1;
        else
            return table[hash].getValue();
    }
    
    public void put(int key, int value) {
        int hash = key % HTSIZE;
        while (table[hash] != null && table[hash].getKey() != key)
            hash = (2*hash + 1) % HTSIZE;
        table[hash] = new HashEntry(key, value);
    }
    
    public void set(int key, int newValue) {
        int hash = key % HTSIZE;
        while (table[hash] != null && table[hash].getKey() != key)
            hash = (2*hash + 1) % HTSIZE;
        table[hash].setValue(newValue);
    }
    
    public static void main (String argv[]) {
        HashTable ht = new HashTable();
        ht.put(1, 10);
        ht.put(2, 11);
        ht.put(129, 12);
        int x, y, z;
        x = ht.get(1);
        y = ht.get(2);
        z = ht.get(129);
        
        System.out.print(x + " " + y + " " + z);
    }
}