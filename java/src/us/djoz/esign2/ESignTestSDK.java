package us.djoz.esign2;

import com.google.gson.*;
import io.github.cdimascio.dotenv.Dotenv;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ESignTestSDK {
    static Dotenv dotenv = Dotenv.configure()
            .directory("./..")
            .ignoreIfMalformed()
            .ignoreIfMissing()
            .load();
    static String folderEndpoint = dotenv.get("FOLDER_ENDPOINT");
    static String tokenEndpoint = dotenv.get("TOKEN_ENDPOINT");
    static String clientID = dotenv.get("ESIGN_CLIENT_ID");
    static String clientSecret = dotenv.get("ESIGN_CLIENT_SECRET");

    public static String createFolder(String data, List<String> filePaths) {
        ArrayList<File> filez = new ArrayList<File>();
        for (String path: filePaths) {
            filez.add(new File(path));
        }

        HttpResponse<String> response = Unirest.post(folderEndpoint)
                .header("Authorization", retrieveAccessToken())
                .field("data", data)
                .field("file", filez)
                .asString();

        for (String path: filePaths) {
            try
            {
                Files.deleteIfExists(Path.of(path));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return response.getBody();
    }

    public static String simpleDraftSend(String folderId) {
        HttpResponse<String> response = Unirest.post("https://www.esigngenie.com/esign/api/folders/sendDraftFolder")
                .header("Authorization", retrieveAccessToken())
                .header("Content-Type", "application/json")
                .body("{\"folderId\":"+folderId+"}")
                .asString();

        return response.getBody();
    }

    public static String retrieveAccessToken() {
        HttpResponse<String> response = Unirest.post(tokenEndpoint)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .field("grant_type", "client_credentials")
                .field("client_id", clientID)
                .field("client_secret", clientSecret)
                .field("scope", "read-write")
                .asString();

        JsonObject myObject = JsonParser.parseString(response.getBody()).getAsJsonObject();

        return ("Bearer " + myObject.get("access_token").getAsString());
    }
}
