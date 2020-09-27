package esignsenddraftfolder;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;

public class ESignTestSDK {
    public static String simpleDraftSend(String folderId) {
        HttpResponse<String> response = Unirest.post(System.getenv("SEND_DRAFT_ENDPOINT"))
                .header("Authorization", retrieveAccessToken())
                .header("Content-Type", "application/json")
                .body("{\"folderId\":"+folderId+"}")
                .asString();

        return response.getBody();
    }

    public static String retrieveAccessToken() {
        HttpResponse<String> response = Unirest.post(System.getenv("TOKEN_ENDPOINT"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .field("grant_type", "client_credentials")
                .field("client_id", System.getenv("ESIGN_CLIENT_ID"))
                .field("client_secret", System.getenv("ESIGN_CLIENT_SECRET"))
                .field("scope", "read-write")
                .asString();

        JsonObject myObject = JsonParser.parseString(response.getBody()).getAsJsonObject();

        return ("Bearer " + myObject.get("access_token").getAsString());
    }
}
