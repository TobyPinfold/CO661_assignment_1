
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
 *                                                                                                 *
 *  My Approach to this assignment, was to create a custom FileLock, that utilises synchronised    *
 *  blocks and semaphores. Each action (READ, READWRITE) has its own semaphore and lock Object     *
 *  The Semaphore is utilised to handle queued threads, whilst the lock object is used to          *
 *  synchronise its related threads (same action) to manage which threads get told to wait         *
 *  or be notified. While same requests are being received, ie (R, R, R, R) then they are all      *
 *  added to the read semaphore. As soon as a different request is received ie (R,R,R,R,W) then    *
 *  the requests are synchronised to the lock object and enter a wait state. This allows the       *
 *  remaining (R) requests (blocked) to finish that entered before the (W) but will ensure the     *
 *  (W) gets a change to write. Following the write, the unlock will determine whether to notify   *
 *  the (R) lock  or to allow other (W) requests to execute.                                       *
 *                                                                                                 *
 *  To avoid race conditions, between read and write requests I ensure mutual exclusion by         *
 *  synchronising on their respective lock objects I've created, this ensures only one request to  *
 *  acquire or release a lock can happen at a given time. Provided I am also instantiating the     *
 *  write semaphore with only 1 permit, it ensures only one write condition can execute at a time, *
 *  ensuring Mutual exclusion for all write operations. The solution is not entirely fair, as it   *
 *  is biased towards write conditions. The thinking here, being if you wanted to read, you would  *
 *  want the most up to date version. Subsequently in the event there was a continuous stream of   *
 *  (W) then it could starve an (R) until all (W) have finished. But given the probable desire to  *
 *  read the latest version then that should be fine.                                              *
 *                                                                                                 *
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
