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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class EventDispatcher {
    
    public static final int CLOSE = 5;
    public static final int OPEN = 6;
    public static final int ERROR = 7;
    public static final int MESSAGE = 8;
    
    private static ExecutorService eventsThread = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "Event dispatcher thread");
            t.setDaemon(true);
            return t;
        }
    });
    
    private Queue<Event> queue = new ConcurrentLinkedQueue<Event>();

    private BasicWSClient client;
    public EventDispatcher(BasicWSClient basicWSClient) {
        this.client = basicWSClient;
    }
    
    public void createEvent(int what, String text) {
        queue.add(new Event(what,text));
        eventsThread.execute(new Dispatcher());
    }
    
    public void handleEvent(Event event) {
        if (event == null) {
            System.out.println("UNEXPECTED");
            return;
        }
        
        if (event.what == OPEN) {
            
            this.client.forwardOpen();
            
        } else if (event.what == CLOSE) {
        
            this.client.forwardClose();
            
        } else if (event.what == MESSAGE) {
            this.client.forwardMessage(event.text);
            
        } else /*if (event.what == ERROR)*/ {
            
            this.client.forwardError();
            
        }
        
    }
   
    private class Event {
        public int what;
        public String text = null;
        public Event(int what, String text) {
            this.what = what;
            this.text = text;
        }
    }
    
    private class Dispatcher extends Thread { 
        
        public void run() {
           //synchronize to keep the order 
           synchronized(queue) {
                handleEvent(queue.poll());
           }
            
        }
        
    }
    
}
