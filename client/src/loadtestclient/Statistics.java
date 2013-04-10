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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;




public class Statistics {

    private String testName;
    private String fileName;
    
    private long[] delays;
    private long totUpdates = 0;
    private long totDelays = 0;
    
    //max delay ever 
    private int maxDelayFound = Integer.MIN_VALUE;
    //min delay ever 
    private int minDelayFound = Integer.MAX_VALUE;
    
    private ConcurrentLinkedQueue<Integer> queue = new ConcurrentLinkedQueue<Integer>();
    
    private long startTime;
    private boolean enabled = false;
    
    //string buffer used to compone log report
    StringBuffer results = new StringBuffer();
    
    private boolean alive = true;
    private String type;
    private int clientsNum;
    private boolean tabLog;
    
    
    public Statistics(String type, int clientsNum, boolean tabLog, String fileName, int maxDelayMillis) {
        this.testName = "Test " + type + " " + clientsNum;
        this.type = type;
        this.clientsNum = clientsNum;
        this.tabLog = tabLog;
        this.fileName = fileName;
        
        //in case delays > conf.maxDelayMillis are received then the code will fail and conf.maxDelayMillis have to be changed
        this.delays = new long[maxDelayMillis];
        //init the delays array
        for (int i=0; i<this.delays.length; i++) {
            this.delays[i] = 0;
        }
        
        new DequeueThread().start();
    }
    
    
    
    public void onDelay(int delay) {
        if (!enabled) {
            return;
        }
        queue.add(delay);
    }
    
    public void onRampUpEnd() {
        System.out.println("Test " + this.testName + " starts collecting data");
        this.enabled  = true;
        this.startTime = new Date().getTime();
    }
    
    public void onTestComplete() {
        this.generateReport();
        System.out.println("Test " + this.testName + " complete");
    }
    
    private synchronized void generateReport() {
        if (totUpdates == 0) {
            System.out.println("No updates received to generate report");
            return;
        } 

        this.alive  = false;
        
        this.append(results, "*********TEST ", "", this.type);
        this.append(results, "Clients ", "", this.clientsNum);
        
      //THROUGHPUT
        long spentTime = new Date().getTime()-this.startTime;
        long throughput = totUpdates/(spentTime/1000);
       
        this.append(results, "Throughput ", " messages per second", throughput);
        
      //MEAN
        //calculate the average delay
        long mean = totDelays/totUpdates; 
        this.append(results, "Mean ", " millis", mean);
        
      //STANDARD-DEVIATION RELATED
        //sum needed to calculate the standard deviation: sqrt {[ (value1-avg)^2 + (value2-avg)^2 + (value3-avg)^2 ... (valueN-avg)^2] / N }
        long deviationSum = 0;
        
      //MEDIAN-RELATED
        //needed to find the Median value
        long medianSum = 0;
        boolean median = false;
        long medianPosition = totUpdates/2;
        
        for (int i=minDelayFound; i<=maxDelayFound; i++) {
            if (this.delays[i]>0) {
                
                if (!median) {
                    medianSum+=this.delays[i];
                    if (medianSum >= medianPosition) {
                        median = true;
                        this.append(results, "Median ", " millis", i);
                    }
                }
                            
                
                
                //STANDARD-DEVIATION RELATED
                deviationSum += (Math.pow(i-mean,2)*this.delays[i]);
            }
        }
        
        long deviation = Math.round(Math.sqrt((double)deviationSum/totUpdates)); 
        this.append(results, "Standard deviation ", "", deviation);
        results.append("\n");
        
        File file = new File(fileName);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            BufferedWriter out = new BufferedWriter(new FileWriter(file,true));
            out.write(results.toString());
            out.close();
        } catch (IOException e) {
            System.out.println("CAN'T WRITE ON FILE: \n*************************");
            System.out.println(results);
            System.out.println("*************************");
        }
       
      
        
        
    }
    
    private void append(StringBuffer results,String prefix,String suffix,long val) {
        this.append(results,prefix,suffix,String.valueOf(val));
    }
    private void append(StringBuffer results,String prefix,String suffix,String val) {
        if(this.tabLog) {
            results.append(val);
            results.append(",");
        } else {
            results.append(prefix);
            results.append(val);
            results.append(suffix);   
            results.append("\n");
        }
    }
    
    private void dequeueData() {
        while (alive) {
            while (queue.isEmpty()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }
            
            int delay = queue.poll();
            
            //if delay < 0 or delay maxDelayMillis we'll have an OutOfBoundException
            try {
                synchronized(this) {
                    this.delays[delay]++;
                    totDelays += delay;
                    totUpdates++;
                    
                    if (maxDelayFound < delay) {
                        maxDelayFound = delay;
                    }
                    if (minDelayFound > delay) {
                        minDelayFound = delay;
                    }
                }
                
            } catch(ArrayIndexOutOfBoundsException ex) {
                if (delay < 0) {
                    System.out.println("Negative delay received; the network latency during the test was probably smaller than the network latency detected when synchronizing clocks");
                } else {
                    System.out.println("A delay > " + delays.length + " was received. Please change the Constants.MAX_DELAY_MILLIS constant to support such big delays");
                }
            }
        }
    }
    
    private class DequeueThread extends Thread {
        public void run() {
            dequeueData();
        }
    }

}

