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
package loadtestclient.lightstreamer;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class ProtocolHandler {
    
    private static final int OFF = 1;
    private static final int WAITING_SUB = 3;
    private static final int ON = 2;
    private static final int END = 4;
    
    private static final String HEADER_END = "// END OF HEADER";
    private static final String START_STRING = "start('";
    
    private WebSocketConnection networkHandler;
    private EventDispatcher dispatcher;
    
    private int nextProg = 1;
    private long keepaliveMillis;
    
    private int status = OFF;
    private String session = null;
    private String adapter;
    private String previous = null;

    public ProtocolHandler(BasicWSClient basicWSClient,String adapter, long keepaliveMillis) {
        this.dispatcher = new EventDispatcher(basicWSClient);
        //let's encode it once
        try {
            this.adapter = URLEncoder.encode(adapter,"UTF-8");
        } catch (UnsupportedEncodingException e) {
        }
        this.keepaliveMillis = keepaliveMillis;
    }
    
    public void setNetworkHandler(WebSocketConnection networkHandler) {
        this.networkHandler = networkHandler;
        
    }
    
    private void abortGateway() {
        if (this.status == ON || this.status == OFF) {
            this.dispatcher.createEvent(EventDispatcher.ERROR, null);
            
            this.closeGateway();
            
            networkHandler.close();
         }
    }
    
    private void closeGateway() {
        if (this.status == ON || this.status == OFF) {
            this.status = END;

            this.dispatcher.createEvent(EventDispatcher.CLOSE, null);
        }
    }
    
    private void openGateway() {
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


    public synchronized void handleMessage(String text) {
      
        if (this.status == OFF) {
           int pointer = 0;
           //first frame should contain header end and start command
         
           if ((pointer = text.indexOf(HEADER_END)) > -1) {
               pointer += HEADER_END.length();

               pointer = text.indexOf(START_STRING,pointer);
               pointer += START_STRING.length();
               
               //let's encode it once
               try {
                   this.session = URLEncoder.encode(text.substring(pointer,text.indexOf("'",pointer)),"UTF-8");
               } catch (UnsupportedEncodingException e) {
               }
               
               this.status = WAITING_SUB;
               this.openGateway();
               this.sendSubscription();
           }
           
       } else {
           
           //I need to handle:
           // d(1,1,1); <- update 
           // d(1,1,'data'); <- update 
           // c(4,1,"socket",0,0); <- message ack
           // c(5,1,"socket",0,0); <- message ok
           // r( <- lost updates
           // error(errorCode, msgProg, "MSGsocket", "something"); <-- message error

           
         //Let's keep it simple:
           // assume no call can contain ); in its internal strings
           
           
           StringBuffer command = new StringBuffer(5);
           int pointer = 0;

           while(pointer < text.length() && this.status != END) {
               
               char ch = text.charAt(pointer);
               if (ch == '(') {
                   
                   if (command.toString().equals("d")) {
                       
                       //update
                       this.readUpdate(text,pointer);
                       
                       
                   } else if (command.toString().equals("c")) {
                       
                       //message ack or ok
                       this.readMessageNotify(text,pointer);
                       
                       
                   } else if (command.toString().equals("error") || command.toString().equals("r")) {
                   
                       //error in message handling
                       this.readMessageError(text,pointer);
                       
                   } //else we don't care
                   
                   
                   pointer = text.indexOf(");",pointer)+2;
                   command.setLength(0);
                   
                   
               } else {
                   if (ch >= 97 && ch <= 122) {
                       command.append(ch);
                   }
                   pointer++;
               }
           }
           
       }
      
    }
   
    private void readMessageError(String text, int pointer) {
        //again, we actually don't care what kind of error this is, just abort
        this.abortGateway();
    }

    private void readMessageNotify(String text, int pointer) {
        //we actually don't care
    }

    private void readUpdate(String text, int pointer) {
    	//actually we don't care, we only want to create load on the server 
    	
    	
    	
    	
        //skip 1,1,
        
        pointer += 5;
        
        char ch = text.charAt(pointer);
        
        String update = null;

        if (ch == '1') {
            //update unchanged
            update = previous;
            this.dispatcher.createEvent(EventDispatcher.MESSAGE, update);
            return;
           
            
        } else { //'
            ch = text.charAt(pointer+1);
            int end = text.indexOf("');",pointer);
            if (ch != '#') {
                update = text.substring(pointer+1,end);
            }
            
            pointer = end+2;
            previous = update;
        }
        
        
        //then the update
        if (update != null) {
            this.dispatcher.createEvent(EventDispatcher.MESSAGE, update); 
        }
        
    }
    

    public synchronized void sendMessage(String message) {
        try {
            this.networkHandler.send("msg\r\nLS_session="+this.session+"&LS_message="+URLEncoder.encode(message,"UTF-8")+"&LS_sequence=socket&LS_outcome=&LS_ack=&LS_msg_prog="+(this.nextProg++));
        } catch (UnsupportedEncodingException e) {
            
        }
    }
    
    private void sendSubscription() {
        this.networkHandler.send("control\r\nLS_session="+this.session+"&LS_table=1&LS_op=add&LS_mode=RAW&LS_id=timestamp&LS_schema=message&LS_max_frequency=unfiltered");
    }
    
    public void createSession() {
        this.networkHandler.send("create_session\r\nLS_client_version=6.0&LS_adapter_set="+this.adapter + (this.keepaliveMillis > 0 ? "&LS_keepalive_millis="+this.keepaliveMillis : "") );
    }

    
    

}
