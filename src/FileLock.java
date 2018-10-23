import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.Semaphore;

public class FileLock
{

    private final static boolean ensureFairnessIsTrue = true;
    private enum Action {
        WRITE, READ
    }
    private Semaphore writeLock = new Semaphore(1, ensureFairnessIsTrue);
    private Semaphore readLock = new Semaphore(Integer.MAX_VALUE, ensureFairnessIsTrue);
    private LinkedHashMap<Action, Thread> waitQueue = new LinkedHashMap<>();

    /**
     * Acquire Read Lock
     */
    public synchronized void acquireReadLock() throws Exception {
        try {
            int numberOfAcquiredReadLocks = getReadLockCount();
            boolean fileIsNotBeingRead = numberOfAcquiredReadLocks <= 0;
            boolean fileIsNotBeingWritten = writeLock.availablePermits() > 0;

            if(fileIsNotBeingRead && fileIsNotBeingWritten) {
                readLock.acquire(1);
            } else {
                readLock.wait();
                addCurrentlyRunningThreadToWaitQueue(Action.READ);
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
            System.out.println(Integer.MAX_VALUE - readLock.availablePermits());
            this.readLock.release(1);
            if(waitQueue.size() > 0) {
                Thread longestWaitingThread = waitQueue.get(0);
                longestWaitingThread.notify();
            }
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
                writeLock.acquire(1);
            } else {
                writeLock.wait();
                addCurrentlyRunningThreadToWaitQueue(Action.WRITE);
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
          this.writeLock.release(1);
          if(waitQueue.size() > 0) {
           Thread longestWaitingThread = waitQueue.get(0);
           longestWaitingThread.notify();
          }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public synchronized boolean isWriteLocked() {
        return this.writeLock.availablePermits() <= 0;
    }


    public synchronized int getReadLockCount() {
        return Integer.MAX_VALUE - readLock.availablePermits();
    }

    private void addCurrentlyRunningThreadToWaitQueue(Action requestedAction) {
        Thread theCurrentlyRunningThread = Thread.currentThread();
        waitQueue.put(requestedAction, theCurrentlyRunningThread);
    }

    private void notifyOldestWaitingThread() {
        if(!waitQueue.isEmpty()) {
//            waitQueue.
//            Thread oldestWaitingThread = waitQueue.get(0);
//            oldestWaitingThread.
        }
    }

}
