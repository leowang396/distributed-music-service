package albumstore;

import com.google.gson.Gson;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

import org.apache.commons.pool2.impl.GenericObjectPool;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@WebServlet(name = "ReviewServlet", value = "/review/*")
public class ReviewServlet extends HttpServlet {

  private final int REVIEW_POST_PATH_INFO_TOKEN_COUNT = 3;
  private final int CHANNEL_POOL_SIZE = 50;
  // Uses private EC2 address.
  private final String RMQ_HOST = "RMQ_HOST";
  private final String RMQ_USERNAME = "RMQ_USERNAME";
  private final String RMQ_PASSWORD = "RMQ_PASSWORD";
  private final String REVIEW_LIKE = "like";
  private final String REVIEW_DISLIKE = "dislike";
  private final String QUEUE_NAME = "store_review";
  private final Connection connection;
  private final GenericObjectPool<Channel> channelPool;

  public ReviewServlet() throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
//    System.out.println(System.getProperty(RMQ_HOST));
//    System.out.println(System.getProperty(RMQ_USERNAME));
//    System.out.println(System.getProperty(RMQ_PASSWORD));
    factory.setHost(System.getProperty(RMQ_HOST));
    factory.setUsername(System.getProperty(RMQ_USERNAME));
    factory.setPassword(System.getProperty(RMQ_PASSWORD));
    connection = factory.newConnection();
    channelPool = new GenericObjectPool<>(new RMQChannelFactory(connection));
    channelPool.setMaxTotal(CHANNEL_POOL_SIZE);
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//    System.out.println("doPost called!");
    response.setContentType("application/json");
    Gson gson = new Gson();

    // Validates and retrieves the likeornot and albumID from request URL.
    String pathInfo = request.getPathInfo();
    if (pathInfo == null || pathInfo.isEmpty()) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      ResponseMessage res = new ResponseMessage();
      res.setMsg("Invalid inputs: Wrong format for ReviewServlet POST request.");
      response.getOutputStream().print(gson.toJson(res));
      return;
    }
    String[] pathTokens = pathInfo.split("/");
    if (pathTokens.length != REVIEW_POST_PATH_INFO_TOKEN_COUNT
            || (pathTokens[1].equals(REVIEW_LIKE) && pathTokens[1].equals(REVIEW_DISLIKE))) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      ResponseMessage res = new ResponseMessage();
      res.setMsg("Invalid inputs: Wrong format for ReviewServlet POST request.");
      response.getOutputStream().print(gson.toJson(res));
      return;
    }

    // Publishes to MQ for async review processing.
    try {
      Channel channel = channelPool.borrowObject();

      // Declares a durable queue and sends a durable message.
      channel.queueDeclare(QUEUE_NAME, true, false, false, null);
      String message = String.format("%s %s", pathTokens[1], pathTokens[2]);
      channel.basicPublish("", QUEUE_NAME, MessageProperties.PERSISTENT_TEXT_PLAIN,
              message.getBytes(StandardCharsets.UTF_8));

      channelPool.returnObject(channel);
    } catch (IOException e) {
      System.err.println(e.getMessage());
      e.printStackTrace();

      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      ResponseMessage res = new ResponseMessage();
      res.setMsg("Unable to basicPublish.");
      response.getOutputStream().print(gson.toJson(res));
      return;
    } catch (Exception e) {
      System.err.println(e.getMessage());
      e.printStackTrace();

      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      ResponseMessage res = new ResponseMessage();
      res.setMsg("Unable to borrowObject.");
      response.getOutputStream().print(gson.toJson(res));
      return;
    }

    response.setStatus(HttpServletResponse.SC_CREATED);
    response.getOutputStream().flush();
  }
}
