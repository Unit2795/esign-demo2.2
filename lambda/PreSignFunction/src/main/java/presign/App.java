package presign;

import java.net.URL;
import java.util.*;

import com.amazonaws.HttpMethod;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kong.unirest.*;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    public static Boolean validateCaptcha(String captchaResponse) {
        HttpResponse<String> captchaPost = Unirest.post(System.getenv("CAPTCHA_ENDPOINT"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Origin", "http://localhost:4200")
                .field("secret", System.getenv("CAPTCHA_SECRET"))
                .field("response", captchaResponse)
                .asString();

        JsonObject myObject = JsonParser.parseString(captchaPost.getBody()).getAsJsonObject();

        return myObject.get("success").getAsBoolean();
    }

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {

        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Origin", "*");


        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent().withHeaders(headers);

        if (!validateCaptcha(input.getHeaders().get("Authorization")))
        {
            return response
                    .withStatusCode(401);
        }


        String[] arr = input.getBody().split("=");
        int fileCount = Integer.parseInt(arr[1]);

        if (fileCount > 3)
        {
            return response
                    .withStatusCode(400);
        }

        JsonArray presignedURLs = new JsonArray();
        JsonArray fileNames = new JsonArray();

        Regions clientRegion = Regions.US_EAST_1;
        String bucketName = "esign2-demo-tempfiles";
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(clientRegion)
                .build();

        java.util.Date expiration = new java.util.Date();
        long expTimeMillis = expiration.getTime();
        int expMinutes = 5;
        expTimeMillis += (1000 * 60) * expMinutes;
        expiration.setTime(expTimeMillis);
        for (int i = 0; i < fileCount; i++)
        {
            String folderName = UUID.randomUUID().toString() + ".pdf";


            GeneratePresignedUrlRequest preSignRequest = new GeneratePresignedUrlRequest(bucketName, folderName)
                    .withMethod(HttpMethod.PUT)
                    .withExpiration(expiration);

            URL presignedURL = s3Client.generatePresignedUrl(preSignRequest);

            presignedURLs.add(String.valueOf(presignedURL));
            fileNames.add(folderName);
        }

        JsonObject returnJSON = new JsonObject();

        returnJSON.add("urls", presignedURLs);
        returnJSON.add("files", fileNames);

        return response
                .withStatusCode(200)
                .withBody(String.valueOf(returnJSON));
    }
}
