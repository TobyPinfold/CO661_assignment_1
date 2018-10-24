import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class FileProvider {

    ConcurrentHashMap<String, Mode> modeMap = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, String> contentMap = new ConcurrentHashMap<>();

    public void setFileMode(String filename, Mode targetMode) {
        if(modeMap.containsKey(filename)) {
            modeMap.replace(filename, targetMode);
        } else {
            modeMap.put(filename, targetMode);
        }
    }

    public void setContent(String filename, String content) {
        if(contentMap.containsKey(filename)){
            contentMap.replace(filename, content);
        } else {
            contentMap.put(filename, content);
        }
    }

    public Mode getMode(String filename) {
        if(modeMap.containsKey(filename)) {
            return modeMap.get(filename);
        } else {
            return Mode.UNKNOWN;
        }
    }

    public boolean doesFileExist(String filename) {
        return modeMap.containsKey(filename);
    }

    public void createFile(String filename, String content) {
        modeMap.put(filename, Mode.CLOSED);
        contentMap.put(filename, content);
    }

    public File fetchFile(String filename) {
        if(doesFileExist(filename)) {

            Mode mode = modeMap.get(filename);
            String content = contentMap.get(filename);

            File file = new File(filename, content, mode);

            return file;
        } else {
            return null;
        }
    }

    public HashSet<String> getAllAvailableFiles() {
        HashSet<String> availableFiles = new HashSet<>();
        modeMap.forEach((k, v) -> availableFiles.add(k));
        return availableFiles;
    }
}
