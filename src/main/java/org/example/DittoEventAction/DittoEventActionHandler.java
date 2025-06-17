package org.example.DittoEventAction;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;

public class DittoEventActionHandler {
    private final Logger logger = LoggerFactory.getLogger(DittoEventActionHandler.class);




    public DittoEventActionHandler(){

    }

    public void createEventLoggingForAttribute(String thingID, String subject) {
        URL url = null;
        try {
            url = new URL("http://localhost:8080/api/2/things/"+ thingID + "/outbox/messages/" + subject);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        createEventLogging(thingID, url);
    }
    public void  createEventLoggingForFeature(String thingID, String subject, String featureID){
        URL url = null;
        try {
            url = new URL("http://localhost:8080/api/2/things/"+ thingID + "/features/"+ featureID + "/outbox/messages/" + subject);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        createEventLogging(thingID, url);
    }

    public void createEventLogging(String thingID, URL url){
        Runnable task = () -> {
            try {
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                String username = "ditto";
                String password = "ditto";
                String auth = username + ":" + password;
                String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
                httpURLConnection.setRequestProperty("Authorization", "Basic " + encodedAuth);
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setRequestProperty("Accept", "text/event-stream");
                httpURLConnection.setDoInput(true);


                BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                String inputLine;

                BufferedWriter fileWriter = new BufferedWriter(new FileWriter("received_sse_messages.txt", true));


                while ((inputLine = in.readLine()) != null) {
                    if(inputLine.startsWith("data:")) {
                        String data = inputLine.substring(5).trim();

                        if(!data.isEmpty()) {

                                    logger.info("Received Event: {} for {}", inputLine, thingID);
                                    fileWriter.write("New Event: " + inputLine + "\n");
                                    fileWriter.newLine();
                                }
                    }
                }

                in.close();
                fileWriter.close();


            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
        new Thread(task).start();
    }
    public void createActionLogging(String thingID, String subject){
        Runnable task2 = () -> {
            try {
                URL url = new URL("http://localhost:8080/api/2/things/"+ thingID + "/inbox/messages/" + subject);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                String username = "ditto";
                String password = "ditto";
                String auth = username + ":" + password;
                String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
                httpURLConnection.setRequestProperty("Authorization", "Basic " + encodedAuth);
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setRequestProperty("Accept", "text/event-stream");
                httpURLConnection.setDoInput(true);


                BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                String inputLine;

                BufferedWriter fileWriter = new BufferedWriter(new FileWriter("received_sse_messages.txt", true));


                while ((inputLine = in.readLine()) != null) {
                    if(inputLine.startsWith("data:")) {
                        String data = inputLine.substring(5).trim();

                        if(!data.isEmpty()) {

                                     logger.info("Received Action: {} for {}", inputLine, thingID);
                                     fileWriter.write("New Action: " + inputLine + "\n");
                                     fileWriter.newLine();
                                 }


                    }
                }

                in.close();
                fileWriter.close();


            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
        new Thread(task2).start();
    }


}
