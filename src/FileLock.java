import java.util.concurrent.Semaphore;

public class FileLock
{
    private final boolean ensureFairnessIsTrue = true;
    private Semaphore semaphore = new Semaphore(Integer.MAX_VALUE, ensureFairnessIsTrue);
    private boolean isWriting = false;


    public  void acquireReadLock() throws Exception {
        semaphore.acquire(1);
    }

    public void acquireWriteLock() throws Exception {

        semaphore.acquire(Integer.MAX_VALUE);
        isWriting = true;
    }


    public synchronized void releaseReadLock() throws Exception {
        if(!isWriting) {
            semaphore.release(1);
        }
    }

    public synchronized void releaseWriteLock() {
        if(isWriting){
            semaphore.release(Integer.MAX_VALUE);
            isWriting = false;
        }
    }

    public synchronized boolean isWriteSemaphoreLocked() {
        return isWriting;
    }


    public int getCurrentReadCount() {
        return Integer.MAX_VALUE - semaphore.availablePermits();
    }
}
