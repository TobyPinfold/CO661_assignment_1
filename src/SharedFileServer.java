import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**---------------------------------------------------------------------------------------------**
 *         ,-----.                                                                               *
 *        '  .--./ ,---. ,--,--,  ,---.,--.,--.,--.--.,--.--. ,---. ,--,--,  ,---.,--. ,--.      *
 *        |  |    | .-. ||      \| .--'|  ||  ||  .--'|  .--'| .-. :|      \| .--' \  '  /       *
 *        '  '--'\' '-' '|  ||  |\ `--.'  ''  '|  |   |  |   \   --.|  ||  |\ `--.  \   '        *
 *         `-----' `---' `--''--' `---' `----' `--'   `--'    `----'`--''--' `---'.-'  /         *
 *                                Shared FileSystem Explanation                   `---'          *
 *                                                                                               *
 **---------------------------------------------------------------------------------------------**
 *                                                                                               *
 *  The Approach used here, utilises a locking system using the Java Provided                    *
 *  ReentrantReadWriteLock. The benefits of this Lock object is its ability to handle both       *
 *  read and writing locks as a single individual lock and automatically allows for multiple     *
 *  read access and individual write access. The fac that it is also a Reentrant lock allows     *
 *  us to initialise the Lock with fairness implemented with a simple constructor boolean        *
 *  parameter which solves the issues surrounding starvation as the longest waiting thread       *
 *  is given priority upon the lock being freed.                                                 *
 *                                                                                               *
 *  I utilised the Lock mentioned above to be referenced in a hashmap to its corresponding       *
 *  file. This subsequently meant each file had its own lock and could be locked individually.   *
 *  This meant multiple different files can be accessed at the same time without interference.   *
 *  With regard to reading the same file, the ReadWriteLock provided such functionality as to    *
 *  allow multiple read locks to be set on any given lock by other threads, while only allowing  *
 *  a single thread to have write access at any given time. By only allowing an individual file  *
 *  to be written to by one thread at any given time, it avoids race conditions and ensures      *
 *  mutual exclusion.                                                                            *
 *                                                                                               *
 **---------------------------------------------------------------------------------------------**/

public class SharedFileServer implements FileServer {

    private ConcurrentHashMap<String, File> files;
    private ConcurrentHashMap<String, FileLock> locks;

    public SharedFileServer() {
        this.files = new ConcurrentHashMap<>();
        this.locks = new ConcurrentHashMap<>();
    }

    @Override
    public void create(String filename, String content) {
        if (!files.containsKey(filename)) {

            File newFile = new File(filename, content, Mode.CLOSED);
            files.put(filename, newFile);

            if (!locks.containsKey(filename)) {
                createLock(filename);
            }
        }
    }


    @Override
    public Optional<File> open(String filename, Mode targetMode) {
        lockFile(filename, targetMode);

        if (files.containsKey(filename) && !targetMode.equals(Mode.UNKNOWN) && !targetMode.equals(Mode.CLOSED)) {

            File file = files.get(filename);

            manipulateModeOfFile(file, targetMode);

            file = files.get(filename);


            return Optional.of(file);

        } else {
            return Optional.empty();
        }
    }


    @Override
    public void close(File closedFile) {

        String filename = closedFile.filename();

        if (files.get(filename) != null) {

            File file = files.get(filename);

            if (file.mode().equals(Mode.READABLE) || file.mode().equals(Mode.READWRITEABLE)) {

                unlockFile(filename, file.mode());

                if (file.mode().equals(Mode.READWRITEABLE) && locks.get(filename).getReadLockCount() > 0) {
                    manipulateModeOfFile(file, Mode.READABLE);
                }

                if ((file.mode().equals(Mode.READABLE) && locks.get(filename).getReadLockCount() < 1)) {
                    manipulateModeOfFile(file, Mode.CLOSED);
                }

                if ((file.mode().equals(Mode.READWRITEABLE) && !locks.get(filename).isWriteLocked() && locks.get(filename).getReadLockCount() <= 0)) {
                    manipulateModeOfFile(file, Mode.CLOSED);
                }

            }
        }
    }


    @Override
    public Mode fileStatus(String filename) {
        if (files.containsKey(filename)) {
            return files.get(filename).mode();
        } else {
            return Mode.UNKNOWN;
        }
    }

    @Override
    public Set<String> availableFiles() {
        HashSet<String> availableFiles = new HashSet<>();
        files.forEach((filename, file) -> {
            availableFiles.add(filename);
        });
        return availableFiles;
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

            locks.remove(filename);
            locks.put(filename, lock);
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

            locks.remove(filename);
            locks.put(filename, lock);
        }
    }

    private void createLock(String filename) {
        FileLock lock = new FileLock();
        locks.put(filename, lock);
    }

    private void manipulateModeOfFile(File file, Mode targetMode) {
        files.remove(file.filename());
        File newFile = new File(file.filename(), file.read(), targetMode);
        files.put(file.filename(), newFile);
    }
}
