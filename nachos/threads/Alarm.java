package nachos.threads;

import nachos.machine.*;
import java.util.PriorityQueue;
import java.util.Comparator;
/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {

	PriorityQueue<ThreadNode> pq = new PriorityQueue<ThreadNode> (10, new Comparator<ThreadNode>() {
		public int compare(ThreadNode n1, ThreadNode n2) {
			if (n1.getTime() - n2.getTime() > 0) {
				return -1;
			}
			else if (n1.getTime() - n2.getTime() < 0) {
				return 1;
			}
			else {
				return 0;
			}
		}
	});
	// create a node 
	private class ThreadNode{
		KThread thread;
		long remainingTime;

		public ThreadNode(KThread thread, long remainingTime) {
			this.thread = thread;
			this.remainingTime = remainingTime;
		}

		public long getTime() {
			return remainingTime;
		}

		public KThread getThread() {
			return thread;
		}

		public void setTime(long x) {
			remainingTime = x;
		}
	}




	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 * 
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */
	public Alarm() {
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() {
				timerInterrupt();
			}
		});
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread that
	 * should be run.
	 */
	public void timerInterrupt() {
<<<<<<< HEAD
		
		for (ThreadNode k : pq) {
			k.setTime(k.getTime() - 2);
		}
		printPQ(pq);
		while(pq.peek().getTime() <= 0) {
			ThreadNode nextNode = pq.poll();
			nextNode.thread.ready();
		}
		// make sure KThread.currentThread().yield() stay at the end of this method
		KThread.currentThread().yield();
=======
//		KThread.currentThread().yield();
>>>>>>> db2e167968361923041beb9bfaebc1b58420d446
	}

	public void printPQ(PriorityQueue<ThreadNode> pq) {
		for (ThreadNode k : pq) {
			System.out.println(k.getTime());
		}
	}


	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
	 * in the timer interrupt handler. The thread must be woken up (placed in
	 * the scheduler ready set) during the first timer interrupt where
	 * 
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 * 
	 * @param x the minimum number of clock ticks to wait.
	 * 
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
		// for now, cheat just to get something working (busy waiting is bad)
		pq.add(new ThreadNode(KThread.currentThread(), x));
		KThread pre = KThread.currentThread();
		KThread.yield();
		pre.sleep();
	}



	    // Add Alarm testing code to the Alarm class
    
    public static void alarmTest1() {
		int durations[] = {1000, 10*1000, 100*1000};
		long t0, t1;

		for (int d : durations) {
		    t0 = Machine.timer().getTime();
		    ThreadedKernel.alarm.waitUntil (d);
		    t1 = Machine.timer().getTime();
		    System.out.println ("alarmTest1: waited for " + (t1 - t0) + " ticks");
		}
    }

    // Implement more test methods here ...

    // Invoke Alarm.selfTest() from ThreadedKernel.selfTest()
    public static void selfTest() {
		alarmTest1();
	// Invoke your other test methods here ...
    }










}
