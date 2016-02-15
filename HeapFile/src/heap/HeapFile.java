package heap;

import global.Convert;
import global.GlobalConst;
import global.Minibase;
import global.RID;
import global.Page;
import global.PageId;

import bufmgr.BufMgr;
import diskmgr.DiskMgr;
import heap.HFPage;
import heap.Tuple;
import heap.HeapScan;
import heap.HFNode;

import java.io.File;

import chainexception.ChainException;

public class HeapFile {
    
  protected HFNode header; //Prev is No Space list, next is Free Space list. Free space is ordered from least space to most.
  private int recCount;

  public HeapFile(String name) {
        Minibase.DiskManager.closeDB();
	Minibase.DiskManager.openDB(name);
	header = new HFNode(new HFPage(), null, null);
        int numPages = Minibase.DiskManager.getAllocCount();
        HFNode newHFNode, iter;
        Page newPage;
	HFPage newHFPage;
	RID pageCounter;
	recCount = 0;

	//read the pages from the disk
       	for (int i = 1; i <= numPages; i++) {
       		//get page from disk
		newPage = new Page();
		Minibase.DiskManager.read_page(new PageId(i), newPage);
		newHFPage = new HFPage(newPage);
		pageCounter = newHFPage.firstRecord();
		try {
			while (pageCounter != null) {
				recCount++;
				pageCounter = newHFPage.nextRecord(pageCounter);
			}
		} catch (Exception e) {}

		if (newHFPage.getFreeSpace() == 0) { //put it in Free Space List
			if (header.getNext() == null) {
				newHFNode = new HFNode(newHFPage, header, null);
				header.setNext(newHFNode);
			}
			else {
				iter = header.getNext();
				while (iter.getNext() != null && iter.getFreeSpace() < newHFPage.getFreeSpace()) {
					iter = iter.getNext();
				}
				if (iter.getNext() == null && iter.getFreeSpace() < newHFPage.getFreeSpace()) {
					//insert at the end of the list
					newHFNode = new HFNode(newHFPage, iter, null);
					iter.setNext(newHFNode);
				} else {
					//insert before iterator
					newHFNode = new HFNode(newHFPage, iter.getPrev(), iter);
					iter.getPrev().setNext(newHFNode);
					iter.setPrev(newHFNode);
				}
			}
		} else { //put it in No Space list
			newHFNode = new HFNode(newHFPage, header, header.getPrev());
			if (header.getPrev() != null)
				header.getPrev().setPrev(newHFNode);
			header.setNext(newHFNode);
		}
        }

	
    }
    
    public RID insertRecord(byte[] record) throws ChainException {
	Page newPage;
	HFPage newHFPage;
	HFNode targetNode = header;
	HFNode newHFNode;
	RID toReturn;
	HFPage bucket;
	
	while (targetNode.getNext() != null && header.getNext().getFreeSpace() < record.length) {
		targetNode = targetNode.getNext();
	}
	if (targetNode.getNext() == null) {
		//free list doesn't have a page for it, allocate a new page
		newPage = new Page();
		Minibase.DiskManager.read_page(Minibase.DiskManager.allocate_page(), newPage);
		newHFPage = new HFPage(newPage);
		toReturn = newHFPage.insertRecord(record);
		
		//add to no space list if page is full
		if (newHFPage.getFreeSpace() == 0) {
			newHFNode = new HFNode(newHFPage, header, header.getPrev());
			if (header.getPrev() != null) header.getPrev().setPrev(newHFNode);
			header.setPrev(newHFNode);
			recCount++;
			return toReturn;
		}

		newHFNode = new HFNode(newHFPage, targetNode, null);
		targetNode.setNext(newHFNode);
		
		//go through the list to find the place for the record
		targetNode = newHFNode;
		while (targetNode.getPrev() != header && targetNode.getPrev().getFreeSpace() > targetNode.getFreeSpace()) {
			bucket = targetNode.getPage();
			targetNode.setPage(targetNode.getPrev().getPage());
			targetNode.getPrev().setPage(bucket);
			targetNode = targetNode.getPrev();
		}
		recCount++;
		return toReturn;
	}

	targetNode = header.getNext();
	toReturn = targetNode.getPage().insertRecord(record);
	if (targetNode.getFreeSpace() == 0) {
		//remove from free list, add to no space list
		targetNode.getNext().setPrev(targetNode.getPrev());
		targetNode.getPrev().setNext(targetNode.getNext());
		targetNode.setPrev(header);
		targetNode.setNext(header.getPrev());
		header.getPrev().setPrev(targetNode);
		header.setPrev(targetNode);
		recCount++;
		return toReturn;
	}
	//put page in place
	while (targetNode.getPrev() != header && targetNode.getPrev().getFreeSpace() > targetNode.getFreeSpace()) {
		bucket = targetNode.getPage();
		targetNode.setPage(targetNode.getPrev().getPage());
		targetNode.getPrev().setPage(bucket);
		targetNode = targetNode.getPrev();
	}
	recCount++;
	return toReturn;
    }
    
    public Tuple getRecord(RID rid) throws ChainException {
        return null; 
    }
    
    public boolean updateRecord(RID rid, Tuple newRecord) throws ChainException {
        return false;
    }
    
    public boolean deleteRecord(RID rid) throws ChainException {
        return false;
    }
    
    public int getRecCnt() { //get number of records in the file
        return recCount;
    }
    
    public HeapScan openScan() {
    	System.out.println("Free:\n--------------------");
	HFNode node = header.getNext();
	while (node != null) {
		node.getPage().print();
		node = node.getNext();
	}
	System.out.println("Used:\n--------------------");
	node = header.getPrev();
	while (node != null) {
		node.getPage().print();
		node = node.getNext();
	}
        return new HeapScan(this);
    }
}
