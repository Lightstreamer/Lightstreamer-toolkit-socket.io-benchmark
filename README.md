# Server #

## Required libs ##
*   socket.io 
*   express.js
*   lightstreamer-adapter 

you can install everything using npm

## Run ##


*   Launch Lightstreamer server:
    Install and configure the Lightstreamer server following its instructions
    Create a new folder in its adapters subfolder
    Copy the adapters.xml file from server/conf/adapters.xml
    Create a lib folder
    Copy the ls-proxy-adapters.jar from the Lightstreamer distribution into it (you can find it under DOCS-SDKs/sdk_adapter_remoting_infrastructure/lib)
    Start Lightstreamer
    From the server folder run
        node src/server ls ../conf/conf.js

*   Launch Socket.io server:
    From the server folder run
        node src/server io ../conf/conf.js


# Java Client #

## Required libs ##
*   netty-3.5.8.Final.jar
*   websocket-0.7.jar

Download them from their respective websites and place them under a newly created lib folder under the client folder

## Compile ##
Create a classes folder in the client folder

From the client folder run
        javac -classpath ./lib/netty-3.5.8.Final.jar;./lib/websocket-0.7.jar -d ./classes ./src/loadtestclient/*.java ./src/loadtestclient/socket_io/*.java ./src/loadtestclient/lightstreamer/*.java
Then go into the classes folder and run        
        jar cf ../lib/loadtestclient.jar ./loadtestclient


## Run ##
From the client folder 
*   Launch Lightstreamer client:
        java -classpath ./lib/loadtestclient.jar;./lib/netty-3.5.8.Final.jar;./lib/websocket-0.7.jar  loadtestclient.NodeLoadTest ls ./conf/conf.properties  

*   Launch Socket.io client:
        java -classpath ./lib/loadtestclient.jar;./lib/netty-3.5.8.Final.jar;./lib/websocket-0.7.jar  loadtestclient.NodeLoadTest io ./conf/conf.properties  
        

# Lightstreamer Compatibility Notes #
Compatible with Lightstreamer Server since 5.0
Compatible with Adapter Remoting Infrastructure since 1.4.3