
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**-----------------------------------------------------------------------------------------------**
 *         ,-----.                                                                                 *
 *        '  .--./ ,---. ,--,--,  ,---.,--.,--.,--.--.,--.--. ,---. ,--,--,  ,---.,--. ,--.        *
 *        |  |    | .-. ||      \| .--'|  ||  ||  .--'|  .--'| .-. :|      \| .--' \  '  /         *
 *        '  '--'\' '-' '|  ||  |\ `--.'  ''  '|  |   |  |   \   --.|  ||  |\ `--.  \   '          *
 *         `-----' `---' `--''--' `---' `----' `--'   `--'    `----'`--''--' `---'.-'  /           *
 *                                  Shared FileSystem Explanation                 `---'            *
 *                                                                                                 *
 **-----------------------------------------------------------------------------------------------**
 *
 *  My implementation for the filesystem is using a single semaphore to all process', the
 *  semaphore is initialised with a permit count of maximum integer to allow for a maximum
 *  amount of reader processes to run without blocking one another. The Read action only requires
 *  a single permit to execute and so many read can happen at any given time. A Write action
 *  however requires all permits inorder to proceed. Therefore a write cannot attain the semaphore
 *  until all permits are free, and if it does indeed get all permits, then a read action cannot
 *  acquire a permit to process either. This assures mutual exclusion between the two actions being
 *  performed and prevents race conditions between the two.
 *
 *  Fairness is enabled when I initialise the Semaphore as it uses FIFO on its queue of threads
 *  trying to acquire permits. This ensures that all threads get access to the Semaphore which
 *  prevents any thread being being blocked indefinitely by other processes causing the process to
 *  become starved.
 *
 *  With regards to write operations, the semaphore using FIFO ensures write can acquire available
 *  permits in queue and will have a chance to run. The necessity to acquire all permits then also
 *  ensures mutual exclusion in that no read operations can perform until write has released its
 *  permits (given it acquires all available permits) and Write cannot perform until it has
 *  acquired all permits, meaning all read actions must have finished before it can acquire
 *
 **-----------------------------------------------------------------------------------------------**/

public class MyFileServer implements FileServer {

    private FileProvider fileProvider = new FileProvider();
    private ConcurrentHashMap<String, FileLock> locks;

    public MyFileServer() {
        this.locks = new ConcurrentHashMap<>();
    }

    @Override
    public void create(String filename, String content) {
        if (!fileProvider.doesFileExist(filename)) {

            fileProvider.createFile(filename, content);
            if (!locks.containsKey(filename)) {
                createLock(filename);
            }
        }
    }

    @Override
    public Optional<File> open(String filename, Mode targetMode) {
        lockFile(filename, targetMode);

        if (fileProvider.doesFileExist(filename) && !targetMode.equals(Mode.UNKNOWN) && !targetMode.equals(Mode.CLOSED)) {

            fileProvider.setFileMode(filename, targetMode);
            File file = fileProvider.fetchFile(filename);

            return Optional.of(file);

        } else {
            return Optional.empty();
        }
    }


    @Override
    public void close(File closedFile) {
        String filename = closedFile.filename();

        if (fileProvider.doesFileExist(filename)) {

            File file = fileProvider.fetchFile(filename);

            if (file.mode().equals(Mode.READABLE) || file.mode().equals(Mode.READWRITEABLE)) {

                unlockFile(filename, file.mode());

                if (file.mode().equals(Mode.READWRITEABLE) && locks.get(filename).getCurrentReadCount() > 0) {
                    fileProvider.setContent(file.filename(), closedFile.read());
                    fileProvider.setFileMode(file.filename(), Mode.READABLE);
                }

                if ((file.mode().equals(Mode.READABLE) && locks.get(filename).getCurrentReadCount() < 1)) {
                    fileProvider.setFileMode(file.filename(), Mode.CLOSED);
                }

                if ((file.mode().equals(Mode.READWRITEABLE) && !locks.get(filename).isWriteSemaphoreLocked()
                        && locks.get(filename).getCurrentReadCount() <= 0)) {
                    fileProvider.setContent(file.filename(), closedFile.read());
                    fileProvider.setFileMode(file.filename(), Mode.CLOSED);
                }
            }
        }
    }


    @Override
    public Mode fileStatus(String filename) {
        return fileProvider.getMode(filename);
    }

    @Override
    public Set<String> availableFiles() {
        return fileProvider.getAllAvailableFiles();
    }


    private void lockFile(String filename, Mode mode) {
        if (locks.containsKey(filename)) {
            FileLock lock = locks.get(filename);

            switch (mode) {
                case READWRITEABLE:
                    try {
                        lock.acquireWriteLock();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case READABLE:
                    try {
                        lock.acquireReadLock();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }

            locks.replace(filename, lock);
        }
    }

    private void unlockFile(String filename, Mode mode) {
        if (locks.containsKey(filename)) {
            FileLock lock = locks.get(filename);

            switch (mode) {
                case READWRITEABLE:
                    try {
                        lock.releaseWriteLock();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case READABLE:
                    try {
                        lock.releaseReadLock();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }

            locks.replace(filename, lock);
        }
    }

    private void createLock(String filename) {
        FileLock lock = new FileLock();
        locks.put(filename, lock);
    }
}
