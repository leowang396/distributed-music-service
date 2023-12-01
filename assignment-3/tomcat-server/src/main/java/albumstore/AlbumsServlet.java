package albumstore;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

@WebServlet(name="AlbumsServlet", value="/albums/*")
@MultipartConfig
public class AlbumsServlet extends HttpServlet {

    private final String POST_PROFILE_FIELD = "profile";
    private final String POST_IMAGE_FILE_FIELD = "image";
    private final String ARTIST = "artist";
    private final String TITLE = "title";
    private final String YEAR = "year";
    private final int GET_URL_PARTS = 2;
    private AlbumStoreDdbDao albumStoreDao;

    public AlbumsServlet() {
        albumStoreDao = new AlbumStoreDdbDao();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
//        System.out.println("doGet called!");
        Gson gson = new Gson();

        // Validates that albumID is provided in request URL.
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            ResponseMessage res = new ResponseMessage();
            res.setMsg("Invalid GET URL");
            response.getOutputStream().print(gson.toJson(res));
            return;
        }

        String[] pathInfoParts = pathInfo.split("/");
        if (pathInfoParts.length != GET_URL_PARTS) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            ResponseMessage res = new ResponseMessage();
            res.setMsg("Invalid GET URL");
            response.getOutputStream().print(gson.toJson(res));
            return;
        }
        AlbumInfo res = albumStoreDao.getAlbum(pathInfoParts[1]);

        // Prepares HTTP response.
        // Case 1: ID is not found.
        if (res == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            ResponseMessage msg = new ResponseMessage();
            msg.setMsg("Key not found");
            response.getOutputStream().print(gson.toJson(msg));
            return;
        }
        // Case 2: ID is found.
        response.setStatus(HttpServletResponse.SC_OK);
        response.getOutputStream().print(gson.toJson(res));
        response.getOutputStream().flush();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
//        System.out.println("doPost called!");
        Gson gson = new Gson();

        // Validates that request URL contains no extra characters.
        String pathInfo = request.getPathInfo();
        if (pathInfo != null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            ResponseMessage res = new ResponseMessage();
            res.setMsg("Invalid POST URL");
            response.getOutputStream().print(gson.toJson(res));
            return;
        }

        // Retrieves the image file.
        Part imageFilePart = request.getPart(POST_IMAGE_FILE_FIELD);
        if (imageFilePart == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            ResponseMessage res = new ResponseMessage();
            res.setMsg("POST request body missing image field");
            response.getOutputStream().print(gson.toJson(res));
            return;
        }
        long imageSize = imageFilePart.getSize();

        // Retrieves the profile field.
        Part albumProfilePart = request.getPart(POST_PROFILE_FIELD);
        if (albumProfilePart == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            ResponseMessage res = new ResponseMessage();
            res.setMsg("POST request body missing profile field");
            response.getOutputStream().print(gson.toJson(res));
            return;
        }
        AlbumInfo newAlbumInfo = getCleanAlbumInfo(albumProfilePart, gson);

//        System.out.println("Starting postAlbum");
        String albumID = albumStoreDao.postAlbum(newAlbumInfo, imageFilePart.getInputStream());
//        System.out.println("Completed postAlbum");

        // Prepares HTTP response.
        // Case 1: Album info failed to be inserted.
        if (albumID == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            ResponseMessage res = new ResponseMessage();
            res.setMsg("POST request failed");
            response.getOutputStream().print(gson.toJson(res));
            return;
        }
        // Case 2: Album info is successfully inserted.
        response.setStatus(HttpServletResponse.SC_OK);
        ImageMetaData res = new ImageMetaData();
        res.setAlbumID(albumID);
        res.setImageSize(String.valueOf(imageSize));
        response.getOutputStream().print(gson.toJson(res));
        response.getOutputStream().flush();
    }

    // Handles the inconsistency between SwaggerHub API and Postman requests manually.
    private AlbumInfo getCleanAlbumInfo(Part albumProfilePart, Gson gson) throws IOException {
        AlbumInfo res = new AlbumInfo();

        albumProfilePart.getInputStream().toString();
        String albumInfoString = new BufferedReader(new InputStreamReader(
                albumProfilePart.getInputStream()))
                .lines()
                .collect(Collectors.joining("\n"));

        String[] albumInfoArray = albumInfoString.split("\n");
        if (albumInfoArray.length > 1) {
            // Case 1: Request from SwaggerHub API.
            for (String line: albumInfoArray) {
                if (line.contains(ARTIST)) {
                    res.setArtist(line.split(ARTIST + ": ")[1]);
                } else if (line.contains(TITLE)) {
                    res.setTitle(line.split(TITLE + ": ")[1]);
                } else if (line.contains(YEAR)) {
                    res.setYear(line.split(YEAR + ": ")[1]);
                }
            }
        } else {
            // Case 2: Request from Postman.
            try {
                res = gson.fromJson(albumInfoString, AlbumInfo.class);
            } catch (JsonSyntaxException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }

        return res;
    }
}
