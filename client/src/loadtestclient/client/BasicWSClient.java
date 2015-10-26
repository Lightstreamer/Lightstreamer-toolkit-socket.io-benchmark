/*
Copyright (c) Lightstreamer Srl

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/   
package loadtestclient.client;


import loadtestclient.ClientListener;

public class BasicWSClient implements WSAdapter {

     
    //Fake implementation of a Lightstreamer client over websocket. Here some of its limitations:
    //  Works on the js protocol
    //  Does not handle any kind of exception
    //  No stream-sense 
    //  No recoveries of any kind
    //  Fixed subscription (targeted to the LSWebSocket client
    
    private ClientListener listener;
    private ProtocolHandler protocolHandler;
    
    public BasicWSClient(String server, ClientListener listener2, boolean useIO) throws Exception {
        this.listener = listener2;
        
        if (useIO) {
            this.protocolHandler = new ProtocolHandlerIO(this,server);//escapes!
        } else {
            this.protocolHandler = new ProtocolHandlerLS(this,server,"NODEJS_PERF_TEST",1000000);//escapes!
        }
        
        
        this.protocolHandler.open();
    }
    
    public void send(String message) {
        this.protocolHandler.sendMessage(message);
    }
    
    public void close() {
        this.protocolHandler.close();
    }
    
    void forwardClose() {
        this.listener.onClose();
    }

    void forwardOpen() {
        this.listener.onOpen();
    }
    
    void forwardError() {
        this.listener.onError();
    }

    public void forwardMessage(String text) {
        this.listener.onMessage(text);
    }
    
    
}
