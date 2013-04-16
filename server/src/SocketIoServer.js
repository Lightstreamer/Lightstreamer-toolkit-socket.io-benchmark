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
   
var app = require('express')(),
 server = require('http').createServer(app),
 io = require('socket.io').listen(server);

var conf = "./conf";
process.argv.forEach(function (val, index, array) {
  if (index == 2) {  
    console.log("Using custom conf file " + val + ".js");
    conf = val;
  }
});

conf = require(conf);

//io.set('heartbeats',false);
io.set('heartbeat timeout',10000);
io.set('heartbeat interval',9000);
io.set('close timeout', 9000);
io.set("log level", 0);

server.listen(conf.LISTEN_PORT);

io.sockets.on('connection', function(socket) {
    socket.on('generated', function(data) {
        io.sockets.emit('timestamp', data);
    });
});