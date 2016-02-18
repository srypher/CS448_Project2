package hashtable;

public class HashTable {
    private final static int HTSIZE = 101; //arbitrary size. Change later?
    private int maxSize; //the maximum amount of entries (i.e., the size of the buffer), 0 if there is no max
    private int currentSize; //the current amount of entries
    
    HashEntry[] table;
    
    public HashTable() {
        table = new HashEntry[HTSIZE];
        for (int i = 0; i < HTSIZE; i++) {
            table[i] = null;
        }
        maxSize = 0;
        currentSize = 0;
    }
    
    //initialize with a max size
    public HashTable(int max) {
        this();
        maxSize = max;
        
    }
    
    //get value from hashtable, return -1 if it does not exist
    public int get(int key) {
        int hash = (2 * key + 1) % HTSIZE;
        
        //check if bucket exists
        if (table[hash] == null) 
            return -1;
        
        //go through the list
        HashEntry search = table[hash];

        while (search.getNextEntry() != null) {
            //check if we found the key
            if (search.getKey() == key)
                return search.getValue();
            
            search = search.getNextEntry();
        }
        
        //we're now on the last node
        if (search.getKey() == key)
            return search.getValue();
        else
            return -1;
    }
    
    //returns -1 on size failure (throw some kind of error if it returns this), 0 on success
    public int put(int key, int value) {
        //check if table is full
        if (maxSize != 0 && currentSize >= maxSize) {
            System.out.println("Houston we have a problem");
            //error, exceed max
            return -1;
        }
        currentSize++;
        
        int hash = (2 * key + 1) % HTSIZE;
        
        //check if bucket exists
        if (table[hash] == null) {
            table[hash] = new HashEntry(key, value);
            return 0;
        }
        
        //put new entry at end of the list
        HashEntry search = table[hash];
        
        while (search.getNextEntry() != null)
            search = search.getNextEntry();
        
        search.setNextEntry(new HashEntry(key, value));
        
        return 0;
    }
    
    public void set(int key, int newValue) {
        int hash = (2 * key + 1) % HTSIZE;
        
        //check if bucket exists
        if (table[hash] == null) 
            return;
        
        //check bucket
        if (table[hash].getKey() == key) {
            table[hash].setValue(newValue);
            return;
        }
        
        //go through the list
        HashEntry search = table[hash];

        while (search.getNextEntry() != null) {
            //check if we found the key
            if (search.getKey() == key) {
                search.setValue(newValue);
                return;
            }
            
            search = search.getNextEntry();
        }
        
        //we're now on the last node
        if (search.getKey() == key) {
            search.setValue(newValue);
            return;
        }
    }
    
    public void delete(int key) {
        int hash = (2 * key + 1) % HTSIZE;
        //check if bucket exists
        if (table[hash] == null) 
            return;
        
        //check bucket
        if (table[hash].getKey() == key) {
            table[hash] = table[hash].getNextEntry();
            currentSize--;
            return;
        }
        
        //go through the list
        HashEntry search = table[hash];
        
        while (search.getNextEntry() != null) {
            //check if we found the key in the next entry
            if (search.getNextEntry().getKey() == key) {
                search.setNextEntry(search.getNextEntry().getNextEntry());
                currentSize--;
                return;
            }
            
            search = search.getNextEntry();
        }
        
        //didn't find it. oh well.
    }
    
    //Just for testing
    public static void main (String argv[]) {
        HashTable ht = new HashTable(2);
        ht.put(0, 10);
        ht.put(1, 11);
        if(ht.put(101, 12)==-1) System.out.println("SUCCESS");
        ht.delete(1);
        ht.set(101, 13);
        int x, y, z;
        x = ht.get(0);
        y = ht.get(1);
        z = ht.get(101);
        
        System.out.print(x + " " + y + " " + z);
    }
}
