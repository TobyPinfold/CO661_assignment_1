
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
 *                                                                                                 *
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
