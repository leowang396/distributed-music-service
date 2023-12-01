package albumstore;

import java.io.InputStream;
import java.sql.*;
import org.apache.commons.dbcp2.*;

public class AlbumStoreDao {

  private static BasicDataSource dataSource;
  private static final String LAST_INSERT_ID = "last_insert_id";
  private static final String ARTIST = "artist";
  private static final String TITLE = "title";
  private static final String YEAR = "year";
  private static final String POST_ALBUM_SQL =
          "INSERT INTO album (artist, title, year, image) VALUES (?,?,?,?)";
  private static final String GET_ALBUM_INFO_SQL =
          "SELECT artist, title, year FROM album WHERE album_id = ?";
  private static final String LAST_INSERT_ID_SQL = "SELECT LAST_INSERT_ID() AS " + LAST_INSERT_ID;

  public AlbumStoreDao() {
    dataSource = DBCPDataSource.getDataSource();
  }

  public int postAlbum(AlbumInfo albumInfo, InputStream image) {
    // If insertion is successful, res will be positive; else it stays 0.

    Connection conn = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    int res = 0;
    try {
      conn = dataSource.getConnection();
      preparedStatement = conn.prepareStatement(POST_ALBUM_SQL);
      preparedStatement.setString(1, albumInfo.getArtist());
      preparedStatement.setString(2, albumInfo.getTitle());
      preparedStatement.setString(3, albumInfo.getYear());
      preparedStatement.setBlob(4, image);
      preparedStatement.executeUpdate();

      preparedStatement = conn.prepareStatement(LAST_INSERT_ID_SQL);
      resultSet = preparedStatement.executeQuery();

      if (resultSet.next()) {
        res = resultSet.getInt(LAST_INSERT_ID);
      }
    } catch (SQLException e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
    } finally {
      try {
        if (conn != null) {
          conn.close();
        }
        if (preparedStatement != null) {
          preparedStatement.close();
        }
        if (resultSet != null) {
          resultSet.close();
        }
      } catch (SQLException se) {
        System.err.println(se.getMessage());
        se.printStackTrace();
      }
    }

    return res;
  }

  public AlbumInfo getAlbum(int albumId) {
    // If an album is found, res will be an instance of AlbumInfo class; else it is null.

    Connection conn = null;
    PreparedStatement preparedStatement = null;
    ResultSet resultSet = null;
    AlbumInfo res = null;
    try {
      conn = dataSource.getConnection();
      preparedStatement = conn.prepareStatement(GET_ALBUM_INFO_SQL);
      preparedStatement.setInt(1, albumId);
      resultSet = preparedStatement.executeQuery();

      if (resultSet.next()) {
        res = new AlbumInfo();
        res.setArtist(resultSet.getString(ARTIST));
        res.setTitle(resultSet.getString(TITLE));
        res.setYear(resultSet.getString(YEAR));
      }
    } catch (SQLException e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
    } finally {
      try {
        if (conn != null) {
          conn.close();
        }
        if (preparedStatement != null) {
          preparedStatement.close();
        }
        if (resultSet != null) {
          resultSet.close();
        }
      } catch (SQLException se) {
        se.printStackTrace();
      }
    }

    return res;
  }
}
