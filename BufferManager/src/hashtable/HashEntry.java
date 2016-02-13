package hashtable;

public class HashEntry {
    private int key;
    private int value;
    private HashEntry nextEntry;
    
    HashEntry(int key, int value) {
        this.key = key;
        this.value = value;
        nextEntry = null;
    }     
    
    public int getKey() {
        return key;
    }
    
    public int getValue() {
        return value;
    }
    
    public HashEntry getNextEntry() {
        return nextEntry;
    }
    
    public void setValue(int newValue) {
        value = newValue;
    }
    
    public void setNextEntry(HashEntry newNext) {
        nextEntry = newNext;
    }
    
    
}
