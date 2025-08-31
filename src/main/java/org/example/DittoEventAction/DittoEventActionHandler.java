package org.example.DittoEventAction;

/*
import org.example.util.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.*;

public class DittoEventActionHandler {
    private final Logger logger = LoggerFactory.getLogger(DittoEventActionHandler.class);
    private final ExecutorService executorService = Executors.newCachedThreadPool();


    private final Map<String, Future<?>> activeTasks = new ConcurrentHashMap<>();




    public DittoEventActionHandler(){
    }

    public void stopEventActionHandler(String thingId){
        Future<?> future = activeTasks.remove(thingId);
        if(future != null){
            future.cancel(true);
            logger.debug("Stopped logger for {}", thingId);


        }
    }

    public void createEventLoggingForAttribute(String thingID, String subject) {
        URL url = null;
        try {
            url = new URL("http://localhost:8080/api/2/things/"+ thingID + "/outbox/messages/" + subject);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        createActionEventLogging(thingID, url, "Event");
    }
    public void  createEventLoggingForFeature(String thingID, String subject, String featureID){
        URL url = null;
        try {
            url = new URL("http://localhost:8080/api/2/things/"+ thingID + "/features/"+ featureID + "/outbox/messages/" + subject);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        createActionEventLogging(thingID, url, "Event");
    }
    public void createActionLoggingForAttribute(String thingID, String subject) {
        URL url = null;
        try {
            url = new URL("http://localhost:8080/api/2/things/"+ thingID + "/inbox/messages/" + subject);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        createActionEventLogging(thingID, url, "Action");
    }
    public void  createActionLoggingForFeature(String thingID, String subject, String featureID){
        URL url = null;
        try {
            url = new URL("http://localhost:8080/api/2/things/"+ thingID + "/features/"+ featureID + "/inbox/messages/" + subject);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        createActionEventLogging(thingID, url, "Action");
    }

    public void createActionEventLogging(String thingID, URL url, String sendType) {
        stopEventActionHandler(thingID);
        try {


            Future<?> future = executorService.submit(() -> {
                HttpURLConnection httpURLConnection = null;
                try {
                    httpURLConnection = (HttpURLConnection) url.openConnection();

                    String username = Config.DITTO_USERNAME;
                    String password = Config.DITTO_PASSWORD;
                    String auth = username + ":" + password;
                    String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
                    httpURLConnection.setRequestProperty("Authorization", "Basic " + encodedAuth);
                    httpURLConnection.setRequestMethod("GET");
                    httpURLConnection.setRequestProperty("Accept", "text/event-stream");
                    httpURLConnection.setDoInput(true);
                    httpURLConnection.setReadTimeout(10000);




                    BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                    String inputLine;

                    //BufferedWriter fileWriter = new BufferedWriter(new FileWriter("messages.txt", true));


                    while ((inputLine = in.readLine()) != null) {
                        if (inputLine.startsWith("data:")) {
                            String data = inputLine.substring(5).trim();

                            if (!data.isEmpty()) {
                                logger.info("Received {}: {} from {}", sendType, inputLine, thingID);
                                //fileWriter.write("New Event: " + inputLine + "\n");
                                //fileWriter.newLine();
                            }
                        }
                    }
                    in.close();
                    //fileWriter.close();
                } catch (IOException e) {
                    if (Thread.currentThread().isInterrupted()) {
                        logger.info("Thing ID {} was interrupted", thingID);
                    } else {
                        logger.info("Error for {}: {}", thingID, e.getMessage());
                    }

                } finally {
                    if (httpURLConnection != null) {
                        httpURLConnection.disconnect();
                        logger.info("Connection closed for {}", thingID);
                    }
                }
            });
            activeTasks.put(thingID, future);
        }catch (Exception e){
            logger.info("ERERERERERRERER {}", e.getMessage());
        }
    }
}

 */
