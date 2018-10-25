import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

public class BigDaddyClient implements Runnable {


    MyFileServer myFileServer;
    private CountDownLatch countDownLatch;

    public BigDaddyClient(MyFileServer myFileServer, CountDownLatch countDownLatch) {
        this.myFileServer = myFileServer;
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void run() {
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Set<String> availableFiles = myFileServer.availableFiles();
        Optional<String> filenameOp = availableFiles.stream().findFirst();
        String filename = filenameOp.get();

        for (int i = 0; i < 4; i++) {
            boolean write = ThreadLocalRandom.current().nextBoolean();

            if(write) {
                Optional<File> fileOp = myFileServer.open(filename, Mode.READWRITEABLE);
                File file = fileOp.get();
                file.write(file.read() + "*");
                myFileServer.close(file);
            } else {
                Optional<File> fileOp = myFileServer.open(filename, Mode.READABLE);
                File file = fileOp.get();
                file.read();
                myFileServer.close(file);
            }
        }
    }
}
