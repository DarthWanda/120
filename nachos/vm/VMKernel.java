package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

/**
 * A kernel that can support multiple demand-paging user processes.
 */
public class VMKernel extends UserKernel {
	/**
	 * Allocate a new VM kernel.
	 */
	public VMKernel() {
		super();
		

	}

	/**
	 * Initialize this kernel.
	 */
	public void initialize(String[] args) {
		super.initialize(args);
		lock = new Lock();
		invertedPageTable = new invertedPageTableEntry[Machine.processor().getNumPhysPages()];
		usedFlag = new boolean[Machine.processor().getNumPhysPages()];
		for (int i = 0; i < usedFlag.length; i++) {
			usedFlag[i] = false;
			invertedPageTable[i] = new invertedPageTableEntry(null, null, 0);
		}
	}

	/**
	 * Test this kernel.
	 */
	public void selfTest() {
		super.selfTest();
	}

	/**
	 * Start running user programs.
	 */
	public void run() {
		super.run();
	}

	/**
	 * Terminate this kernel. Never returns.
	 */
	public void terminate() {
		super.terminate();
	}

	// add inverted page table;
	private class invertedPageTableEntry {
		public VMProcess process;
		public TranslationEntry transEntry;
		public int pinCount;

		public invertedPageTableEntry(VMProcess p, TranslationEntry t, int cnt) {
			this.process = p;
			this.transEntry = t;
			this.pinCount = pinCount;
		}

	}
	private static void swapOut(int ppn) {
		System.out.println("swapping out physical page#" + ppn);
	}

	public static int getNextPage() {
		pageLock.P();
		int nextPage = -1;
		if (!pageList.isEmpty()) {
			nextPage = pageList.removeLast();
		} else {
			
			int ppn = clock();
			System.out.println("clock returns ppn#" + ppn);
			invertedPageTableEntry physEntry = invertedPageTable[ppn];
			if (physEntry.transEntry.dirty) {
				swapOut(ppn);
			}
			physEntry.transEntry.valid = false;
			System.out.println("not sufficcient page, require swap");
		}
        
		pageLock.V();
		return nextPage;
	}
	public void fillInvertedEntry(invertedPageTableEntry it,VMProcess p, TranslationEntry t) {
		lock.acquire();
		it.process = p;
		it.transEntry = t;
		lock.release();
	}

	public void increasePinCnt (invertedPageTableEntry e) {
		lock.acquire();
		e.pinCount += 1;
		lock.release();
	}

	public invertedPageTableEntry getInvertedPageTableEntry(int ppn) {
		return invertedPageTable[ppn];
	}

	private static int clock() {
		lock.acquire();
		int res = 0;
		for (int i = 0; ; i = (i + 1) % Machine.processor().getNumPhysPages()) {
			if(usedFlag[i] == false) {
				usedFlag[i] = true;
				res = i;
				break;
			} else {
				usedFlag[i] = false;
			}
		}
		lock.acquire();
		return res;
	}

	private static Lock lock;
	private static invertedPageTableEntry[] invertedPageTable;
	private static int[] frame;
	private static boolean[] usedFlag;
	// dummy variables to make javac smarter
	private static VMProcess dummy1 = null;
	private static final char dbgVM = 'v';
}
