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

function startServer(listenPort,redisPort) {
  var app = require('express')();
  var server = require('http').createServer(app);
  var io = require('socket.io').listen(server);
  var redis = require("socket.io/node_modules/redis");
  var RedisStore = require('socket.io/lib/stores/redis');
  
  //io.set('heartbeats',false);
  io.set('heartbeat timeout',10000);
  io.set('heartbeat interval',9000);
  io.set('close timeout', 9000);
  io.set("log level", 1);
  io.set('store',new RedisStore({
      redisPub : redis.createClient(redisPort,"localhost"),
      redisSub : redis.createClient(redisPort,"localhost"),
      redisClient : redis.createClient(redisPort,"localhost")
  }));
  
  server.listen(listenPort);
  
  return io;
}


   
var cluster = require('cluster');   
if (cluster.isMaster) {
  var numCPUs = require('os').cpus().length;
   // Fork workers.
  for (var i = 0; i < numCPUs; i++) {
    cluster.fork();
  }
  
} else {
  var conf = "./conf";
  process.argv.forEach(function (val, index, array) {
    if (index == 2) {  
      console.log("Using custom conf file " + val + ".js");
      conf = val;
    }
  });
  conf = require(conf);
  
  var io = startServer(conf.LISTEN_PORT,conf.REDIS_LISTEN_PORT);
  
  io.sockets.on('connection', function(socket) {
    socket.on('generated', function(data) {
      //when a message is received from the generator it is broadcasted to all the clients
      socket.broadcast.emit("timestamp",data);
    });
  });
  
}