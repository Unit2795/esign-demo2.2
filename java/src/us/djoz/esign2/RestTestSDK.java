package us.djoz.esign2;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kong.unirest.Unirest;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class RestTestSDK {
    enum ContentType {
        URL_ENCODED {
            public String toString() {
                return "application/x-www-form-urlencoded";
            }
        },
        MULTIPART_FORM {
            public String toString() {
                return "multipart/form-data";
            }
        },
        JSON {
            public String toString() {
                return "application/json";
            }
        },
    }

    public static String Post(String targetURL, ContentType contentType, String parameters, String[] headers, String[] filePaths)
    {
        Unirest.config().socketTimeout(0).connectTimeout(0);




        URL url = null;
        String responseString = "";

        try
        {
            url = new URL(targetURL);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Content-Type", contentType.toString());

            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(parameters);

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Failed : HTTP error code : " + connection.getResponseCode());
            }
            BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getInputStream())
            );

            String output;
            StringBuffer response = new StringBuffer();

            while((output = br.readLine()) != null) {
                response.append(output);
            }

            responseString = response.toString();

            connection.disconnect();

        } catch (MalformedURLException e)
        {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return responseString;
    }
}
