package org.example;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

  private final static int CHANNEL_POOL_SIZE = 200;
  private final static int QOS = 200;
  private final static String RMQ_HOST = "RMQ_HOST";
  private final static String RMQ_USERNAME = "RMQ_USERNAME";
  private final static String RMQ_PASSWORD = "RMQ_PASSWORD";
  private final static String QUEUE_NAME = "store_review";
  private final static String LIKE = "like";
  private final static String DISLIKE = "dislike";
  // Uses private EC2 address.

  public static void main(String[] argv) throws Exception {
//    System.out.println(System.getenv("AWS_ACCESS_KEY_ID"));
//    System.out.println(" Main thread ID: " + Thread.currentThread().getId());
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(System.getenv(RMQ_HOST));
    factory.setUsername(System.getenv(RMQ_USERNAME));
    factory.setPassword(System.getenv(RMQ_PASSWORD));
    // Provides threads for processing callbacks.
    ExecutorService connectionExecutor = Executors.newFixedThreadPool(CHANNEL_POOL_SIZE);
    final Connection connection = factory.newConnection(connectionExecutor);
    mqConsumerDdbDao dao = new mqConsumerDdbDao();

    Runnable runnable = () -> {
      try {
        final Channel channel = connection.createChannel();
        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
        channel.basicQos(QOS);
        System.out.println(" [*] Thread " + Thread.currentThread().getId()
                + " waiting for messages. To exit press CTRL+C");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
          System.out.println( "Callback thread ID = " + Thread.currentThread().getId()
                  + " Received '" + new String(delivery.getBody(), "UTF-8") + "'");
          String[] tokens = new String(delivery.getBody(), "UTF-8").split(" ");

          boolean hasValidToken = false;
          try {
//            System.out.println(String.format("Thread %d: Before postReview",
//                    Thread.currentThread().getId()));
            if (tokens[0].equals(LIKE)) {
              hasValidToken = true;
              dao.postReview(true, tokens[1]);
            } else if (tokens[0].equals(DISLIKE)) {
              hasValidToken = true;
              dao.postReview(false, tokens[1]);
            }
//            System.out.println(String.format("Thread %d: postReview succeeded",
//                    Thread.currentThread().getId()));
          } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
          } finally {
            if (hasValidToken) {
              channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
//              System.out.println(String.format("Thread %d: basicAck",
//                      Thread.currentThread().getId()));
            } else {
              // No requeue of rejected messages to avoid an infinite loop between MQ and consumer.
              channel.basicReject(delivery.getEnvelope().getDeliveryTag(), false);
//              System.out.println(String.format("Thread %d: basicReject",
//                      Thread.currentThread().getId()));
            }
          }
        };

        channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> { });
      } catch (IOException ex) {
        System.err.println(ex.getMessage());
        ex.printStackTrace();
      }
    };

    for (int i = 0; i < CHANNEL_POOL_SIZE; i++) {
      Thread receiver = new Thread(runnable);
      receiver.start();
    }
  }
}