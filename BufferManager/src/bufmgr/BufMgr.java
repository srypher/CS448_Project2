package bufmgr;

import diskmgr.DiskMgr;
import diskmgr.DiskMgrException;
import diskmgr.DuplicateEntryException;
import diskmgr.FileEntryNotFoundException;
import diskmgr.FileIOException;
import diskmgr.FileNameTooLongException;
import diskmgr.InvalidPageNumberException;
import diskmgr.InvalidRunSizeException;
import diskmgr.OutOfSpaceException;


import hashtable.HashTable;
import hashtable.HashEntry;

import chainexception.ChainException;

import global.GlobalConst;
import global.PageId;
import global.Page;
/**/import global.Minibase;

public class BufMgr {

private Page[] bufferPool;
private Descriptor[] bufDescr;
private HashTable directory;
private boolean isFull;
private int numbufs;
private int bufferLoc;
private DiskMgr diskmgr;

/**
* Create the BufMgr object.
* Allocate pages (frames) for the buffer pool in main memory and
* make the buffer manage aware that the replacement policy is
* specified by replacerArg (e.g., LH, Clock, LRU, MRU, LFU, etc.).
*
* @param numbufs number of buffers in the buffer pool
* @param lookAheadSize: Please ignore this parameter
* @param replacementPolicy Name of the replacement policy, that parameter will be set to "LFU" (you
can safely ignore this parameter as you will implement only one policy)
*/
public BufMgr(int numbufs, int lookAheadSize, String replacementPolicy) {
	this.numbufs = numbufs;
	bufferLoc = 0;
	bufferPool = new Page[numbufs];
	bufDescr = new Descriptor[numbufs];
	directory = new HashTable(numbufs);
	isFull = false;
	diskmgr = Minibase.DiskManager;
}
/**
* Pin a page.
* First check if this page is already in the buffer pool.
* If it is, increment the pin_count and return a pointer to this
* page.
* If the pin_count was 0 before the call, the page was a
* replacement candidate, but is no longer a candidate.
* If the page is not in the pool, choose a frame (from the
* set of replacement candidates) to hold this page, read the
* page (using the appropriate method from {\em diskmgr} package) and pin it.
* Also, must write out the old page in chosen frame if it is dirty
* before reading new page.__ (You can assume that emptyPage==false for
* this assignment.)
*
* @param pageno page number in the Minibase.
* @param page the pointer point to the page.
* @param emptyPage true (empty page); false (non-empty page)
*/
public void pinPage(PageId pageno, Page page, boolean emptyPage) throws BufferPoolExceededException, ChainException, InvalidPageNumberException {
	if (pageno.pid < 0 || pageno.pid > numbufs) {
		throw new InvalidPageNumberException(null, "Invalid Page Number Exeption");
	}
	if(directory.get(pageno.pid) != -1) {
		bufDescr[directory.get(pageno.pid)].pinPage();
		page.setPage(bufferPool[directory.get(pageno.pid)]);
	} 
	else {
		//if buffer is full, find the replacement per LFU strategy
		//if the page to replace is dirty, flush it
		//then read the page in then put it in the bufferpool and bufDescr then set it's frame in the directory
		if(isFull) {
			if(getNumUnpinned() == 0) {
				throw new BufferPoolExceededException(null, "BUFMGR:NO_UNPINNED_FRAMES");
			}
			int replacement = findReplacement();
			if(bufDescr[replacement].getDirty()) {
				flushPage(bufDescr[replacement].getPagenumber());
			}
			try {
				diskmgr.read_page(pageno, page);
			}
			catch(Exception e) {
				throw new DiskMgrException(e, "DB.java:read_page() failed");
			}
			bufferPool[replacement] = page;
			bufDescr[replacement] = new Descriptor(pageno);
			bufDescr[replacement].pinPage();
			directory.set(pageno.pid, replacement);
		}
		//otherwise, just read in the page
		else {
			try {
				diskmgr.read_page(pageno, page);
			}
			catch(Exception e) {
				throw new DiskMgrException(e, "DB.java:read_page() failed");
			}
			bufferPool[bufferLoc] = page;
			bufDescr[bufferLoc] = new Descriptor(pageno);
			bufDescr[bufferLoc].pinPage();
			directory.put(pageno.pid, bufferLoc);
			bufferLoc++;
			if(bufferLoc == numbufs) {
				isFull = true;
			}
		}
	}
}
/**
* Unpin a page specified by a pageId.
* This method should be called with dirty==true if the client has
* modified the page.
* If so, this call should set the dirty bit
* for this frame.
* Further, if pin_count>0, this method should
* decrement it.
*If pin_count=0 before this call, throw an exception
* to report error.
*(For testing purposes, we ask you to throw
* an exception named PageUnpinnedException in case of error.)
*
* @param pageno page number in the Minibase.
* @param dirty the dirty bit of the frame
*/
public void unpinPage(PageId pageno, boolean dirty) throws PagePinnedException, HashEntryNotFoundException, ChainException{
	if(directory.get(pageno.pid) != -1) {
		if(dirty) {
			bufDescr[directory.get(pageno.pid)].toggleDirty();
		}
		if(bufDescr[directory.get(pageno.pid)].getPinCount() > 0) {
			bufDescr[directory.get(pageno.pid)].unpinPage();
		}
		else {
			throw new PagePinnedException(null, "BUFMGR:PAGE_NOT_PINNED");
		}
	}
	else {
		throw new HashEntryNotFoundException(null, "BUFMGR:PAGE_NOT_FOUND_IN_BUFFER_POOL");
	}
}
/**
* Allocate new pages.
* Call DB object to allocate a run of new pages and
* find a frame in the buffer pool for the first page
* and pin it. (This call allows a client of the Buffer Manager
* to allocate pages on disk.) If buffer is full, i.e., you
* can't find a frame for the first page, ask DB to deallocate
* all these pages, and return null.
*
* @param firstpage the address of the first page.
* @param howmany total number of allocated new pages.
*
* @return the first page id of the new pages.__ null, if error.
*/
public PageId newPage(Page firstpage, int howmany) throws ChainException {
	if(isFull || bufferLoc + howmany > numbufs) {
		return null;
	}
	else {
		PageId start_page_num = new PageId();
		try {
			diskmgr.allocate_page(start_page_num, howmany);
		}
		catch(Exception e) {
			throw new DiskMgrException(e, "DB.java:allocate_page() failed");
		}
		pinPage(start_page_num, firstpage, false);
		
		return start_page_num;
	}
}
/**
* This method should be called to delete a page that is on disk.
* This routine must call the method in diskmgr package to
* deallocate the page.
*
* @param globalPageId the page number in the data base.
*/
public void freePage(PageId globalPageId) throws ChainException {
	try {
		diskmgr.deallocate_page(globalPageId);
	}
	catch(Exception e) {
		throw new DiskMgrException(e, "DB.java:deallocate_page() failed");
	}
}
/**
* Used to flush a particular page of the buffer pool to disk.
* This method calls the write_page method of the diskmgr package.
*
* @param pageid the page number in the database.
*/
public void flushPage(PageId pageid) throws HashEntryNotFoundException, ChainException{
	if(directory.get(pageid.pid) != -1) {
		Page page = new Page();
		page = bufferPool[directory.get(pageid.pid)];
		try { 
			diskmgr.write_page(pageid, page);
		}
		catch(Exception e) {
			throw new DiskMgrException(e, "DB.java:write_page() failed");
		}
	}
	else {
		throw new HashEntryNotFoundException(null, "BUFMGR:PAGE_NOT_FOUND_IN_BUFFER_POOL");
	}
}
/**
* Used to flush all dirty pages in the buffer pool to disk
*
*/
public void flushAllPages() throws HashEntryNotFoundException, ChainException {
	for(int i = 0; i < numbufs; i++) {
		if(bufDescr[i] != null && bufDescr[i].getDirty()) {
			flushPage(bufDescr[i].getPagenumber());
		}
	}
}
/**
* Returns the total number of buffer frames.
*/
public int getNumBuffers() {
	return numbufs;
}
/**
* Returns the total number of unpinned buffer frames.
*/
public int getNumUnpinned() {
	int numUnPinned = 0;
	for(int i = 0; i < numbufs; i++) {
		if(bufDescr[i] != null && bufDescr[i].getPinCount() == 0) {
			numUnPinned++;
		}
	}
	return numUnPinned;
}

public int findReplacement() {
	int tempCounter = 100000;
	int tempIndex = -1;
	for(int i = 0; i < numbufs; i++) {
		if(bufDescr[i] != null && bufDescr[i].getPinCount() == 0) {
			if(tempCounter > bufDescr[i].getLFUCount()) {
				tempCounter = bufDescr[i].getLFUCount();
				tempIndex = i;
			}
		}
	}
	return tempIndex;
}
}
