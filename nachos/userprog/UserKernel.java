	package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import java.util.*;
/**
 * A kernel that can support multiple user processes.
 */
public class UserKernel extends ThreadedKernel {
	/**
	 * Allocate a new user kernel.
	 */
	public UserKernel() {
		super();

	}

	/**
	 * Initialize this kernel. Creates a synchronized console and sets the
	 * processor's exception handler.
	 */
	public void initialize(String[] args) {
		super.initialize(args);
		mapLock = new Semaphore(1);
		pageLock = new Semaphore(1);
		console = new SynchConsole(Machine.console());
		int numPhysPages = Machine.processor().getNumPhysPages();

		//initialize pageList
		for(int i = 0; i < numPhysPages; i++) {
			pageList.add(i);
		}


		Machine.processor().setExceptionHandler(new Runnable() {
			public void run() {
				exceptionHandler();
			}
		});
	}

	/**
	 * Test the console device.
	 */
	public void selfTest() {
		// super.selfTest();

		// System.out.println("Testing the console device. Typed characters");
		// System.out.println("will be echoed until q is typed.");

		// char c;

		// do {
		// 	c = (char) console.readByte(true);
		// 	console.writeByte(c);
		// } while (c != 'q');

		// System.out.println("");
	}

	/**
	 * Returns the current process.
	 * 
	 * @return the current process, or <tt>null</tt> if no process is current.
	 */
	public static UserProcess currentProcess() {
		if (!(KThread.currentThread() instanceof UThread))
			return null;

		return ((UThread) KThread.currentThread()).process;
	}

	/**
	 * The exception handler. This handler is called by the processor whenever a
	 * user instruction causes a processor exception.
	 * 
	 * <p>
	 * When the exception handler is invoked, interrupts are enabled, and the
	 * processor's cause register contains an integer identifying the cause of
	 * the exception (see the <tt>exceptionZZZ</tt> constants in the
	 * <tt>Processor</tt> class). If the exception involves a bad virtual
	 * address (e.g. page fault, TLB miss, read-only, bus error, or address
	 * error), the processor's BadVAddr register identifies the virtual address
	 * that caused the exception.
	 */
	public void exceptionHandler() {
		Lib.assertTrue(KThread.currentThread() instanceof UThread);

		UserProcess process = ((UThread) KThread.currentThread()).process;
		int cause = Machine.processor().readRegister(Processor.regCause);
		
		process.handleException(cause);
	}

	/**
	 * Start running user programs, by creating a process and running a shell
	 * program in it. The name of the shell program it must run is returned by
	 * <tt>Machine.getShellProgramName()</tt>.
	 * 
	 * @see nachos.machine.Machine#getShellProgramName
	 */
	public void run() {
		super.run();

		rootProcess = UserProcess.newUserProcess();

		String shellProgram = Machine.getShellProgramName();
		//System.out.println(shellProgram);
		Lib.assertTrue(rootProcess.execute(shellProgram, new String[] {}));

		KThread.currentThread().finish();
	}

	/**
	 * Terminate this kernel. Never returns.
	 */
	public void terminate() {
		super.terminate();
	}

	/** Globally accessible reference to the synchronized console. */
	public static SynchConsole console;

	// dummy variables to make javac smarter
	private static Coff dummy1 = null;
	private static HashMap<Integer, UserProcess> processMap = new HashMap<Integer, UserProcess>();

	public static int nextPid() {
		for(int i = 0;; i++) {	
			mapLock.P();
			if(!processMap.containsKey(i)) {
				processMap.put(i, null);
				mapLock.V();
				return i;
			}
			mapLock.V();
		}
	}
	public static UserProcess getProcessByPid(int pid) {
		mapLock.P();
		UserProcess process = processMap.get(pid);
		mapLock.V();
		return process;
	}

	public static void addProcess(int pid,UserProcess up) {
		mapLock.P();
		processMap.put(pid, up);
		mapLock.V();
	}

	public static void remove(Integer pid) {
		//System.out.println(lock);

		mapLock.P();
		if(processMap.containsKey(pid))
			processMap.remove(pid);
		mapLock.V();
	}

	public static boolean checkIfOnlyOneElement(){
		int remain = processMap.keySet().size();
		if (remain == 1) {
			return true;
		}
		else {
			return false;
		}
	}
	
	//check if processmap contain this process
	public static boolean contain(int pid){
		return processMap.containsKey(pid);
	}
	
	public static Semaphore mapLock;

	/*
	 *************************************************************************
		virtual memory support!!
	*/

	//
	public static LinkedList<Integer> pageList = new LinkedList<Integer>();

	public static Semaphore pageLock;
	
	public static UserProcess rootProcess;
	public static int getNextPage() {
		pageLock.P();
        int nextPage = pageList.removeLast();
		pageLock.V();
		return nextPage;
	}

	public static void addFreePage(int pageNum) {
		pageLock.P();
		pageList.add(pageNum);
		pageLock.V();
	}

}
