package org.example;

import java.util.HashMap;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

public class mqConsumerDdbDao {
  private static final String TABLE_NAME = "cs6650-ddb-album";
  private static final String ALBUM_ID = "album_id";
  private static final String LIKES = "likes";
  private static final String DISLIKES = "dislikes";
  private static final String UPDATE_EXPRESSION_ADD = "ADD";
  private static final String EXPRESSION_ATTRIBUTE_VALUES_ADD = ":inc";

  private DynamoDbClient ddb;

  public mqConsumerDdbDao() {
    Region region = Region.US_WEST_2;
    ddb = DynamoDbClient.builder()
            .region(region)
            .build();
  }

  // Returns true if the post is successful; otherwise returns false.
  public boolean postReview(boolean isLike, String albumId) {
    HashMap<String, AttributeValue> itemKey = new HashMap<>();
    itemKey.put(ALBUM_ID, AttributeValue.builder()
            .s(albumId)
            .build());
    // Increments respective counter field by 1 in each call.
    HashMap<String, AttributeValue> expressionAttributeValues = new HashMap<>();
    expressionAttributeValues.put(EXPRESSION_ATTRIBUTE_VALUES_ADD, AttributeValue.builder()
            .n("1")
            .build());
    String name = isLike ? LIKES : DISLIKES;
    String updateExpression = String.format("%s %s %s",
            UPDATE_EXPRESSION_ADD, name, EXPRESSION_ATTRIBUTE_VALUES_ADD);
    UpdateItemRequest request = UpdateItemRequest.builder()
            .tableName(TABLE_NAME)
            .key(itemKey)
            .expressionAttributeValues(expressionAttributeValues)
            .updateExpression(updateExpression)
            .returnValues("ALL_NEW")
            .build();

    try {
//      System.out.println(String.format("Thread %d: before updateItem",
//              Thread.currentThread().getId()));
      ddb.updateItem(request).toString();
//      System.out.println(String.format("Thread %d: updateItem succeeded, %s",
//              Thread.currentThread().getId(), ));
    } catch (ResourceNotFoundException e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
      return false;
    } catch (DynamoDbException e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
      return false;
    }

//    System.out.println(String.format("Thread %d: postReview returned",
//            Thread.currentThread().getId()));
    return true;
  }
}
