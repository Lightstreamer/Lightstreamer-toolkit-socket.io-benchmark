# Toolkit for Benchmarking Socket.io and Lightstreamer-Node #

## Requirements ##
This README assumes that node.js and a JDK are already installed on the target machines and referenced in the global path.

## Server ##

### Required libs ###
*   socket.io 
*   express.js
*   socket.io-client
*   lightstreamer-adapter 

you can install everything using npm
```
npm install socket.io express socket.io-client lightstreamer-adapter
```

### Run ###
The following instructions assume everything is run on the same machine; configuration files need to be edited to run the various pieces on different machines. 
The suggested setup is to run the clients and the generator on a machine and the server on a different one; this will prevent you from having to deal with clock synchronization issues (as long as the clocks of a multi-core machine are synchronized themselves).

#### Lightstreamer #####
*    Launch Lightstreamer server:
     -    Install and configure the Lightstreamer server following its instructions: [http://www.lightstreamer.com/download](http://www.lightstreamer.com/download)
     -    Create a new folder in its adapters subfolder
     -    Copy the adapters.xml file from server/conf/adapters.xml
     -    Create a lib folder inside the new subfolder
     -    Copy the ls-proxy-adapters.jar from the Lightstreamer distribution into it (you can find it under DOCS-SDKs/sdk_adapter_remoting_infrastructure/lib)
     -    Start Lightstreamer
*    Start the generator
     -    From the server folder run
```
node src/server ls ../conf/conf.js
```

#### Socket.io ####
*    Launch Redis
     -    Install and configure the Redis server following its instructions: [http://redis.io/download](http://redis.io/download)
     -    Start redis
*    Launch the Socket.io server
     -    From the server folder run
```
node src/SocketIoServer ../conf/conf.js
```
*    Start the generator
     -    From the server folder run
```
node src/server io ../conf/conf.js
```

## Client ##
The clients used to run the load tests are not full-fledged clients of their respective servers but simple and minimal implementations based on netty.

### Required libs ###
*   netty-3.5.8.Final.jar

You can download netty from its website ( [Netty](http://netty.io/) ). The use of a newer or older version of the library may or may not result in a working client. In any case, if using a different version, update the jar name when necessary.
Once downloaded place it under a newly created lib folder under the client folder.

### Compile ###
Create a classes folder in the client folder

From the client folder run
```
javac -classpath ./lib/netty-3.5.8.Final.jar -d ./classes ./src/loadtestclient/*.java ./src/loadtestclient/client/*.java
```
Then go into the classes folder and run
```
jar cf ../lib/loadtestclient.jar ./loadtestclient
```


### Run ###
From the client folder 
*   Launch Lightstreamer client:
        java -classpath ./lib/loadtestclient.jar;./lib/netty-3.5.8.Final.jar  loadtestclient.NodeLoadTest ls ./conf/conf.properties  

*   Launch Socket.io client:
        java -classpath ./lib/loadtestclient.jar;./lib/netty-3.5.8.Final.jar  loadtestclient.NodeLoadTest io ./conf/conf.properties  
        
## See Also ##
* [Lightstreamer](http://www.lightstreamer.com)
* [Node.js](http://nodejs.org/)
* [Socket.io](http://socket.io/)
* [Lightstreamer SDK for Node Adapters](https://github.com/Weswit/Lightstreamer-lib-node-adapter)

## Lightstreamer Compatibility Notes ##
Compatible with Lightstreamer Server since 5.0  
Compatible with Adapter Remoting Infrastructure since 1.4.3  
Compatible with Lightstreamer SDK for Node Adapters 1.0.0  
