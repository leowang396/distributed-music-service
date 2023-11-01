package albumstore;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

public class AlbumStoreDdbDao {

  private static final String TABLE_NAME = "cs6650-ddb-album";
  private static final String ALBUM_ID = "album_id";
  private static final String ARTIST = "artist";
  private static final String TITLE = "title";
  private static final String YEAR = "year";
  private static final String IMAGE = "image";

  private DynamoDbClient ddb;

  public AlbumStoreDdbDao() {
    Region region = Region.US_WEST_2;
    ddb = DynamoDbClient.builder()
            .region(region)
            .build();
  }
  public String postAlbum(AlbumInfo albumInfo, InputStream image) {
    // If insertion is successful, res will be a universally unique string; else it will be null.

    String res = UUID.randomUUID().toString();
    HashMap<String, AttributeValue> itemValues = new HashMap<>();
    itemValues.put(ALBUM_ID, AttributeValue.builder().s(res).build());
    itemValues.put(ARTIST, AttributeValue.builder().s(albumInfo.getArtist()).build());
    itemValues.put(TITLE, AttributeValue.builder().s(albumInfo.getTitle()).build());
    itemValues.put(YEAR, AttributeValue.builder().s(albumInfo.getYear()).build());
    itemValues.put(IMAGE, AttributeValue.builder().b(SdkBytes.fromInputStream(image)).build());

    PutItemRequest request = PutItemRequest.builder()
            .tableName(TABLE_NAME)
            .item(itemValues)
            .build();

    try {
      PutItemResponse response = ddb.putItem(request);
    } catch (ResourceNotFoundException e) {
      res = null;
    } catch (DynamoDbException e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
      res = null;
    }

    return res;
  }

  public AlbumInfo getAlbum(String albumId) {
    // If an album is found, res will be an instance of AlbumInfo class; else it is null.

    HashMap<String,AttributeValue> keyToGet = new HashMap<>();
    keyToGet.put(ALBUM_ID, AttributeValue.builder()
            .s(albumId)
            .build());

    GetItemRequest request = GetItemRequest.builder()
            .key(keyToGet)
            .tableName(TABLE_NAME)
            .build();

    AlbumInfo res = null;
    try {
      // If there is no matching item, GetItem does not return any data.
      Map<String,AttributeValue> returnedItem = ddb.getItem(request).item();
      if (!returnedItem.isEmpty() && returnedItem.containsKey(ARTIST)
              && returnedItem.containsKey(TITLE) && returnedItem.containsKey(YEAR)) {
        res = new AlbumInfo();
        res.setArtist(returnedItem.get(ARTIST).toString());
        res.setTitle(returnedItem.get(TITLE).toString());
        res.setYear(returnedItem.get(YEAR).toString());
      }
    } catch (DynamoDbException e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
      res = null;
    }

    return res;
  }
}
