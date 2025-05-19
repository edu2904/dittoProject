package org.example;


import org.example.Client.DittoClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

public class DittoEventActionHandler {
    private final Logger logger = LoggerFactory.getLogger(DittoEventActionHandler.class);


    public DittoEventActionHandler(){

    }

    public void createEventLogging(String thingID, String subject){
        Runnable task = () -> {
            try {
                URL url = new URL("http://localhost:8080/api/2/things/"+ thingID + "/outbox/messages/" + subject);
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
                        };
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
                        };
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

/*
    public void createLogging() {
        String url = "http://localhost:8080/api/2/things/mytest:LKW-1/outbox/messages/showStatus";
        OkHttpClient client = new OkHttpClient();

        String credentials = "ditto:ditto";
        String basicAuth = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());

        Request request = new Request.Builder()
                .url(url)
                .header("Accept", "text/event-stream")
                .header("Authorization", basicAuth)
                .build();
        EventSourceListener listener = new EventSourceListener() {
            @Override
            public void onEvent(@NotNull EventSource source, String id, String type, @NotNull String data) {
                System.out.println("Event erhalten: " + data);
                writeToFile(data);
            }

            @Override
            public void onFailure(@NotNull EventSource source, Throwable t, Response response) {
                if (t != null) {
                    System.err.println("Fehler beim Empfangen: " + t.getMessage());
                }
            }
        };

        EventSources.createFactory(client).newEventSource(request, listener);
    }






    private static void writeToFile(String data) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("ditto-events.log", true))) {
            writer.write(data);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Fehler beim Schreiben in Datei: " + e.getMessage());
        }
    }

 */

}
