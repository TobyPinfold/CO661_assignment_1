import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

public class FileLock
{

    private final static boolean ensureFairnessIsTrue = true;
    private enum Action {
        WRITE, READ
    }
    private Semaphore writeLock = new Semaphore(1, ensureFairnessIsTrue);
    private Semaphore readLock = new Semaphore(Integer.MAX_VALUE, ensureFairnessIsTrue);
    private ArrayList<HashMap<Action, Thread>> waitQueue = new ArrayList<>();
    int counter = 0;

    /**
     * Acquire Read Lock
     */
    public void acquireReadLock() throws Exception {
        synchronized (readLock) {
            try {

                if (isWriteLocked()) {
                    addCurrentlyRunningThreadToWaitQueue(Action.READ);
                    readLock.wait();
                }
            } catch (Exception e) {
                System.out.println(e + " ( " + Thread.currentThread().getName() + " )");
                e.printStackTrace();
            } finally {
                readLock.acquire();
            }
        }
    }

    /**
     * Acquire Write Lock
     */

    public void acquireWriteLock() throws Exception {
        synchronized (writeLock) {
            try {
                synchronized (writeLock) {
                    if (isWriteLocked() || isReadLocked()) {
                        addCurrentlyRunningThreadToWaitQueue(Action.READ);
                        Thread.currentThread().wait();
                    }
                }
            } catch (Exception e) {
                System.out.println(e + " ( " + Thread.currentThread().getName() + " )");
                e.printStackTrace();
            } finally {

                writeLock.acquire();
            }
        }
    }


    /**
     * Release Read Lock
     */
    public void releaseReadLock() throws Exception {
        synchronized(readLock) {
            try {
                readLock.release();
                notifyOldestWaitingThread();
            } catch (Exception e) {
                System.out.println(e + " ( " + Thread.currentThread().getName() + " )");
                e.printStackTrace();
            }
        }
    }


    /**
     * Release Write Lock
     */
    public synchronized void releaseWriteLock() throws Exception {
        try {
            writeLock.release();
            notifyOldestWaitingThread();
        } catch (Exception e) {
            System.out.println(e + " ( "  + Thread.currentThread().getName() + " )");
            e.printStackTrace();
        }
    }

    public boolean isWriteLocked() {
        return this.writeLock.availablePermits() <= 0;
    }

    public boolean isReadLocked() {
        return Integer.MAX_VALUE - readLock.availablePermits() > 0;
    }


    public int getReadLockCount() {
        return Integer.MAX_VALUE - readLock.availablePermits();
    }

    private synchronized void addCurrentlyRunningThreadToWaitQueue(Action requestedAction) {
        HashMap<Action, Thread> waitingThread = new HashMap<>();

        waitingThread.put(requestedAction, Thread.currentThread());

        waitQueue.add(waitingThread);
    }

    private void notifyOldestWaitingThread() {
        if(!waitQueue.isEmpty()) {
           HashMap<Action, Thread> waitingThread = waitQueue.get(0);
           waitingThread.forEach((action, thread) -> {
              switch (action) {
                  case WRITE: if(!isReadLocked() && !isWriteLocked()) {
                      synchronized (writeLock) {
                          thread.notify();
                          waitQueue.remove(0);
                      }
                  }
                  break;
                  case READ: if(!isWriteLocked()) {
                      synchronized (readLock) {
                          thread.notify();
                          waitQueue.remove(0);
                      }
                  }
                  break;
              }


           });
        }
    }
}
