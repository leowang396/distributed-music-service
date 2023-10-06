package musicserviceclient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LoadTestOrchestrator {
  private static final int STARTUP_THREAD_GROUP_SIZE = 10;
  private static final int STARTUP_API_CALL_COUNT = 100;
  private static final int RUNNING_API_CALL_COUT = 1000;
  private static final String METRICS_FILE_PATH_HEADER = "./metrics";
  private static final String JAVA_WEB_SERVER_IP_ADDRESS_IDENTIFIER = "war";

  public static void main(String[] args) throws InterruptedException {
    int threadGroupSize = Integer.valueOf(args[0]);
    int numThreadGroups = Integer.valueOf(args[1]);
    long delay = Long.valueOf(args[2]);
    String iPAddress = args[3];

    ConcurrentLoadTestRequestDispatcher.dispatchConcurrentPostAndGetRequests(
            1, STARTUP_THREAD_GROUP_SIZE, STARTUP_API_CALL_COUNT, iPAddress, 0l, null);

    // Initiates a thread-safe list as the recording data structure.
    List<String[]> metrics = Collections.synchronizedList(new ArrayList<>());

    long startTime = System.currentTimeMillis();
    ConcurrentLoadTestRequestDispatcher.dispatchConcurrentPostAndGetRequests(
            numThreadGroups, threadGroupSize, RUNNING_API_CALL_COUT, iPAddress, delay, metrics);
    long wallTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime);

    // Builds a specific metric file name.
    StringBuilder metricsFilePath = new StringBuilder(METRICS_FILE_PATH_HEADER);
    metricsFilePath.append("-");
    if (iPAddress.contains(JAVA_WEB_SERVER_IP_ADDRESS_IDENTIFIER)) {
      metricsFilePath.append("java");
      metricsFilePath.append("-");
    } else {
      metricsFilePath.append("go");
      metricsFilePath.append("-");
    }
    metricsFilePath.append(threadGroupSize);
    metricsFilePath.append("-");
    metricsFilePath.append(numThreadGroups);
    metricsFilePath.append("-");
    metricsFilePath.append(delay);

    // Reports on load tests.
    MetricReporter.reportInMemoryList(metricsFilePath.toString(), metrics);
    System.out.println(String.format("Wall time: %d s", wallTime));
    System.out.println(String.format("Throughput: %d requests per second",
            (2 * RUNNING_API_CALL_COUT * numThreadGroups * threadGroupSize) / wallTime));
  }

}