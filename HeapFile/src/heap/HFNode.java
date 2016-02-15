package heap;

public class HFNode {
	private HFPage page;
	private HFNode nextNode;
	private HFNode prevNode;

	public HFNode(HFPage cur, HFNode prev, HFNode next) {
		page = cur;
		nextNode = next;
		prevNode = prev;
	}

	public HFPage getPage() { return page; }
	public HFNode getNext() { return nextNode; }
	public HFNode getPrev() { return prevNode; }
	public int getFreeSpace() { return page.getFreeSpace(); }
	public void setPage(HFPage nPage) { page = nPage; }
	public void setNext(HFNode nNext) { nextNode = nNext; }
	public void setPrev(HFNode nPrev) { prevNode = nPrev; }
}
