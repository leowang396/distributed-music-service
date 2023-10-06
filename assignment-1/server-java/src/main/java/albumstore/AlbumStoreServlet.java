package albumstore;

import com.google.gson.Gson;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;

import java.io.IOException;

@WebServlet(name="AlbumStoreServlet", value="/albums/*")
@MultipartConfig
public class AlbumStoreServlet extends HttpServlet {
    private static final String POST_PROFILE_FIELD = "profile";
    private static final String POST_IMAGE_FILE_FIELD = "image";
    private static final int GET_URL_PARTS = 2;

    private class ResponseMessage {
        private String msg;
    }

    private class AlbumInfo {
        private String artist;
        private String title;
        private String year;
    }

    private class ImageMetaData {
        private String albumID;
        private String imageSize;
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
            res.msg = "Invalid GET URL";
            response.getOutputStream().print(gson.toJson(res));
            return;
        }

        String[] pathInfoParts = pathInfo.split("/");
        if (pathInfoParts.length != GET_URL_PARTS) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            ResponseMessage res = new ResponseMessage();
            res.msg = "Invalid GET URL";
            response.getOutputStream().print(gson.toJson(res));
            return;
        }
        // TODO: Get the actual album information.
//        String albumID = pathInfoParts[1];
        String artist = "Sex Pistols";
        String title = "Never Mind The Bollocks!";
        int year = 1977;

        // Prepares HTTP response.
        response.setStatus(HttpServletResponse.SC_OK);
        AlbumInfo res = new AlbumInfo();
        res.artist = artist;
        res.title = title;
        res.year = String.valueOf(year);
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
            res.msg = "Invalid POST URL";
            response.getOutputStream().print(gson.toJson(res));
            return;
        }

        // Retrieves the image file.
        Part imageFilePart = request.getPart(POST_IMAGE_FILE_FIELD);
        if (imageFilePart == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            ResponseMessage res = new ResponseMessage();
            res.msg = "POST request body missing image field";
            response.getOutputStream().print(gson.toJson(res));
            return;
        }
        long imageSize = imageFilePart.getSize();
        // TODO: Do more work with image file.
        // Fix that MSIE browser incorrectly sends the full file path along the name.
//        String fileName = Paths.get(imageFilePart.getSubmittedFileName()).getFileName().toString();
//        InputStream fileContent = imageFilePart.getInputStream();

        // TODO: Figure out why SwaggerHub API and Postman generates different request data.
//        // Retrieves the profile field.
//        Part albumProfilePart = request.getPart(POST_PROFILE_FIELD);
//        if (albumProfilePart == null) {
////            System.out.println("POST request body missing profile field");
//            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
//            ResponseMessage res = new ResponseMessage();
//            res.msg = "POST request body missing profile field";
//            response.getOutputStream().print(gson.toJson(res));
//            return;
//        }
////        System.out.println("POST request body has profile field");
        // TODO: Do some work with album info.
        int albumID = 0;
//        byte[] input = new byte[200];
//        albumProfilePart.getInputStream().read(input);
//        System.out.println(new String(input, StandardCharsets.UTF_8));
//        try {
//            AlbumInfo newAlbumInfo = gson.fromJson(
//                new BufferedReader(new InputStreamReader(albumProfilePart.getInputStream(), "UTF-8")),
//                AlbumInfo.class);
//            System.out.println(newAlbumInfo);
//        } catch (JsonSyntaxException e) {
//            System.out.println(e.getMessage());
//            e.printStackTrace();
//        }
//        System.out.println(gson.fromJson(String.valueOf(input), AlbumInfo.class));
//        gson.fromJson(String.valueOf(input), AlbumInfo.class);
//        String profileString = request.getParameter(POST_PROFILE_FIELD);
//        System.out.println(profileString);
//        AlbumInfo newAlbumInfo = gson.fromJson(
//                new BufferedReader(new InputStreamReader(albumProfilePart.getInputStream(), "UTF-8")),
//                AlbumInfo.class);
//        System.out.println(newAlbumInfo);

        // Prepares HTTP response.
        response.setStatus(HttpServletResponse.SC_OK);
        ImageMetaData res = new ImageMetaData();
        res.albumID = String.valueOf(albumID);
        res.imageSize = String.valueOf(imageSize);
        response.getOutputStream().print(gson.toJson(res));
        response.getOutputStream().flush();
    }
}
