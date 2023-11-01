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

@WebServlet(name="AlbumStoreServlet", value="/albums/*")
@MultipartConfig
public class AlbumStoreServlet extends HttpServlet {

    private static final String POST_PROFILE_FIELD = "profile";
    private static final String POST_IMAGE_FILE_FIELD = "image";
    private static final int GET_URL_PARTS = 2;
    private AlbumStoreDdbDao albumStoreDao;

    public AlbumStoreServlet() {
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
//        // Fix that MSIE browser incorrectly sends the full file path along the name.
//        String fileName = Paths.get(imageFilePart.getSubmittedFileName()).getFileName().toString();

        // Retrieves the profile field.
        Part albumProfilePart = request.getPart(POST_PROFILE_FIELD);
        if (albumProfilePart == null) {
//            System.out.println("POST request body missing profile field");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            ResponseMessage res = new ResponseMessage();
            res.setMsg("POST request body missing profile field");
            response.getOutputStream().print(gson.toJson(res));
            return;
        }
//        System.out.println("POST request body has profile field");
//        byte[] input = new byte[200];
//        albumProfilePart.getInputStream().read(input);
//        System.out.println(new String(input, StandardCharsets.UTF_8));
        AlbumInfo newAlbumInfo = getCleanAlbumInfo(albumProfilePart, gson);

//        System.out.println(gson.fromJson(String.valueOf(input), AlbumInfo.class));
//        gson.fromJson(String.valueOf(input), AlbumInfo.class);
//        String profileString = request.getParameter(POST_PROFILE_FIELD);
//        System.out.println(profileString);
//        AlbumInfo newAlbumInfo = gson.fromJson(
//                new BufferedReader(new InputStreamReader(albumProfilePart.getInputStream(), "UTF-8")),
//                AlbumInfo.class);
//        System.out.println(newAlbumInfo);

        String albumID = albumStoreDao.postAlbum(newAlbumInfo, imageFilePart.getInputStream());

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
        res.setAlbumID(String.valueOf(albumID));
        res.setImageSize(String.valueOf(imageSize));
        response.getOutputStream().print(gson.toJson(res));
        response.getOutputStream().flush();
    }

    // Handles the inconsistency between SwaggerHub API and Postman requests manually.
    private static AlbumInfo getCleanAlbumInfo(Part albumProfilePart, Gson gson) throws IOException {
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
                if (line.contains("artist")) {
                    res.setArtist(line.split("artist: ")[1]);
                } else if (line.contains("title")) {
                    res.setTitle(line.split("title: ")[1]);
                } else if (line.contains("year")) {
                    res.setYear(line.split("year: ")[1]);
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
