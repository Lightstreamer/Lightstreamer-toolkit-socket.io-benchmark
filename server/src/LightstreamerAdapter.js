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

var 
  // Imports
  MetadataProvider = require('lightstreamer-adapter').MetadataProvider,
  DataProvider = require('lightstreamer-adapter').DataProvider,
  net = require('net'),
  inspect = require('util').inspect;


module.exports.start = function(conf) {
  
  // Create socket connectio
  var metadataStream = net.createConnection(conf.LS_META_PORT, conf.LS_SERVER_HOST);
  metadataStream.setNoDelay();
  // Create the metadata provider object from the lightstreamer module
  var metadataProvider = new MetadataProvider(metadataStream, {
    distinctSnapLen: 1,
    itemAllowedModes: {raw: true},
    userAllowedModes: {raw: true},
  });
  
  // Create socket connections
  var stopGen = null;
  var reqRespStream = net.createConnection(conf.LS_REQ_RESP_PORT, conf.LS_SERVER_HOST);
  reqRespStream.setNoDelay();
  var notifyStream = net.createConnection(conf.LS_WRITE_PORT, conf.LS_SERVER_HOST);
  notifyStream.setNoDelay();
  // Create the data provider object from the lightstreamer module
  var dataProvider = new DataProvider(reqRespStream, notifyStream);
  // Handle subscribe event
  dataProvider.on('subscribe', function(itemName, response) {
    if(itemName != "timestamp") {
      response.error("Unexpected subscription");
      return;
    }
    console.log("Subcribed item: " + itemName);
    response.success();    
    
    stopGen = require("./Generator")(conf.MEX_PER_SECOND,conf.BURST,function(timestamp) {
      dataProvider.update("timestamp", false, {'message': timestamp});
    });
  
  });

  // Handle unsubscribe event
  dataProvider.on('unsubscribe', function(itemName, response) {
    console.log("Unsubscribed item: " + itemName);
    response.success();    
    
    if (stopGen) {
      stopGen();
    }
  });
  
  console.log("UP AND RUNNING");
};

