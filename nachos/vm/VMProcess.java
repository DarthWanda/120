package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
	/**
	 * Allocate a new process.
	 */
	public VMProcess() {
		super();
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
		super.saveState();
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		super.restoreState();
	}

	/**
	 * Initializes page tables for this process so that the executable can be
	 * demand-paged.
	 * 
	 * @return <tt>true</tt> if successful.
	 */
	protected boolean loadSections() {
		//return super.loadSections();
		if (numPages > Machine.processor().getNumPhysPages()) {
			coff.close();
			Lib.debug(dbgProcess, "\tinsufficient physical memory");
			return false;
		}


		pageTable = new TranslationEntry[numPages];
		//set all translation entry to invalid
		for (int i = 0; i < pageTable.length; i++) {
			pageTable[i] = new TranslationEntry(i, UserKernel.getNextPage(), false, false, false, false);

		}
		return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
		super.unloadSections();
	}

	/**
	 * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>
	 * . The <i>cause</i> argument identifies which exception occurred; see the
	 * <tt>Processor.exceptionZZZ</tt> constants.
	 * 
	 * @param cause the user exception that occurred.
	 */
	public void handleException(int cause) {
		Processor processor = Machine.processor();
		
		switch (cause) {
		case Processor.exceptionPageFault:
			System.out.println("exception cause: " + cause);
			int result = handlePageFault(processor.readRegister(Processor.regBadVAddr));
			processor.writeRegister(Processor.regV0, result);
			break;
		default:
			System.out.println("exception cause: " + cause);
			super.handleException(cause);
			break;
		}
	}

	private int handlePageFault(int vaddr) {
		int vpn = vaddr / Processor.pageSize;
		TranslationEntry entry = pageTable[vpn];
		if (vpn >= numPages) {
			return -1;
		}
		entry.valid = true;
		entry.vpn = vpn;
		// if is argument;
		// if (vpn == numPages - 1) {

		// }
		
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			Lib.debug(dbgProcess, "\tinitializing " + section.getName()
					+ " section (" + section.getLength() + " pages)");
			for (int i = 0; i < section.getLength(); i++) {
				int secVpn = section.getFirstVPN() + i;
				if (secVpn == vpn) {	
					entry.readOnly = section.isReadOnly();
					section.loadPage(i, entry.ppn);
					return 1;
				}			
			}
		}
		flushMemory(entry.ppn);
		return 0;
	}


	public void flushMemory(int ppn) {
		int startAdd = Processor.makeAddress(ppn, 0);
		byte[] mem = Machine.processor().getMemory();
		for (int i = startAdd; i < startAdd + pageSize; i++) {
			mem[i] = 0;
		}
	}


	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';

	private static final char dbgVM = 'v';
}
