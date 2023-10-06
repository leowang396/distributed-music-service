package musicserviceclient;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.DefaultApi;
import io.swagger.client.model.AlbumsProfile;

public class ConcurrentLoadTestRequestDispatcher {
  private static final int MAX_RETRIES = 5;
  private static final int RETRY_STATUS_LOWER = 400;
  private static final int RETRY_STATUS_UPPER = 599;

  protected static void dispatchConcurrentPostAndGetRequests(int numThreadGroups,
                                                             int threadGroupSize, int apiCallCount,
                                                             String serverAddress, long delay,
                                                             List<String[]> metrics)
          throws InterruptedException {
    CountDownLatch completed = new CountDownLatch(numThreadGroups * threadGroupSize);

    for (int i = 0; i < numThreadGroups; i++) {

      for (int j = 0; j < threadGroupSize; j++) {
        Runnable thread = () -> {
          ApiClient rawApiInstance = new ApiClient();
          rawApiInstance.setBasePath(serverAddress);
          DefaultApi apiInstance = new DefaultApi(rawApiInstance);

          for (int k = 0; k < apiCallCount; k++) {
            try {
              // Hard codes test request data.
              File stubImage = new File("./nmtb.png");
              AlbumsProfile stubProfile = new AlbumsProfile();
              stubProfile.setArtist("new-artist");
              stubProfile.setTitle("new-title");
              stubProfile.setYear("2000");

              int attempted = 1;
              long tic = System.currentTimeMillis();
              ApiResponse postRes = apiInstance.newAlbumWithHttpInfo(stubImage, stubProfile);
              while (attempted < MAX_RETRIES && postRes.getStatusCode() >= RETRY_STATUS_LOWER
                      && postRes.getStatusCode() <= RETRY_STATUS_UPPER) {
                postRes = apiInstance.newAlbumWithHttpInfo(stubImage, stubProfile);
                attempted++;
              }
              long toc = System.currentTimeMillis();
              if (metrics != null) {
                metrics.add(new String[]{String.valueOf(tic), "POST",
                        String.valueOf(toc - tic), String.valueOf(postRes.getStatusCode())});
              }
              //            System.out.println(postRes.getData());

              String stubAlbumID = "albumID_example";
              attempted = 1;
              tic = System.currentTimeMillis();
              ApiResponse getRes = apiInstance.getAlbumByKeyWithHttpInfo(stubAlbumID);
              while (attempted < MAX_RETRIES && getRes.getStatusCode() >= RETRY_STATUS_LOWER
                      && getRes.getStatusCode() <= RETRY_STATUS_UPPER) {
                getRes = apiInstance.getAlbumByKeyWithHttpInfo(stubAlbumID);
                attempted++;
              }
              toc = System.currentTimeMillis();
              if (metrics != null) {
                metrics.add(new String[]{String.valueOf(tic), "GET",
                        String.valueOf(toc - tic), String.valueOf(getRes.getStatusCode())});
              }
              //            System.out.println(getRes.getData());
            } catch (ApiException e) {
              System.err.println("Fatal error when calling API: " + e.getMessage());
              e.printStackTrace();
            }
          }

          completed.countDown();
        };

        new Thread(thread).start();
      }

      if (i < numThreadGroups - 1 && delay > 0) {
        TimeUnit.SECONDS.sleep(delay);
      }
    }

    completed.await();
  }

}
