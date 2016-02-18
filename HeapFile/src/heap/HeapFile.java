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

  static final int SLOT_SIZE = 4; //size of a slot in HFPage. This + record length = free space needed
  				  //note that HFPage has 1028 bytes, 20 of which are used for metadata, and 4 extra byte are used per record

  public HeapFile(String name) {
	Minibase.DiskManager.openDB(name);
	header = new HFNode(new HFPage(), null, null);
        int numPages = Minibase.DiskManager.getAllocCount();
        HFNode newHFNode, iter;
        Page newPage = new Page();
	PageId newPageId = new PageId();
	HFPage newHFPage = new HFPage();
	RID pageCounter;
	recCount = 0;

	//Minibase.DiskManager.print_space_map();
	
	//read the pages from the disk
       	for (int i = 1; i <= numPages; i++) {
       		//get page from disk
		newPage = new Page();
		newPageId = new PageId(i);
		newHFPage = new HFPage();
		Minibase.DiskManager.read_page(newPageId, newHFPage);
		newHFPage.setCurPage(newPageId);
		pageCounter = newHFPage.firstRecord();
		try {
			while (pageCounter != null) {
				recCount++;
				pageCounter = newHFPage.nextRecord(pageCounter);
			}
		} catch (Exception e) { System.out.println("ERROR READING RECORDS!"); }

		if (newHFPage.getFreeSpace() > SLOT_SIZE) { //put it in Free Space List
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
	PageId newPageId;
	HFPage newHFPage;
	HFNode targetNode = header;
	HFNode newHFNode;
	HFNode noSpaceIter;
	RID toReturn;
	HFPage bucket;
	
	//go through list to find a page with enough space
	while (targetNode.getNext() != null && targetNode.getNext().getFreeSpace() < record.length + SLOT_SIZE) {
		targetNode = targetNode.getNext();
	}
	if (targetNode.getNext() == null) {
		//**/System.out.println("adding page!");
		//free list doesn't have a page for it, allocate a new page
		newHFPage = new HFPage();
		newPageId = Minibase.DiskManager.allocate_page();
		Minibase.DiskManager.read_page(newPageId, newHFPage);
		
		newHFPage.setCurPage(newPageId);
		//newHFPage.print();
		toReturn = newHFPage.insertRecord(record);
		//newHFPage.print();
		
		//add to no space list if page is full
		if (newHFPage.getFreeSpace() <= SLOT_SIZE) {
			noSpaceIter = header.getPrev();
			if (noSpaceIter == null) {
				newHFNode = new HFNode(newHFPage, header, null);
				header.setPrev(newHFNode);
				return toReturn;
			}
			while (noSpaceIter.getNext() != null) 
				noSpaceIter = noSpaceIter.getNext();
			newHFNode = new HFNode(newHFPage, noSpaceIter, null);
			noSpaceIter.setNext(newHFNode);
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

	//update one of the pages in the free list
	targetNode = targetNode.getNext();
	toReturn = targetNode.getPage().insertRecord(record);
	if (targetNode.getFreeSpace() == 0) {
		//remove from free list, add to no space list
		if (targetNode.getNext() != null) targetNode.getNext().setPrev(targetNode.getPrev());
		targetNode.getPrev().setNext(targetNode.getNext());
		noSpaceIter = header.getPrev();
		if (noSpaceIter == null) {
			targetNode.setPrev(header);
			targetNode.setNext(null);
			header.setPrev(targetNode);
			recCount++;
			return toReturn;
		}

		while (noSpaceIter.getNext() != null) 
			noSpaceIter = noSpaceIter.getNext();
		targetNode.setPrev(noSpaceIter);
		targetNode.setNext(null);
		noSpaceIter.setNext(targetNode);
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
	//uncomment this to get the actual data in the pages
	/*RID id = header.getNext().getPage().firstRecord();
	for (int i = 0; i < 3; i++) {
		for (int j = 0; j < 4; j++) {
			System.out.print(header.getNext().getPage().selectRecord(id)[j]);
		}
		System.out.print(" ");
		id = header.getNext().getPage().nextRecord(id);
	}
	System.out.println();
	*/
	//printHeap();
	return new HeapScan(this);
    }

    public void printHeap() {
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
    }
}
