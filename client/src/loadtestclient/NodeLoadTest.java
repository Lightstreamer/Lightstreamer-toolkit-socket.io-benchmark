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
package loadtestclient;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import loadtestclient.client.BasicWSClient;





public class NodeLoadTest {

    
    private static int INCREASE_CLIENTS = 100;
    private static int TEST_DURATION_MS = 60000;
    private static int CONNECT_BATCH_SIZE = 100;
    private static int CONNECT_BATCH_INTERVAL = 1000;
    
    private static int MAX_DELAY_MILLIS = 5000;

    private static String SERVER_URL = "localhost:8080";
              
    private static String FILE_PATH = "results.log";
    private static boolean TAB_LOG = true;
    
    private static String CLOSED_CLIENT_POLICY = "IGNORE";
    
    private static int currentClients = 0;
    private static int expectingClients = 0;
    private static int disconnectedClients = 0;
    
    private static Statistics stats = null;
    
    private static boolean useIO = false;
    
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        
        if (args.length < 1) {
            return;
        }
        useIO = args[0].equals("io");
        String type = useIO ? "io" : "ls";
        
        if (args.length > 1) {

            Properties props = new Properties();
            try {
                props.load(new FileInputStream(new File(args[1])));
            } catch (IOException e) {
            }
            
            INCREASE_CLIENTS = Integer.parseInt(props.getProperty("INCREASE_CLIENTS"));
            TEST_DURATION_MS = Integer.parseInt(props.getProperty("TEST_DURATION_MS"));
            CONNECT_BATCH_SIZE = Integer.parseInt(props.getProperty("CONNECT_BATCH_SIZE"));
            CONNECT_BATCH_INTERVAL = Integer.parseInt(props.getProperty("CONNECT_BATCH_INTERVAL"));
            
            MAX_DELAY_MILLIS = Integer.parseInt(props.getProperty("MAX_DELAY_MILLIS"));
            
            String host = props.getProperty("HOST");
            int port = Integer.parseInt(props.getProperty("PORT"));
            SERVER_URL = host+":"+port;    
            
            FILE_PATH = props.getProperty("FILE_PATH");
            
            TAB_LOG = props.getProperty("TAB_LOG").equals("true");
            
            CLOSED_CLIENT_POLICY = props.getProperty("CLOSED_CLIENT_POLICY");
            
        }
        
        
        
        CListener currentTestListener = new CListener(); 
        
        while(true) {
            expectingClients += INCREASE_CLIENTS;
                     
            if (stats != null) {
                stats.onTestComplete();
            }
            
            System.out.println("Launching "+INCREASE_CLIENTS+ " new clients");
            
            stats = new Statistics(type,expectingClients,TAB_LOG,FILE_PATH,MAX_DELAY_MILLIS);
    
            //connect the clients CONNECT_BATCH_SIZE at a time, then wait CONNECT_BATCH_INTERVAL
            for (int i = 0; i < INCREASE_CLIENTS;) {
                for (int b = 0; b < CONNECT_BATCH_SIZE && i < INCREASE_CLIENTS; b++,i++) {
                    try {
                        
                        new BasicWSClient(SERVER_URL,currentTestListener,useIO);
                     
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    
                }
                try {
                    Thread.sleep(CONNECT_BATCH_INTERVAL);
                } catch (InterruptedException e) {
                }
            }
            
            try {
                Thread.sleep(TEST_DURATION_MS);
            } catch (InterruptedException e) {
            }
        }
        
        
        
    }
    
    
    private static class CListener implements ClientListener {
        
              public CListener() {
            
        }
        
        
        
        public synchronized void onOpen() {

            currentClients++;
            if (currentClients%50 == 0) {
                System.out.println("Connected " + currentClients + " clients");
            }
            
            if (currentClients == expectingClients) {
                System.out.println("New clients connected. Currently a total of " + currentClients + " clients connected");
                stats.onRampUpEnd();
            }
        }

        public void onError() {
            System.out.println("GOT ERROR!");
        }

        public void onClose() {
            synchronized(this) {
                disconnectedClients++;
                System.out.println("WARNING disconnected client! Currently " + disconnectedClients + " disconnected clients");
            
                if (CLOSED_CLIENT_POLICY.equals("ABORT")) {
                    System.exit(1);
                    return;
                } else if (CLOSED_CLIENT_POLICY.equals("RETRY")) {
                    currentClients--;
                    //will launch another client from outside the synchornized block
                } else { //IGNORE do nothing
                    return;
                }
            }
            try {
                new BasicWSClient(SERVER_URL,this,useIO);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onMessage(String stringTimestamp) {
            long timestamp = Long.parseLong(stringTimestamp);
            long delay = new Date().getTime() - timestamp;
            
            //the diff should always fit an int... or we have very very very very big 
            //performances issues :) (very big)
           stats.onDelay((int) delay);
            
        }

 
    }

}
