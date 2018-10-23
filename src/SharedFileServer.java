import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
 *                                                                                               *
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
            System.out.println(file.filename());
            if (file.mode().equals(Mode.READABLE) || file.mode().equals(Mode.READWRITEABLE)) {

                unlockFile(filename, file.mode());

                if (file.mode().equals(Mode.READWRITEABLE) && locks.get(filename).getCurrentReadCount() > 0) {
                    manipulateModeOfFile(file, Mode.READABLE);
                }

                if ((file.mode().equals(Mode.READABLE) && locks.get(filename).getCurrentReadCount() < 1)) {
                    manipulateModeOfFile(file, Mode.CLOSED);
                }

                if ((file.mode().equals(Mode.READWRITEABLE) && !locks.get(filename).isWriteSemaphoreLocked()
                        && locks.get(filename).getCurrentReadCount() <= 0)) {
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
