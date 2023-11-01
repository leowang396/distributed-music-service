package albumstore;

import org.apache.commons.dbcp2.*;

public class DBCPDataSource {

  private static BasicDataSource dataSource;
  private static final String HOST_NAME = System.getProperty("MySQL_HOST_NAME");
  private static final String PORT = System.getProperty("MySQL_PORT");
  private static final String DEFAULT_DATABASE = "album_store";
  private static final String USERNAME = System.getProperty("DB_USERNAME");
  private static final String PASSWORD = System.getProperty("DB_PASSWORD");
  private static final int INITIAL_POOL_SIZE = 10;
  private static final int MAX_POOL_SIZE = 60;

  static {
    try {
      Class.forName("com.mysql.cj.jdbc.Driver");
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }

    dataSource = new BasicDataSource();
    String url = String.format("jdbc:mysql://%s:%s/%s?serverTimezone=UTC",
            HOST_NAME, PORT, DEFAULT_DATABASE);
    dataSource.setUrl(url);
    dataSource.setUsername(USERNAME);
    dataSource.setPassword(PASSWORD);
    dataSource.setInitialSize(INITIAL_POOL_SIZE);
    dataSource.setMaxTotal(MAX_POOL_SIZE);
  }

  public static BasicDataSource getDataSource() {
    return dataSource;
  }
}