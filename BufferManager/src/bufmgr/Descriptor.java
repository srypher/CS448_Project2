package bufmgr;

import global.PageId;

public class Descriptor {
	private PageId pagenumber;
	private int pin_count;
	private boolean dirtybit;
	private int lfucounter;

	Descriptor(PageId pagenumber) {
			this.pagenumber = pagenumber;
			pin_count = 0;
			dirtybit = false;
	}

	//toggle dirty bit to on
	public void toggleDirty() {
		dirtybit = true;
	}
	public void untoggleDirty() {
		dirtybit = false;
	}

	//get dirtybit value;
	public boolean getDirty() {
		return dirtybit;
	}

	//get pagenumber
	public PageId getPagenumber() {
		return pagenumber;
	}

	public int getLFUCount() {
		return lfucounter;
	}

	//get pin count
	public int getPinCount() {
		return pin_count;
	}

	//increment the pin count and lfucounter
	public void pinPage() {
		pin_count++;
		lfucounter++;
	}

	//decrement the pin count
	public void unpinPage() {
		pin_count--;
	}

}
