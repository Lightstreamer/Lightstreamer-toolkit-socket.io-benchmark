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
var SocketIoClient = require('socket.io-client');

module.exports.start = function(conf) {
  var socket = SocketIoClient.connect("http://"+conf.LS_SERVER_HOST+":"+conf.TIMESTAMP_LISTEN_PORT);
  
  socket.on('connect',function() {
    require("./Generator")(conf.MEX_PER_SECOND,conf.BURST,function(timestamp) {
      socket.emit("generated",timestamp);
    });
    console.log("UP AND RUNNING");
  });
  
  
};