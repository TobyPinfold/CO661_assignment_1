import java.util.concurrent.Semaphore;

public class FileLock
{
    private final Object writeLock = new Object();
    private final Object readLock = new Object();
    private final boolean ensureFairnessIsTrue = true;
    private Semaphore writeSemaphore = new Semaphore(1, ensureFairnessIsTrue);
    private Semaphore readSemaphore = new Semaphore(Integer.MAX_VALUE, ensureFairnessIsTrue);
    private boolean isWriting = false;

    /**
     * Acquire Read Lock
     */
    public void acquireReadLock() throws Exception {
        if(!isWriteSemaphoreLocked()) {
            readSemaphore.acquire();
        } else {
            synchronized (readLock) {
                readLock.wait();
            }
        }
    }


    /**
     * Acquire Write Lock
     */

    public void acquireWriteLock() throws Exception {
        try {
            System.out.println(!isReadLocked() && !isWriting);
            if (!isReadLocked() && !isWriting) {
                System.out.println("acquire");
                writeSemaphore.acquire();
            } else {
                synchronized (writeLock) {
                    System.out.println("wait");
                    writeLock.wait();
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Release Read Lock
     */
    public void releaseReadLock() throws Exception {
        try {
            if (getCurrentReadCount() <= 1) {
                synchronized (writeLock) {
                    isWriting = true;
                    writeLock.notify();
                }
            } else {
                synchronized (readLock) {
                    isWriting = false;
                    readLock.notifyAll();
                }
            }
        } finally {
            if(isReadLocked()){
                isWriting = false;
                readSemaphore.release();
            }
        }
    }

    /**
     * Release Write Lock
     */
    public void releaseWriteLock() throws Exception {
        try {
            if (writeSemaphore.getQueueLength() < 1) {
                synchronized (readLock) {
                    readLock.notifyAll();
                    isWriting = false;
                }
            }
        } finally {
            if (isWriteSemaphoreLocked()) {
                writeSemaphore.release();
                isWriting = true;
            }
        }
    }

    public synchronized boolean isWriteSemaphoreLocked() {
        return this.writeSemaphore.availablePermits() <= 0;
    }

    public  boolean isReadLocked() {
        return Integer.MAX_VALUE - readSemaphore.availablePermits() > 0;
    }

    public int getCurrentReadCount() {
        return Integer.MAX_VALUE - readSemaphore.availablePermits();
    }
}
