package org.example;

import org.eclipse.ditto.client.DittoClient;
import org.example.Client.DittoClientBuilder;

import java.io.IOException;
import java.util.concurrent.ExecutionException;


public class Main {
    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {

        DittoClientBuilder dittoClientBuilder = new DittoClientBuilder();
        DittoClient dittoClient = dittoClientBuilder.getDittoClient();
        ThingHandler lkw = new ThingHandler();
        String officialWoTExampleUrl = "https://eclipse-ditto.github.io/ditto-examples/wot/models/floor-lamp-1.0.0.tm.jsonld";
        String lkwPolicy = "https://raw.githubusercontent.com/edu2904/wotfiles/refs/heads/main/lkwpolicy";
        String LKWWOT = "https://raw.githubusercontent.com/edu2904/wotfiles/refs/heads/main/LKW/lkwMain?cb=" + System.currentTimeMillis();
        String policyID = lkw.getPolicyFromURL(lkwPolicy).get();
       // System.out.println(lkw.thingExist(dittoClient, String.valueOf(ThingId.of("mytest:LKW-10000000000000000000000000000000000000000"))).get());
        if(!(lkw.policyExists(dittoClient, policyID).get())){
            lkw.createTwinAndPolicy(dittoClient, LKWWOT, lkwPolicy);
        }else {
            lkw.deleteThingandPolicy(dittoClient, lkw.getThingId().toString(), policyID);
            lkw.createTwinAndPolicy(dittoClient, LKWWOT, lkwPolicy);
        }
    }
}