package musicserviceclient;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.DefaultApi;
import io.swagger.client.api.LikeApi;
import io.swagger.client.model.AlbumsProfile;
import io.swagger.client.model.ImageMetaData;

public class ConcurrentLoadTestRequestDispatcher {

  private static final int MAX_RETRIES = 5;
  private static final int SUCCESS_STATUS_CODE = 200;
  private static final int FAILURE_STATUS_CODE = 400;
  private static final String STUB_IMAGE_PATH = "./png-transparent.png";
  private static final String LIKE = "like";
  private static final String DISLIKE = "dislike";

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
//          System.out.println(serverAddress);
          DefaultApi apiInstance = new DefaultApi(rawApiInstance);
          LikeApi likeApiInstance = new LikeApi(rawApiInstance);

          for (int k = 0; k < apiCallCount; k++) {
            // Hard codes test request data.
            File stubImage = new File(STUB_IMAGE_PATH);
            AlbumsProfile stubProfile = new AlbumsProfile();
            stubProfile.setArtist("new-artist");
            stubProfile.setTitle("new-title");
            stubProfile.setYear("2000");

            // Post a new album and image with up to 5 retries.
            int attempted = 0;
            long tic = System.currentTimeMillis();
            ImageMetaData postRes = null;
            while (attempted < MAX_RETRIES) {
              try {
                postRes = apiInstance.newAlbum(stubImage, stubProfile);
                break;
              } catch (ApiException e) {
                System.err.println(String.format("API call #%d failed: ", k) + e.getMessage());
                e.printStackTrace();
                attempted++;
              }
            }
            long toc = System.currentTimeMillis();
            if (metrics != null) {
              if (attempted < MAX_RETRIES) {
                metrics.add(new String[]{String.valueOf(tic), "POST",
                        String.valueOf(toc - tic), String.valueOf(SUCCESS_STATUS_CODE)});
              } else {
                metrics.add(new String[]{String.valueOf(tic), "POST",
                        String.valueOf(toc - tic), String.valueOf(FAILURE_STATUS_CODE)});
                continue;
              }
            }

            // Post 2 likes and 1 like for the new album with up to 5 retries each.
            if (postRes != null) {
              postReviewWithRetry(likeApiInstance, LIKE, postRes.getAlbumID(), metrics);
              postReviewWithRetry(likeApiInstance, LIKE, postRes.getAlbumID(), metrics);
              postReviewWithRetry(likeApiInstance, DISLIKE, postRes.getAlbumID(), metrics);
            } else if (metrics != null) {
              // Marks review POST calls as failed if the corresponding album POST call failed.
              metrics.add(new String[]{String.valueOf(System.currentTimeMillis()), "POST",
                      String.valueOf(0l), String.valueOf(FAILURE_STATUS_CODE)});
              metrics.add(new String[]{String.valueOf(System.currentTimeMillis()), "POST",
                      String.valueOf(0l), String.valueOf(FAILURE_STATUS_CODE)});
              metrics.add(new String[]{String.valueOf(System.currentTimeMillis()), "POST",
                      String.valueOf(0l), String.valueOf(FAILURE_STATUS_CODE)});
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

  private static void postReviewWithRetry(LikeApi api, String review, String albumID,
                                   List<String[]> metrics) {
    int attempted = 0;
    long tic = System.currentTimeMillis();
    while (attempted < MAX_RETRIES) {
      try {
        api.review(review, albumID);
        break;
      } catch (ApiException e) {
        System.err.println("Exception when calling LikeApi#review");
        e.printStackTrace();
        attempted++;
      }
    }
    long toc = System.currentTimeMillis();

    if (metrics != null) {
      if (attempted < MAX_RETRIES) {
        metrics.add(new String[]{String.valueOf(tic), "POST",
                String.valueOf(toc - tic), String.valueOf(SUCCESS_STATUS_CODE)});
      } else {
        metrics.add(new String[]{String.valueOf(tic), "POST",
                String.valueOf(toc - tic), String.valueOf(FAILURE_STATUS_CODE)});
      }
    }
  }
}
