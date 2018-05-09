# cse120-proj

Initial repo for cse120 project 1-3!

## Project 2

### Part1: Implement file system call
#### Design
  UserProcess Class:
 * Add FileTable array to record opened file of each process.
 * Add FileReadPos/FileWritePoss array to record next read/write position of each file
 * Add FileUnlink to record unlinked file
 * Modify handleSyscall(), add handler for handleRead(), handleOpen(), handleWrite(),handleClose(), handleUnlink()
 * Add handleRead(), handleOpen(), handleWrite(),handleClose(), handleUnlink(), for each method, we do comprehensive check for corner cases
 
#### Test
 * Test create() on a new file to see if itworks
 * Test read()/write() a file to see if it works
 * Test read()/write() stdin/stdout to see if it works
 * Test unlink() to see if it works
 * Test open()/create() after calling unlink()
 * Test open()/close() on file
 * Test max num of file a process can open
 * Test large bytes of read/write
 
### Part2: Implement virtual memory support
#### Design
UserKernal Class:
 *  Add a linklist(PageList) to record free physical page number
 *  Add getNextPage()/ addNextPage() to allocate or free physical page.Use semaphore to perform synchronization when use PageList.

UserProcess Class:
 * Add a PageTable to record vpn/ppn translation entry
 * Modify UserProcess.loadSections() to initialize PageTable.
 * Modify readVirtualMemory()/writeVirtualMemory(), translate virtual address to physical address when read/write virtual memory
 * Modify UserProcess.unloadSections() to free physical pages when finish the process.

#### Test
 * Test Part2 using the same test cases as part1, to see if it works
 
### Part3: Implement virtual memory support
#### Design
UserKernal Class:
 
 * Add a HashMap(processMap) to record pids and according processes in system.
 * Add UserKernal.nextPid() to allocate a pid numer.
 * Add UserKernal.addProcess() to register a process to processMap, use semaphore to perform synchronization
 * Add UserKernal.getProcessById() to get process by pid
 * Add UserKernal.remove() to remove a process from current system when process finish()
 
 
UserProcess Class:
 * Add a LinkedList(childList) to record all child process of a process
 * Add a int filed to record parent pid number
 * Modify handleSyscall(), add handler for handleExec(), handleJoin()
 * Add handleExec(), handleJoin() to support join() end exec()
 
#### Test
 * Runs exec multiple times and checks each child gets unique PID
 * Tests join on a child process
 * Tests exit syscall releases all resources

### Contribution
 * Shengyuan Lin: Collaborate with Zhenghong to finish Part1 and Part2, implemented and fixed writeVirtualMemory and readVirtualMemory, fixed major bugs in part 3.
 * Zhenghong You. Finish Part 1, Add concurrency support to Virtual memory part and some other contribution on problem Implement handleJoin() handleExec() for part 3 and fix some bugs.