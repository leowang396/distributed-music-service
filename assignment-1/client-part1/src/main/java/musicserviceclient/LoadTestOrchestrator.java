package musicserviceclient;

import java.util.concurrent.TimeUnit;

public class LoadTestOrchestrator {
  private static final int STARTUP_THREAD_GROUP_SIZE = 10;
  private static final int STARTUP_API_CALL_COUNT = 100;
  private static final int RUNNING_API_CALL_COUT = 1000;

  public static void main(String[] args) throws InterruptedException {
    int threadGroupSize = Integer.valueOf(args[0]);
    int numThreadGroups = Integer.valueOf(args[1]);
    long delay = Long.valueOf(args[2]);
    String iPAddress = args[3];

    ConcurrentLoadTestRequestDispatcher.dispatchConcurrentPostAndGetRequests(
            1, STARTUP_THREAD_GROUP_SIZE, STARTUP_API_CALL_COUNT, iPAddress, 0l);

    long startTime = System.currentTimeMillis();
    ConcurrentLoadTestRequestDispatcher.dispatchConcurrentPostAndGetRequests(
            numThreadGroups, threadGroupSize, RUNNING_API_CALL_COUT, iPAddress, delay);
    long wallTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime);

    // Reports on load tests.
    System.out.println(String.format("Wall time: %d s", wallTime));
    System.out.println(String.format("Throughput: %d requests per second",
            (2 * RUNNING_API_CALL_COUT * numThreadGroups * threadGroupSize) / wallTime));
  }

}