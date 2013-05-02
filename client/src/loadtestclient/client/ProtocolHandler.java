/*
Copyright 2013 Weswit s.r.l.

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


public abstract class ProtocolHandler {
    
    protected EventDispatcher dispatcher;
    protected WebSocketConnection networkHandler;
    
    protected static final int OFF = 1;
    protected static final int WAITING_SUB = 3;
    protected static final int ON = 2;
    protected static final int END = 4;
  
    protected int status = OFF;
    protected String server;
    
    public ProtocolHandler(BasicWSClient basicWSClient, String server) {
        this.dispatcher = new EventDispatcher(basicWSClient);
        
        this.networkHandler = new WebSocketConnection();
        this.networkHandler.setMessageListener(this);
        
        this.server = server;
      
        
    }
    
    
    protected void abortGateway() {
        if (this.status == ON || this.status == OFF) {
            this.dispatcher.createEvent(EventDispatcher.ERROR, null);
            
            this.closeGateway();
            
            networkHandler.close();
         }
    }
    
    protected void closeGateway() {
        if (this.status == ON || this.status == OFF) {
            this.status = END;

            this.dispatcher.createEvent(EventDispatcher.CLOSE, null);
        }
    }
    
    protected void openGateway() {
        if (this.status != ON) {
            this.status = ON;

            this.dispatcher.createEvent(EventDispatcher.OPEN, null);
        }
    }
    
    
    public synchronized void handleClose() {
        this.closeGateway();
    }
    
    public synchronized void handleException() {
        this.abortGateway();
    }
    
    public abstract void open();

    public void close() {
        this.networkHandler.close();
    }


    public abstract void handleMessage(String text);
    public abstract void sendMessage(String message);
    public abstract void handleSocketOpen();
    
    

}
