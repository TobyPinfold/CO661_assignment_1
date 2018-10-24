import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

public class FileProvider {

    ConcurrentHashMap<String, Mode> modeMap = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, String> contentMap = new ConcurrentHashMap<>();

    private static final FileProvider instance = new FileProvider();

    private FileProvider(){}

    public static FileProvider getInstance(){
        return instance;
    }

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

    public String getContent(String filename) {
        if(contentMap.containsKey(filename)) {
            return contentMap.get(filename);
        } else {
            return  "";
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


}
