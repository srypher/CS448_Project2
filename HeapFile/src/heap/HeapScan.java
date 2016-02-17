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
  boolean finished;

  protected HeapScan(HeapFile hf) {
	header = hf.header;
	iter = header.getPrev();
	curPage = new Page();
	if (iter != null && iter.getPage().firstRecord() != null) {
		curRID = iter.getPage().firstRecord();
		onFree = false;
		curPageId = iter.getPage().getCurPage();
		Minibase.BufferManager.pinPage(curPageId, curPage, false);
	} else {
		if (header.getNext() == null || header.getNext().getPage().firstRecord() == null) {
			System.out.println("error, nothing to scan");
			return;
		}
		iter = header.getNext();
		curRID = iter.getPage().firstRecord();
		onFree = true;
		curPageId = iter.getPage().getCurPage();
		Minibase.BufferManager.pinPage(curPageId, curPage, false);
	}
	
	finished = false;
  }

  protected void finalize() throws Throwable {

  }

  public void close() throws ChainException {

  }

  public boolean hasNext() {
	if (!iter.getPage().hasNext(curRID)) { //if there's nothing on the page
		if (iter.getNext() == null || iter.getNext().getPage().firstRecord() == null) { //and no next page
			if (onFree == true) { //and we we're on the free space list
				return false;
			}
		}
	}
	return true;
  }

  public Tuple getNext(RID rid) {
	if (finished) {
		Minibase.BufferManager.unpinPage(curPageId, false);
		return null;
	}
	byte[] bytesToReturn = iter.getPage().selectRecord(curRID);
	Tuple toReturn;
	toReturn = new Tuple(bytesToReturn, 0, bytesToReturn.length);

	if (!iter.getPage().hasNext(curRID)) { //if there's nothing more on the page
		Minibase.BufferManager.unpinPage(curPageId, false);
		iter = iter.getNext();
		if (iter == null || iter.getPage().firstRecord() == null) { //and no next page
			if (onFree == false) {
				if (header.getNext() == null) { 
					Minibase.BufferManager.pinPage(curPageId, curPage, false);
					finished = true;
					return toReturn;
				}
				iter = header.getNext();
				onFree = true;
			} else {
				Minibase.BufferManager.pinPage(curPageId, curPage, false);
				finished = true;
				return toReturn;
			}
		}

		curPageId = iter.getPage().getCurPage();
		Minibase.BufferManager.pinPage(curPageId, curPage, false);
		curRID = iter.getPage().firstRecord();
	} else {
		curRID = iter.getPage().nextRecord(curRID);
	}
	rid = curRID;
	return toReturn;
  }

}
