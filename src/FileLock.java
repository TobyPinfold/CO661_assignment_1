import com.sun.xml.internal.xsom.impl.Const;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class FileLock
{

    private final static boolean ensureFairnessIsTrue = true;
    private Semaphore writeLock = new Semaphore(1, ensureFairnessIsTrue);
    private Semaphore readLock = new Semaphore(Integer.MAX_VALUE, ensureFairnessIsTrue);
    private ArrayList<Thread> waitQueue = new ArrayList<>();

    /**
     * Acquire Read Lock
     */
    public synchronized void acquireReadLock() throws Exception {
        try {
            int numberOfAquiredReadLocks = Integer.MAX_VALUE - readLock.availablePermits();
            boolean fileIsNotBeingRead = numberOfAquiredReadLocks <= 0;
            boolean fileIsNotBeingWritten = writeLock.availablePermits() > 0;

            if(fileIsNotBeingRead && fileIsNotBeingWritten) {
                readLock.acquire();
            } else {
                readLock.wait();
                addCurrentlyRunningThreadToWaitQueue();
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }


    /**
     * Release Read Lock
     */
    public synchronized void releaseReadLock() throws Exception {
        try {


        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * Acquire Write Lock
     */

    public synchronized void acquireWriteLock() throws Exception {
        try {
            int numberOfAquiredWriteLocks = writeLock.availablePermits() - 1;
            if(numberOfAquiredWriteLocks <= 0) {
                writeLock.acquire();
            } else {
                writeLock.wait();
                addCurrentlyRunningThreadToWaitQueue();
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * Release Write Lock
     */
    public synchronized void releaseWriteLock() throws Exception {
        try {
            if(readLock.)

        } catch (Exception e) {
            System.out.println(e);
        }
    }


    private void addCurrentlyRunningThreadToWaitQueue() {
        Thread theCurrentlyRunningThread = Thread.currentThread();
        waitQueue.add(theCurrentlyRunningThread);
    }
}
