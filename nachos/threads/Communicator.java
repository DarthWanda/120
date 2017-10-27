package nachos.threads;

import nachos.machine.*;
import java.util.*;
/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>, and multiple
 * threads can be waiting to <i>listen</i>. But there should never be a time
 * when both a speaker and a listener are waiting, because the two threads can
 * be paired off at this point.
 */
public class Communicator {

        ArrayList<Integer> buf;
        private Lock commLock;
        private Condition speakCondition;
        private Condition listenCondition;
        //private Condition2 retCondition;
        //private Integer buffer;
	/**
	 * Allocate a new communicator.
	 */
	public Communicator() {
	
          commLock = new Lock();

          speakCondition = new Condition(commLock);

          listenCondition = new Condition(commLock);
          buf = new ArrayList<Integer>();
        }

	/**
	 * Wait for a thread to listen through this communicator, and then transfer
	 * <i>word</i> to the listener.
	 * 
	 * <p>
	 * Does not return until this thread is paired up with a listening thread.
	 * Exactly one listener should receive <i>word</i>.
	 * 
	 * @param word the integer to transfer.
	 */
	public void speak(int word) {
          commLock.acquire();
        
          if(buf.size() == 0)
          {
            buf.add(0, word);
          }
          else
          {
            buf.add(buf.size(), word);
          }

          listenCondition.wake();
          speakCondition.sleep();
          commLock.release();
        }

	/**
	 * Wait for a thread to speak through this communicator, and then return the
	 * <i>word</i> that thread passed to <tt>speak()</tt>.
	 * 
	 * @return the integer transferred.
	 */
	public int listen() {
	
          commLock.acquire();

          while(buf.size()  == 0)
          {
            listenCondition.sleep();
          }

          speakCondition.wake();
    
          int retValue = buf.get(0);
          
          buf.remove(0);
          
          commLock.release();

          return retValue;
        
        }

    public static void commTest6() {
      final Communicator com = new Communicator();
      final long times[] = new long[4];
      final int words[] = new int[2];
      KThread speaker1 = new KThread( new Runnable () {
        public void run() {
          com.speak(4);
          times[0] = Machine.timer().getTime();
        }
      });
      speaker1.setName("S1");
      KThread speaker2 = new KThread( new Runnable () {
        public void run() {
          com.speak(7);
          times[1] = Machine.timer().getTime();
        }
      });
      speaker2.setName("S2");
      KThread listener1 = new KThread( new Runnable () {
        public void run() {
          times[2] = Machine.timer().getTime();
          words[0] = com.listen();
        }
      });
      listener1.setName("L1");
      KThread listener2 = new KThread( new Runnable () {
        public void run() {
          times[3] = Machine.timer().getTime();
          words[1] = com.listen();
        }
      });
      listener2.setName("L2");
                                                                                                                  
      speaker1.fork(); speaker2.fork(); listener1.fork(); listener2.fork();
      speaker1.join(); speaker2.join(); listener1.join(); listener2.join();
                                                                                                                            
      Lib.assertTrue(words[0] == 4, "Didn't listen back spoken word."); 
      Lib.assertTrue(words[1] == 7, "Didn't listen back spoken word.");
      Lib.assertTrue(times[0] > times[2], "speak() returned before listen() called.");
      Lib.assertTrue(times[1] > times[3], "speak() returned before listen() called.");
      System.out.println("commTest6 successful!");
    }

    // Invoke

    public static void commTest1() {
    	final Communicator com = new Communicator();
      	final long times[] = new long[4];
      	final int words[] = new int[2];
    	KThread speak1 = new KThread( new Runnable() {
    		public void run() {
    			times[0] = Machine.timer().getTime();
    			com.speak(10);
    			times[1] = Machine.timer().getTime();
    		}
    	});

    	KThread listen1 = new KThread( new Runnable() {
    		public void run() {
    			ThreadedKernel.alarm.waitUntil (10000);
    			times[2] = Machine.timer().getTime();
    			words[0] = com.listen();
    			times[3] = Machine.timer().getTime();
    		}
    	});

    	speak1.fork(); listen1.fork();
    	speak1.join(); listen1.join();
		Lib.assertTrue(words[0] == 10, "Didn't listen back spoken word.");
      	Lib.assertTrue(times[1] > times[3], "speak didn't block");
      	//Lib.assertTrue(times[1] > times[3], "speak() returned before listen() called.");
      	System.out.println("commTest1 successful!");

    }


    public static void commTest2() {
    	final Communicator com = new Communicator();
      	final long times[] = new long[4];
      	final int words[] = new int[2];
    	KThread speak1 = new KThread( new Runnable() {
    		public void run() {
    			times[0] = Machine.timer().getTime();
    			com.speak(10);
    			times[1] = Machine.timer().getTime();
    		}
    	});

    	KThread listen1 = new KThread( new Runnable() {
    		public void run() {
    			ThreadedKernel.alarm.waitUntil (10000);
    			times[2] = Machine.timer().getTime();
    			words[0] = com.listen();
    			times[3] = Machine.timer().getTime();
    		}
    	});

    	speak1.fork(); listen1.fork();listen1.join();
    	speak1.join();
    	// will never arrive here
		Lib.assertTrue(words[0] == 10, "Didn't listen back spoken word.");
      	Lib.assertTrue(times[1] > times[3], "speak didn't block");
      	//Lib.assertTrue(times[1] > times[3], "speak() returned before listen() called.");
      	System.out.println("commTest1 successful!");

    }

    public static void commTest3() {
    	final Communicator com1 = new Communicator();
    	final Communicator com2 = new Communicator();
      	final long times[] = new long[4];
      	final int words[] = new int[2];
    	KThread speak1 = new KThread( new Runnable() {
    		public void run() {
    			
    			com1.speak(10);
    			
    		}
    	});

    	KThread listen1 = new KThread( new Runnable() {
    		public void run() {
    			ThreadedKernel.alarm.waitUntil (10000);
    			words[0] = com1.listen();
    			System.out.println("listen1 completed");
    		}
    	});



    	KThread speak2 = new KThread( new Runnable() {
    		public void run() {
    			com2.speak(20);
    			
    		}
    	});

    	KThread listen2 = new KThread( new Runnable() {
    		public void run() {
    			
    			words[1] = com2.listen();
    			System.out.println("listen2 completed");
    		
    		}
    	});

    	speak1.fork(); ; listen2.fork(); speak2.fork();listen1.fork();
    	listen1.join(); speak1.join(); listen2.join(); speak2.join();
    	// will never arrive here
		Lib.assertTrue(words[0] == 10, "Didn't listen back spoken word from listen1.");
		Lib.assertTrue(words[1] == 20, "Didn't listen back spoken word from listen2.");
      	//Lib.assertTrue(times[1] > times[3], "speak() returned before listen() called.");
      	System.out.println("commTest3 successful!");

    }
    
    public static void selfTest()
    {
      commTest6();
    }

}
