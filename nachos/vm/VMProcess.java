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
		// if (numPages > Machine.processor().getNumPhysPages()) {
		// 	coff.close();
		// 	Lib.debug(dbgProcess, "\tinsufficient physical memory");
		// 	return false;
		// }


		pageTable = new TranslationEntry[numPages];
		//set all translation entry to invalid
		for (int i = 0; i < pageTable.length; i++) {
			//pageTable[i] = new TranslationEntry(i, UserKernel.getNextPage(), false, false, false, false);
			pageTable[i] = new TranslationEntry(i, 0, false, false, false, false);

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
			//System.out.println("exception cause: " + cause);
			int result = handlePageFault(processor.readRegister(Processor.regBadVAddr));
			//processor.writeRegister(Processor.regV0, result);
			break;
		default:
			//System.out.println("exception cause: " + cause);
			super.handleException(cause);
			break;
		}
	}

	private int handlePageFault(int vaddr) {
		
		int vpn = Processor.pageFromAddress(vaddr);
		//see if translage is good
		//Lib.assertTrue(vpn == vvpn);
		if (vpn >= numPages) {
					return -1;
		}
		TranslationEntry entry = pageTable[vpn];
		
		entry.valid = true;
		entry.ppn = VMKernel.getNextPage();
		//System.out.print("..........handlePageFault!!!!!   " + entry.vpn + "..........");
		//entry.vpn = vpn;
		// if is argument;
		// if (vpn == numPages - 1) {

		// }
		//System.out.println("num pages is ........." + numPages);
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			Lib.debug(dbgProcess, "\tinitializing " + section.getName()
					+ " section (" + section.getLength() + " pages)");
			for (int i = 0; i < section.getLength(); i++) {
				int secVpn = section.getFirstVPN() + i;
				if (secVpn == vpn) {
					System.out.println("section  " + secVpn);
					entry.readOnly = section.isReadOnly();
					section.loadPage(i, entry.ppn);
					return 1;
				}			
			}
		}
		System.out.println("flush memory.........");
		entry.readOnly = false;
		flushMemory(entry.ppn);
		return 1;
	}




	public void flushMemory(int ppn) {
		int startAdd = Processor.makeAddress(ppn, 0);
		byte[] mem = Machine.processor().getMemory();
		for (int i = startAdd; i < startAdd + pageSize; i++) {
			mem[i] = 0;
		}
	}

	public int readVirtualMemory(int vaddr, byte[] data) {
		return readVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from this process's virtual memory to the specified array.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr the first byte of virtual memory to read.
	 * @param data the array where the data will be stored.
	 * @param offset the first byte to write in the array.
	 * @param length the number of bytes to transfer from virtual memory to the
	 * array.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);
		
		byte[] memory = Machine.processor().getMemory();

		
		int remain = length;
		int j = 0;
		while(remain > 0 ) {
			int vpn = vaddr / Processor.pageSize;
			if(vpn >= numPages) {
				System.out.println("fuck you mom");
				return length - remain;
			}
			TranslationEntry entry = pageTable[vpn];
			if(!entry.valid) {
				if(handlePageFault(vaddr) != 1) {
					System.out.println("encounter handlePageFault error when readVirtualMemory............");
					return -1;
				}
			}
			
			int ptr = vaddr - (vaddr / Processor.pageSize) * Processor.pageSize;
			int ppn = pageTable[vpn].ppn;
			for(int i = 0; i + ptr < Processor.pageSize && remain > 0; i++) {
				data[j + offset] = memory[ppn*Processor.pageSize + i + ptr];
				remain--;
				vaddr++;
				j++;
			}
		}
		return length - remain;
	}

	public int writeVirtualMemory(int vaddr, byte[] data) {
		//System.out.println("writing virtual mem..................");
		return writeVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from the specified array to this process's virtual memory.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr the first byte of virtual memory to write.
	 * @param data the array containing the data to transfer.
	 * @param offset the first byte to transfer from the array.
	 * @param length the number of bytes to transfer from the array to virtual
	 * memory.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

		
		System.out.println("writing ppn..................");
		//added
		int remain = length;
		int j = 0;
		System.out.println(" begin to write virtual ");
		while(remain > 0 ) {
			System.out.println("writed " + (length - remain) + " bytes ");
			int vpn = vaddr / Processor.pageSize;
			if(vpn >= numPages || pageTable[vpn].readOnly) {
				System.out.println("fuck you mom");
				return length - remain;
			}
			TranslationEntry entry = pageTable[vpn];
			if(!entry.valid) {

				if(handlePageFault(vaddr) != 1) {
					System.out.println("encounter handlePageFault error when readVirtualMemory............");
					return -1;
				}
			}
			int ptr = vaddr - (vaddr / Processor.pageSize) * Processor.pageSize;
			
			int ppn = pageTable[vpn].ppn;

			for(int i = 0; i + ptr < Processor.pageSize && remain > 0; i++) {
				memory[ppn*Processor.pageSize + i + ptr] = data[j + offset];
				remain--;
				vaddr++;
				j++;
			}
		}
		
		return length - remain;
	}


	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';

	private static final char dbgVM = 'v';
}
