package albumstore;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

public class AlbumStoreDdbDao {

  private static final String TABLE_NAME = "cs6650-ddb-album";
  private static final String ALBUM_ID = "album_id";
  private static final String ARTIST = "artist";
  private static final String TITLE = "title";
  private static final String YEAR = "year";
  private static final String IMAGE = "image";
  // Presets DDB capacity to reduce auto-scaling delay.
  private static final Long INITIAL_READ_CAPACITY = 100l;
  private static final Long INITIAL_WRITE_CAPACITY = 2000l;

  private DynamoDbClient ddb;

  public AlbumStoreDdbDao() {
    Region region = Region.US_WEST_2;
    ddb = DynamoDbClient.builder()
            .region(region)
            .build();

    // Creates the DDB table if not exists on DAO start up.
    try {
      List<AttributeDefinition> attributeDefinitions= new ArrayList<>();
      attributeDefinitions.add(AttributeDefinition.builder()
              .attributeName(ALBUM_ID)
              .attributeType("S")
              .build());

      List<KeySchemaElement> keySchema = new ArrayList<>();
      keySchema.add(KeySchemaElement.builder().attributeName(ALBUM_ID).keyType(KeyType.HASH).build());

      CreateTableRequest createRequest = CreateTableRequest.builder()
              .tableName(TABLE_NAME)
              .keySchema(keySchema)
              .attributeDefinitions(attributeDefinitions)
              .provisionedThroughput(ProvisionedThroughput.builder()
                      .readCapacityUnits(INITIAL_READ_CAPACITY)
                      .writeCapacityUnits(INITIAL_WRITE_CAPACITY)
                      .build())
              .build();

      ddb.createTable(createRequest);

      // Waits for the table to become active.
      DescribeTableRequest describeRequest = DescribeTableRequest.builder()
              .tableName(TABLE_NAME)
              .build();
      ddb.waiter().waitUntilTableExists(describeRequest);
    } catch (ResourceInUseException e) {
      // Table already exists.
    } catch (Exception e) {
      // Ignored.
    }
  }

  // If insertion is successful, returns a universally unique string; otherwise returns null.
  public String postAlbum(AlbumInfo albumInfo, InputStream image) {
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
      ddb.putItem(request);
    } catch (ResourceNotFoundException e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
      res = null;
    } catch (DynamoDbException e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
      res = null;
    }

    return res;
  }

  // If an album is found, returns the instance of AlbumInfo class; otherwise returns null.
  public AlbumInfo getAlbum(String albumId) {
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
      // If there is no matching item, getItem does not return any data.
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
