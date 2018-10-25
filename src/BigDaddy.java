import java.util.concurrent.CountDownLatch;

public class BigDaddy {

    public static void main(String[] args) {

        MyFileServer myFileServer = new MyFileServer();
        CountDownLatch countDownLatch = new CountDownLatch(1);

        myFileServer.create("a", "");

        Thread thread = new Thread(new BigDaddyClient(myFileServer, countDownLatch));
        Thread thread1 = new Thread(new BigDaddyClient(myFileServer, countDownLatch));
        Thread thread2 = new Thread(new BigDaddyClient(myFileServer, countDownLatch));

        thread.start();
        thread1.start();
        thread2.start();

        countDownLatch.countDown();

        try {
            thread.join();
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("DONE");
    }
}
