package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;
import nachos.vm.VMKernel.*;
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
		
		VMKernel.pageFaultLock.acquire();

		int vpn = Processor.pageFromAddress(vaddr);
		if (vpn >= numPages) {
			VMKernel.pageFaultLock.release();
			return -1;
		}

		TranslationEntry entry = pageTable[vpn];
		Lib.assertTrue(!entry.valid);

		int nextPPN = VMKernel.getNextPage();
		Lib.assertTrue(nextPPN < Machine.processor().getNumPhysPages());
		invertedPageTableEntry invertedEntry = VMKernel.getInvertedEntry(nextPPN);
		// check, this should be valid
		if (invertedEntry != null) {
			Lib.assertTrue(invertedEntry.transEntry.valid);
			loadToPPN(vpn, nextPPN);
		}
		else {
			invertedEntry = VMKernel.newInvertedEntry(nextPPN, this, pageTable[vpn], 0);
			loadToPPN(vpn, nextPPN);
		}

		VMKernel.pageFaultLock.release();
		return 1;
	}

	private void loadToPPN(int vpn, int ppn) {
		invertedPageTableEntry invertedEntry = VMKernel.getInvertedEntry(ppn);
		TranslationEntry entry = pageTable[vpn];

		// Lib.assertTrue(invertedEntry != null);
		// Lib.assertTrue(invertedEntry.transEntry.valid);
		// Lib.assertTrue(!entry.valid);

		invertedEntry.transEntry.valid = false;
		entry.valid = true;

		if (invertedEntry.transEntry.dirty) {
			Lib.assertTrue(!invertedEntry.transEntry.readOnly);
			VMKernel.swapOut(ppn);
		}

		if (entry.dirty) {
			VMKernel.swapIn(entry.ppn, ppn);	
		} else {
			int cntFlag = 0;
			for (int s = 0; s < coff.getNumSections(); s++) {
				CoffSection section = coff.getSection(s);
				Lib.debug(dbgProcess, "\tinitializing " + section.getName()
						+ " section (" + section.getLength() + " pages)");
				for (int i = 0; i < section.getLength(); i++) {
					int secVpn = section.getFirstVPN() + i;
					if (secVpn == vpn) {
						cntFlag++;
						entry.readOnly = section.isReadOnly();
						section.loadPage(i, ppn);
					}			
				}
			}
			Lib.assertTrue(cntFlag == 0 || cntFlag == 1);
			if (cntFlag == 0) {
				entry.readOnly = false;
				flushMemory(ppn);
			}
				
		}
		entry.ppn = ppn;
		VMKernel.fillInvertedEntry(ppn, this, entry);
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
				return length - remain;
			}
			TranslationEntry entry = pageTable[vpn];
			if(!entry.valid) {
				//System.out.println("encounter handlePageFault error when readVirtualMemory............");
				if(handlePageFault(vaddr) != 1) {
					
					return length - remain;
				}
			}
			//check if swap succussfully
			Lib.assertTrue(entry.valid);
			Lib.assertTrue(entry.vpn == vpn);
			Lib.assertTrue(entry.ppn < Machine.processor().getNumPhysPages());


			int ptr = vaddr - (vaddr / Processor.pageSize) * Processor.pageSize;
			int ppn = pageTable[vpn].ppn;
			VMKernel.pinLock.acquire();
			VMKernel.pinSet.add(ppn);
			VMKernel.pinLock.release();

			for(int i = 0; i + ptr < Processor.pageSize && remain > 0; i++) {
				data[j + offset] = memory[ppn*Processor.pageSize + i + ptr];
				remain--;
				vaddr++;
				j++;
			}
			VMKernel.pinLock.acquire();
			VMKernel.pinSet.remove(ppn);
			VMKernel.pinCVLock.acquire();
			VMKernel.pinCV.wake();
			VMKernel.pinCVLock.release();
			VMKernel.pinLock.release();
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
				return length - remain;
			}
			TranslationEntry entry = pageTable[vpn];
			if(!entry.valid) {
				//System.out.println("encounter handlePageFault error when writeVirtualMemory............");
				if(handlePageFault(vaddr) != 1) {
					
					return -1;
				}
			}

			//check if swap succussfully
			Lib.assertTrue(!entry.readOnly);
			Lib.assertTrue(entry.valid);
			Lib.assertTrue(entry.vpn == vpn);
			Lib.assertTrue(entry.ppn < Machine.processor().getNumPhysPages());
			Lib.assertTrue(entry.ppn != 10);


			int ptr = vaddr - (vaddr / Processor.pageSize) * Processor.pageSize;
			
			int ppn = pageTable[vpn].ppn;
			VMKernel.pinLock.acquire();
			entry.dirty = true;
			VMKernel.pinSet.add(ppn);
			VMKernel.pinLock.release();
			//System.out.println(ppn + " is dirty page");
			for(int i = 0; i + ptr < Processor.pageSize && remain > 0; i++) {
				memory[ppn*Processor.pageSize + i + ptr] = data[j + offset];
				remain--;
				vaddr++;
				j++;
			}
			VMKernel.pinLock.acquire();
			VMKernel.pinSet.remove(ppn);
			VMKernel.pinCVLock.acquire();
			VMKernel.pinCV.wake();
			VMKernel.pinCVLock.release();
			VMKernel.pinLock.release();
		}
		
		return length - remain;
	}

	/*
		override
	*/
	protected int handleWrite(int a0, int a1, int a2) {
		if(a0 >= 16 || a0 < 0 || a2 < 0) {
			return -1;
		}
		OpenFile f = fileTable[a0];
		if(f == null) {
			return -1;
		}
		int memoryLength = Machine.processor().getMemory().length;
		
		byte[] localBuf = new byte[a2];

		int vpn = Processor.pageFromAddress(a1 + a2);
		if (vpn >= numPages) {
			return -1;
		}

		if(readVirtualMemory(a1, localBuf) == -1) {
			return -1;
		}
		//System.out.println("handle writing...........");
		// stdin stdout;
		if(a0 == 0 || a0 ==1) {
			return f.write(localBuf, 0, a2);			
		}
		else {
			int pos = fileWritePos[a0];
			//write(int pos, byte[] buf, int offset, int length)
			int flag = f.write(pos, localBuf, 0, localBuf.length);
			if(flag == -1) {
				return flag;
			}
			else {
				fileWritePos[a0] += flag;
				return flag;
			}
		}
	}
	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';

	private static final char dbgVM = 'v';

	// because when encounter page fault, the ppn is useless, so use ppn as spn
	private int vpnToSpn(int vpn) {
		return pageTable[vpn].ppn;
	}






}
