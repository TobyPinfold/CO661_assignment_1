import java.util.concurrent.Semaphore;

public class FileLock
{
    private final Object writeLock = new Object();
    private final Object readLock = new Object();
    private final boolean ensureFairnessIsTrue = true;
    private Semaphore writeSemaphore = new Semaphore(1, ensureFairnessIsTrue);
    private Semaphore readSemaphore = new Semaphore(Integer.MAX_VALUE, ensureFairnessIsTrue);
    private boolean isWriting = false;


    public void acquireReadLock() throws Exception {
        if(!isWriteSemaphoreLocked()) {
            readSemaphore.acquire();
        } else {
            synchronized (readLock) {
                readLock.wait();
            }
        }
    }

    public void acquireWriteLock() throws Exception {
        try {
            if (!isReadLocked() && !isWriting) {
                writeSemaphore.acquire();
                isWriting = true;
            } else {
                synchronized (writeLock) {
                    writeLock.wait();
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }


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

    public void releaseWriteLock() {
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
