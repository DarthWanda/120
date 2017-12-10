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
		invertedPageTable = new InvertedPageTableEntry[Machine.processor().getNumPhysPages()];
	}

	/**
	 * Initialize this kernel.
	 */
	public void initialize(String[] args) {
		super.initialize(args);
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
	private class InvertedPageTableEntry {
		VMProcess process;
		TranslationEntry transEntry;
		int pinCount;

		public InvertedPageTableEntry(VMProcess p, TranslationEntry t, int cnt) {
			this.process = p;
			this.transEntry = t;
			this.pinCount = pinCount;
		}

	}
	
	public static int getNextPage() {
		pageLock.P();
		int nextPage = -1;
		if (!pageList.isEmpty()) {
			nextPage = pageList.removeLast();
		} else {
			
			
			System.out.println("not sufficcient page, require swap");
		}
        
		pageLock.V();
		return nextPage;
	}


	private InvertedPageTableEntry[] invertedPageTable;
	private int[] frame;
	// dummy variables to make javac smarter
	private static VMProcess dummy1 = null;
	private static final char dbgVM = 'v';
}
