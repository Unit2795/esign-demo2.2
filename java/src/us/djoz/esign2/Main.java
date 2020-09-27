package us.djoz.esign2;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import spark.Request;
import spark.Response;
import spark.utils.IOUtils;

import static spark.Spark.*;

public class Main {
    public static void main(String[] args) {
        File uploadDir = new File("C:/java-upload/tmp");
        uploadDir.mkdir();
        staticFiles.externalLocation("C:/java-upload/tmp");

        options("/*",
                (request, response) -> {

                    String accessControlRequestHeaders = request
                            .headers("Access-Control-Request-Headers");
                    if (accessControlRequestHeaders != null) {
                        response.header("Access-Control-Allow-Headers",
                                accessControlRequestHeaders);
                    }

                    String accessControlRequestMethod = request
                            .headers("Access-Control-Request-Method");
                    if (accessControlRequestMethod != null) {
                        response.header("Access-Control-Allow-Methods",
                                accessControlRequestMethod);
                    }

                    return "OK";
                });

        before((request, response) -> response.header("Access-Control-Allow-Origin", "*"));

        post("/demoform2/foldersend", (request, result) -> ESignTestSDK.simpleDraftSend(request.queryParams("folderid")));

        post("/demoform2", (request, result) -> {
            MultipartConfigElement multipartConfigElement = new MultipartConfigElement("C:/java-upload/tmp");
            request.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);

            List<String> tempFiles = new ArrayList<String>();

            for (Part item : request.raw().getParts()) {
                if (item.getContentType() != null && item.getContentType().equals("application/pdf")) {
                    String name = (uploadDir.toPath() + "/" + item.getSubmittedFileName());

                    Path tempFile = Files.createFile(Path.of(name));

                    try (InputStream is = item.getInputStream()) {
                        Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
                    }

                    tempFiles.add(name);
                }
            }

            StringWriter writer = new StringWriter();
            IOUtils.copy(request.raw().getPart("parties").getInputStream(), writer);
            String theString = writer.toString();

            JsonArray myObject = JsonParser.parseString(theString).getAsJsonArray();
            JsonArray parties = new JsonArray();

            int iteration = 1;
            for (Iterator i = myObject.iterator(); i.hasNext(); iteration++)
            {
                JsonObject coolObject = JsonParser.parseString(String.valueOf(i.next())).getAsJsonObject();
                JsonObject newObject = new JsonObject();
                newObject.add("firstName", coolObject.get("fname"));
                newObject.add("lastName", coolObject.get("lname"));
                newObject.add("emailId", coolObject.get("email"));
                newObject.addProperty("permission", "FILL_FIELDS_AND_SIGN");
                newObject.addProperty("workflowSequence", iteration);
                newObject.addProperty("sequence", iteration);
                newObject.addProperty("allowNameChange", false);

                parties.add(newObject);
            }

            StringWriter writer2 = new StringWriter();
            IOUtils.copy(request.raw().getPart("type").getInputStream(), writer2);
            String typeString = writer2.toString();

            boolean isPreview = typeString.equals("preview");
            boolean isSending = typeString.equals("send");
            boolean isSigning = typeString.equals("sign");

            String sendString = "{\"folderName\":\"eSign Genie API Demo Documents\",\"parties\":%s,\"processTextTags\":true,\"processAcroFields\":true,\"signInSequence\":false,\"inPersonEnable\":false,\"themeColor\":\"#003C1C\",\"createEmbeddedSendingSession\":%s,\"sendNow\":%s,\"createEmbeddedSigningSession\":%s,\"createEmbeddedSigningSessionForAllParties\":%s}";
            String populatedSendString = String.format(
                    sendString,
                    parties,
                    isPreview,
                    isSending,
                    isSigning,
                    isSigning
            );

            return ESignTestSDK.createFolder(populatedSendString, tempFiles);
        });
    }
}