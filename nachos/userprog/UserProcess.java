package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

import java.io.EOFException;
import java.util.*;
import java.nio.ByteBuffer;
/**
 * Encapsulates the state of a user process that is not contained in its user
 * thread (or threads). This includes its address translation state, a file
 * table, and information about the program being executed.
 * 
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 * 
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess {
	/**
	 * Allocate a new process.
	 */
	public UserProcess() {
		// UserProcess.cnt++;
		// System.out.println("created " + cnt + " processes");
		// initialize stdin stdout fd
		
		for(int i = 0; i < fileTable.length; i++) {
			fileTable[i] = null;
			fileWritePos[i] = 0;
			fileReadPos[i] = 0;
		}
		this.pid = UserKernel.nextPid();
		UserKernel.addProcess(this.pid, this);
		//System.out.println("created process " + this.pid);


		fileTable[0] = UserKernel.console.openForReading();
		fileTable[1] = UserKernel.console.openForWriting();
		int numPhysPages = Machine.processor().getNumPhysPages();
		
////		//initialize pageTable, vpn = ppn
//		pageTable = new TranslationEntry[numPhysPages];
//		//set virtual address = physical address
//		for (int i = 0; i < numPhysPages; i++)
//			pageTable[i] = new TranslationEntry(i, i, true, false, false, false);
//		
	}

	/**
	 * Allocate and return a new process of the correct class. The class name is
	 * specified by the <tt>nachos.conf</tt> key
	 * <tt>Kernel.processClassName</tt>.
	 * 
	 * @return a new process of the correct class.
	 */
	public static UserProcess newUserProcess() {
	        String name = Machine.getProcessClassName ();

		// If Lib.constructObject is used, it quickly runs out
		// of file descriptors and throws an exception in
		// createClassLoader.  Handleck around it by hard-coding
		// creating new processes of the appropriate type.



		if (name.equals ("nachos.userprog.UserProcess")) {
		    UserProcess newProcess =  new UserProcess ();   
		    return newProcess;

		} else if (name.equals ("nachos.vm.VMProcess")) {
		    return new VMProcess ();
		} else {
		    return (UserProcess) Lib.constructObject(Machine.getProcessClassName());
		}
	}

	/**
	 * Execute the specified program with the specified arguments. Attempts to
	 * load the program, and then forks a thread to run it.
	 * 
	 * @param name the name of the file containing the executable.
	 * @param args the arguments to pass to the executable.
	 * @return <tt>true</tt> if the program was successfully executed.
	 */
	public boolean execute(String name, String[] args) {
		if (!load(name, args)){
			return false;
		}

		currentThread = new UThread(this);
		currentThread.setName(name).fork();
		return true;
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		Machine.processor().setPageTable(pageTable);
	}

	/**
	 * Read a null-terminated string from this process's virtual memory. Read at
	 * most <tt>maxLength + 1</tt> bytes from the specified address, search for
	 * the null terminator, and convert it to a <tt>java.lang.String</tt>,
	 * without including the null terminator. If no null terminator is found,
	 * returns <tt>null</tt>.
	 * 
	 * @param vaddr the starting virtual address of the null-terminated string.
	 * @param maxLength the maximum number of characters in the string, not
	 * including the null terminator.
	 * @return the string read, or <tt>null</tt> if no null terminator was
	 * found.
	 */
	public String readVirtualMemoryString(int vaddr, int maxLength) {
		Lib.assertTrue(maxLength >= 0);

		byte[] bytes = new byte[maxLength + 1];

		int bytesRead = readVirtualMemory(vaddr, bytes);

		for (int length = 0; length < bytesRead; length++) {
			if (bytes[length] == 0)
				return new String(bytes, 0, length);
		}

		return null;
	}

	/**
	 * Transfer data from this process's virtual memory to all of the specified
	 * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr the first byte of virtual memory to read.
	 * @param data the array where the data will be stored.
	 * @return the number of bytes successfully transferred.
	 */
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

		// for now, just assume that virtual addresses equal physical addresses
		if (vaddr < 0 || vaddr >= memory.length)
			return 0;
		
		//added
		int remain = length;
		int j = 0;
		while(remain > 0 ) {
			int vpn = vaddr / Processor.pageSize;
			if(vpn >= numPages) {
				return length - remain;
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

	/**
	 * Transfer all data from the specified array to this process's virtual
	 * memory. Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr the first byte of virtual memory to write.
	 * @param data the array containing the data to transfer.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data) {
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

		// for now, just assume that virtual addresses equal physical addresses
		if (vaddr < 0 || vaddr >= memory.length)
			return 0;
		
		//added
		int remain = length;
		int j = 0;
		while(remain > 0 ) {
			int vpn = vaddr / Processor.pageSize;
			int ptr = vaddr - (vaddr / Processor.pageSize) * Processor.pageSize;
			if(vpn >= numPages) {
				return length - remain;
			}
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

	/**
	 * Load the executable with the specified name into this process, and
	 * prepare to pass it the specified arguments. Opens the executable, reads
	 * its header information, and copies sections and arguments into this
	 * process's virtual memory.
	 * 
	 * @param name the name of the file containing the executable.
	 * @param args the arguments to pass to the executable.
	 * @return <tt>true</tt> if the executable was successfully loaded.
	 */
	private boolean load(String name, String[] args) {
		Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");

		OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
		if (executable == null) {
			Lib.debug(dbgProcess, "\topen failed");
			return false;
		}

		try {
			coff = new Coff(executable);
		}
		catch (EOFException e) {
			executable.close();
			Lib.debug(dbgProcess, "\tcoff load failed");
			return false;
		}

		// make sure the sections are contiguous and start at page 0
		numPages = 0;
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			if (section.getFirstVPN() != numPages) {
				coff.close();
				Lib.debug(dbgProcess, "\tfragmented executable");
				return false;
			}
			numPages += section.getLength();
		}
			
		// make sure the argv array will fit in one page
		byte[][] argv = new byte[args.length][];
		int argsSize = 0;
		for (int i = 0; i < args.length; i++) {
			argv[i] = args[i].getBytes();
			// 4 bytes for argv[] pointer; then string plus one for null byte
			argsSize += 4 + argv[i].length + 1;
		}
		if (argsSize > pageSize) {
			coff.close();
			Lib.debug(dbgProcess, "\targuments too long");
			return false;
		}

		// program counter initially points at the program entry point
		initialPC = coff.getEntryPoint();

		// next comes the stack; stack pointer initially points to top of it
		numPages += stackPages;
		initialSP = numPages * pageSize;

		// and finally reserve 1 page for arguments
		numPages++;

		if (!loadSections())
			return false;

		// store arguments in last page
		int entryOffset = (numPages - 1) * pageSize;
		int stringOffset = entryOffset + args.length * 4;

		this.argc = args.length;
		this.argv = entryOffset;

		for (int i = 0; i < argv.length; i++) {
			byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
			Lib.assertTrue(writeVirtualMemory(entryOffset, stringOffsetBytes) == 4);
			entryOffset += 4;
			Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) == argv[i].length);
			stringOffset += argv[i].length;
			Lib.assertTrue(writeVirtualMemory(stringOffset, new byte[] { 0 }) == 1);
			stringOffset += 1;
		}
		
		return true;
	}

	/**
	 * Allocates memory for this process, and loads the COFF sections into
	 * memory. If this returns successfully, the process will definitely be run
	 * (this is the last step in process initialization that can fail).
	 * 
	 * @return <tt>true</tt> if the sections were successfully loaded.
	 */
	protected boolean loadSections() {
		if (numPages > Machine.processor().getNumPhysPages()) {
			coff.close();
			Lib.debug(dbgProcess, "\tinsufficient physical memory");
			return false;
		}

		/*
			initialize pageTable.
		*/
		pageTable = new TranslationEntry[numPages];
		int numSec = 0;
		//System.out.println(numPages);
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);

			Lib.debug(dbgProcess, "\tinitializing " + section.getName()
					+ " section (" + section.getLength() + " pages)");

			for (int i = 0; i < section.getLength(); i++) {
				numSec++;
				int vpn = section.getFirstVPN() + i;
				pageTable[vpn] = new TranslationEntry(vpn, UserKernel.getNextPage(), true, section.isReadOnly(), false, false);
				// for now, just assume virtual addresses=physical addresses
				TranslationEntry Trans = pageTable[vpn];
				int ppn = Trans.ppn;
				section.loadPage(i, ppn);
			}
		}
		System.out.println(numSec);
		for(int i = 0; i + numSec < numPages; i++) {
			pageTable[i + numSec] = new TranslationEntry(i + numSec, UserKernel.getNextPage(), true, false, false, false);
		}
		return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
//		release those pages to Userkernel
		for (int i = 0; i < numPages; i++){
			UserKernel.addFreePage(pageTable[i].ppn);
		}
	}

	/**
	 * Initialize the processor's registers in preparation for running the
	 * program loaded into this process. Set the PC register to point at the
	 * start function, set the stack pointer register to point at the top of the
	 * stack, set the A0 and A1 registers to argc and argv, respectively, and
	 * initialize all other registers to 0.
	 */
	public void initRegisters() {
		Processor processor = Machine.processor();

		// by default, everything's 0
		for (int i = 0; i < processor.numUserRegisters; i++)
			processor.writeRegister(i, 0);

		// initialize PC and SP according
		processor.writeRegister(Processor.regPC, initialPC);
		processor.writeRegister(Processor.regSP, initialSP);

		// initialize the first two argument registers to argc and argv
		processor.writeRegister(Processor.regA0, argc);
		processor.writeRegister(Processor.regA1, argv);
	}

	/**
	 * Handle the halt() system call.
	 */
	private int handleHalt() {
		if(this.pid != UserKernel.rootProcess.pid)
			return -1;
		Machine.halt();

		Lib.assertNotReached("Machine.halt() did not halt machine!");
		return 0;
	}

	

	private static final int syscallHalt = 0, syscallExit = 1, syscallExec = 2,
			syscallJoin = 3, syscallCreate = 4, syscallOpen = 5,
			syscallRead = 6, syscallWrite = 7, syscallClose = 8,
			syscallUnlink = 9;

	/*
		Handle the open() system call
	*/
	private int handleOpen(int a0) {

		int nextPos = nextAvailable();
		if (nextPos == -1) {
			return -1;
		}
		//if invalid string
		String path = readVirtualMemoryString(a0, 256);
		if(path == null) {
			return -1;
		}
		if(fileUnlinkHold.contains(path)) {
			return -1;
		}
		//if open fail
		OpenFile fd = ThreadedKernel.fileSystem.open(path, false);
		if(fd == null) {
			return -1;
		}
		
		// add to fileTable
		fileTable[nextPos] = fd;
		System.out.println(nextPos);
		return nextPos;
	}

	/*
		Handle the close() system call
	*/
	private int handleClose(int a0) {
		if(a0 >= 16 || a0 < 0) {
			return -1;
		}
		OpenFile f = fileTable[a0];
		if(f == null) {
			return -1;
		}

		f.close();
		fileTable[a0] = null;
		return 0;
	}
	/*
		Handle the write() system call
	*/
	private int handleWrite(int a0, int a1, int a2) {
		if(a0 >= 16 || a0 < 0 || a2 < 0) {
			return -1;
		}
		OpenFile f = fileTable[a0];
		if(f == null) {
			return -1;
		}
		int memoryLength = Machine.processor().getMemory().length;
		if (a1 + a2 >= memoryLength) {
			//System.out.println("invalid buffer:");
			return -1;
		}

		byte[] localBuf = new byte[a2];
		
		readVirtualMemory(a1, localBuf);
		
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
	/*
		Handle the create() system call
	*/
	private int handleCreat(int a0) {
		
		
		int nextPos = nextAvailable();
		if(nextPos == -1) {
			return -1;
		}
		String path = readVirtualMemoryString(a0, 256);
		if(path == null) {
			return -1;
		}
		if(fileUnlinkHold.contains(path)) {
			return -1;
		}
		OpenFile fd = ThreadedKernel.fileSystem.open(path, false);
		if(fd == null) {
			fd = ThreadedKernel.fileSystem.open(path, true);
			if(fd == null) {
				return -1;
			}
		}
		// add to fileTable
		fileTable[nextPos] = fd;
		System.out.println(nextPos);
		return nextPos;
	}

	private int handleUnlink(int a0) {
		String path = readVirtualMemoryString(a0, 256);
		if(path == null) {
			return -1;
		}
		fileUnlinkHold.add(path);
		boolean status = ThreadedKernel.fileSystem.remove(path);
		fileUnlinkHold.remove(path);
		return status == true ? 0 : -1;
	}

	private int handleRead(int fd, int buffer, int count) {
		if(fd >= 16 || fd < 0) {
			return -1;
		}
		OpenFile f = fileTable[fd];
		if(f == null) {
			return -1;
		}

		
		byte[] localBuf = new byte[count];
		// char c;
		// SynchConsole console = new SynchConsole(Machine.console());
		// do {
		// 	c = (char) console.readByte(true);
		// 	console.writeByte(c);
		// } while (c != 'q');

		// System.out.println("");

		// f.read(localBuf, 0, 1000);

		if(fd == 0 || fd ==1) {
			int nBytes;
			int pos = 0;
			do {
				nBytes = f.read(localBuf, pos, count);
				count -= nBytes;
				pos += nBytes;
			}while(count>0 && nBytes != -1);

			writeVirtualMemory(buffer, localBuf);
			return pos;
		}
		else {
			int pos = fileReadPos[fd];
			//write(int pos, byte[] buf, int offset, int length)
			int flag = f.read(pos, localBuf, 0, localBuf.length);
			if(flag == -1) {
				return flag;
			}
			else {
				writeVirtualMemory(buffer, localBuf);
				fileReadPos[fd] += flag;
				return flag;
			}
		}
	}
	private int handleExec(int fName, int argc, int argv) {
		//System.out.println("fuck");
		if(argc < 0) {
			return -1;
		}
		String path = readVirtualMemoryString(fName, 256);
		if(path == null) {
			return -1;
		}
		int len = path.length();
		String lastFive = path.substring(len-5);
		if(!lastFive.equals(".coff")) {
			return -1;
		}
		String[] args = new String[argc];
		for(int i = 0; i < argc; i++) {
			args[i] = readVirtualMemoryString(argv+i, 256);
		}
		
		UserProcess newProcess = UserProcess.newUserProcess();
		newProcess.setParent(UserKernel.currentProcess().getPid());
		int cPid = newProcess.getPid();
		addChild(cPid);
		if(!newProcess.execute(path, args)) {
			//System.out.println("omg");
			return -1;
		}

		return cPid;
	}
	/**
	 * Handle the exit() system call.
	 */
	private void handleExit(int status) {
		Machine.autoGrader().finishingCurrentProcess(status);
		exitStatus = status;

		// UserProcess currentProcess = UserKernel.currentProcess();
		// if(currentProcess != null)
		// 	currentProcess.closeAllFd();
		closeAllFd();
		unloadSections();
		

		
//		System.out.println("exitStatus is " + status);
//		//
		if(UserKernel.checkIfOnlyOneElement()){
			UserKernel.kernel.terminate();
		}
		KThread.finish();
	}
	
	/**
	 * Handle the join() system call.
	 */
	private int handleJoin(int pid, int status){
        if (pid < 0 || status< 0){
            return -1;
        }
		//get the process to be joined
		UserProcess joinProcess;
		if(UserKernel.contain(pid)){
			joinProcess = UserKernel.getProcessByPid(pid);
		}
		else{
			return -1;
		}
		//get current UserProcess 
		UserProcess currentProcess = UserKernel.currentProcess();
		//a process should be able to join only it's child process
		/*
			error!!!!
		*/

		if (joinProcess.getParentPid()!=currentProcess.getPid())
			return -1;

		joinProcess.currentThread.join();
		UserKernel.remove(pid);

		if(joinProcess.exitStatus != null){
			byte exit[] = new byte[4];
			exit = Lib.bytesFromInt(joinProcess.exitStatus);
			int write = writeVirtualMemory(status,exit);
			//if write successfully
			if(write == 4){
				return 1;
			}
				return 0;
		}
		return 0;
	}
	

	

	/**
	 * Handle a syscall exception. Called by <tt>handleException()</tt>. The
	 * <i>syscall</i> argument identifies which syscall the user executed:
	 * 
	 * <table>
	 * <tr>
	 * <td>syscall#</td>
	 * <td>syscall prototype</td>
	 * </tr>
	 * <tr>
	 * <td>0</td>
	 * <td><tt>void halt();</tt></td>
	 * </tr>
	 * <tr>
	 * <td>1</td>
	 * <td><tt>void exit(int status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>2</td>
	 * <td><tt>int  exec(char *name, int argc, char **argv);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>3</td>
	 * <td><tt>int  join(int pid, int *status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>4</td>
	 * <td><tt>int  creat(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>5</td>
	 * <td><tt>int  open(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>6</td>
	 * <td><tt>int  read(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>7</td>
	 * <td><tt>int  write(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>8</td>
	 * <td><tt>int  close(int fd);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>9</td>
	 * <td><tt>int  unlink(char *name);</tt></td>
	 * </tr>
	 * </table>
	 * 
	 * @param syscall the syscall number.
	 * @param a0 the first syscall argument.
	 * @param a1 the second syscall argument.
	 * @param a2 the third syscall argument.
	 * @param a3 the fourth syscall argument.
	 * @return the value to be returned to the user.
	 */
	public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
		switch (syscall) {
		case syscallHalt:
			return handleHalt();
		case syscallExit:
			handleExit(a0);
		case syscallOpen:
			return handleOpen(a0);
		case syscallClose:
			return handleClose(a0);
		case syscallWrite:
			return handleWrite(a0, a1, a2);
		case syscallCreate:
			return handleCreat(a0);
		case syscallUnlink:
			return handleUnlink(a0);
		case syscallRead:
			return handleRead(a0, a1, a2);
		case syscallExec:
			return handleExec(a0, a1, a2);
		case syscallJoin:
			return handleJoin(a0,a1);
		default:
			Lib.debug(dbgProcess, "Unknown syscall " + syscall);
			Lib.assertNotReached("Unknown system call!");
		}
		return 0;
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
		//System.out.println(cause);
		switch (cause) {
		case Processor.exceptionSyscall:
			int result = handleSyscall(processor.readRegister(Processor.regV0),
					processor.readRegister(Processor.regA0),
					processor.readRegister(Processor.regA1),
					processor.readRegister(Processor.regA2),
					processor.readRegister(Processor.regA3));
			processor.writeRegister(Processor.regV0, result);
			processor.advancePC();

			break;

		default:
			Lib.debug(dbgProcess, "Unexpected exception: "
					+ Processor.exceptionNames[cause]);
			Lib.assertNotReached("Unexpected exception");
		}
	}

	/*
		get next available filetable, return -1 if it is full
	*/
	private int nextAvailable() {
		for(int i = 0; i < fileTable.length; i++) {
			if(fileTable[i] == null) {
				return i;
			}
		}
		return -1;
	}

	private void closeAllFd() {
		for(OpenFile f : fileTable) {
			if(f != null)
				f.close();
		}
	}

	/** The program being run by this process. */
	protected Coff coff;

	/** This process's page table. */
	protected TranslationEntry[] pageTable;

	/** The number of contiguous pages occupied by the program. */
	protected int numPages;

	/** The number of pages in the program's stack. */
	protected final int stackPages = 8;

	private int initialPC, initialSP;

	private int argc, argv;

	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';

	private OpenFile[] fileTable = new OpenFile[16];
	private int[] fileReadPos = new int[16];
	private int[] fileWritePos = new int[16];
	private HashSet<String> fileUnlinkHold = new HashSet<String>();

	private final int pid;
	private ArrayList<Integer> childrens = new ArrayList<Integer>();
	private int parrentPid = -1;


	public int getPid() {
		return pid;
	}

	public int getParentPid() {
		return parrentPid;
	}

	public void setParent(int parrentPid) {
		this.parrentPid = parrentPid;
	}

	public void addChild(int pid) {
		childrens.add(pid);
	}

	private static int cnt = 0;
	private UThread currentThread;
	private Integer exitStatus = null;
}
