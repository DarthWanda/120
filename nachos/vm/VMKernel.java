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
		clockLock = new Lock();
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
		public Integer pid;
		public TranslationEntry transEntry;
		public int pinCount;

		public invertedPageTableEntry(Integer p, TranslationEntry t, int cnt) {
			this.pid = p;
			this.transEntry = t;
			this.pinCount = pinCount;
		}

	}

	protected static void swapIn()
	protected static void swapOut(int ppn) {
		System.out.println("swap out dirty page" + ppn);
	}

	public static int getNextPage() {
		lock.acquire();
		int nextPage = -1;
		if (!pageList.isEmpty()) {
			nextPage = pageList.removeLast();

		} else {	
			int ppn = clock();
			nextPage = ppn;
			//System.out.println("clock returns ppn#" + ppn);
			invertedPageTableEntry physEntry = invertedPageTable[ppn];
			if (physEntry.transEntry == null) {
				System.out.println("omg fuck your mom");
			}
			if (physEntry.transEntry.dirty) {
				swapOut(ppn);
			}
			physEntry.transEntry.valid = false;
			//System.out.println("not sufficcient page, require swap");
		}
        
		lock.release();
		return nextPage;
	}
	public static void fillInvertedEntry(int ppn,int p, TranslationEntry t) {
		lock.acquire();
		invertedPageTableEntry it = invertedPageTable[ppn];
		it.pid = p;
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
		clockLock.acquire();
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
		clockLock.release();
		return res;
	}

	// public static VMProcess currentProcess() {
	// 	if (!(KThread.currentThread() instanceof UThread))
	// 		return null;

	// 	return ((UThread) KThread.currentThread()).process;
	// }



	private static Lock clockLock;
	private static Lock lock;
	private static invertedPageTableEntry[] invertedPageTable;
	private static int[] frame;
	private static boolean[] usedFlag;
	// dummy variables to make javac smarter
	private static VMProcess dummy1 = null;
	private static final char dbgVM = 'v';
}
