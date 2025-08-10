package org.example.process;

import org.eclipse.ditto.client.DittoClient;

import java.util.Map;
import java.util.function.Function;

public abstract class AbstractDittoProcess {
    protected final DittoClient dittoClient;
    protected final String cosumerId;

    public AbstractDittoProcess(DittoClient dittoClient, String cosumerId){
        this.dittoClient = dittoClient;
        this.cosumerId = cosumerId;
    }

    public void processtest(){

    }
}
