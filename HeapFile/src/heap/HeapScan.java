package heap;

import global.RID;
import global.Minibase;
import heap.HFPage;
import heap.HeapFile;
import global.Page;
import global.PageId;
import global.RID;

import chainexception.ChainException;

public class HeapScan {

  HFNode iter, header;
  boolean onFree;
  Page curPage;
  PageId curPageId;
  RID curRID;

  protected HeapScan(HeapFile hf) {
	header = hf.header;
	iter = header.getNext();
	curPage = new Page();
	if (iter != null && iter.getPage().firstRecord() != null) {
		/**/System.out.println("Flag 0.1");
		onFree = true;
		curPageId = iter.getPage().getCurPage();
		Minibase.BufferManager.pinPage(curPageId, curPage, false);
	} else {
		/**/System.out.println("Flag 0.2");
		if (header.getPrev() == null) 
			return;
		iter = header.getPrev();
		onFree = false;
		curPageId = iter.getPage().getCurPage();
		Minibase.BufferManager.pinPage(curPageId, curPage, false);
	}
  }

  protected void finalize() throws Throwable {

  }

  public void close() throws ChainException {

  }

  public boolean hasNext() {
	if (iter.getPage().nextRecord(curRID) == null) { //if there's nothing on the page
		if (iter.getNext() == null || iter.getNext().getPage().firstRecord() == null) { //and no next page
			if (onFree == false) { //and we we're on the no space list
				return false;
			}
		}
	}
	return true;
  }

  public Tuple getNext(RID rid) {
	byte[] bytesToReturn = iter.getPage().selectRecord(curRID);
	Tuple toReturn;
	toReturn = new Tuple(bytesToReturn, 0, bytesToReturn.length);

	System.out.println("iter: " + iter + "\npage: " + iter.getPage() + "\nRID: " + curRID);
	
	if (!iter.getPage().hasNext(curRID)) { //if there's nothing more on the page
		if (iter != header) Minibase.BufferManager.unpinPage(curPageId, false);
		iter = iter.getNext();
		if (iter == null || iter.getPage().firstRecord() == null) { //and no next page
			if (onFree == true) {
				if (header.getPrev() == null) 
					return null;
				iter = header.getPrev();
				onFree = false;
			} else {
				return null;
			}
		}
		curPageId = iter.getPage().getCurPage();
		Minibase.BufferManager.pinPage(curPageId, curPage, false);
		curRID = iter.getPage().firstRecord();
	} else {
		curRID = iter.getPage().nextRecord(curRID);
	}
	return toReturn;
  }

}
