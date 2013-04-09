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

var conf = "./conf";
var type = "ls";
process.argv.forEach(function (val, index, array) {
  if (index <= 1) {
    return;
  } else if (index == 2) {  
    type = val;
  } else if (index == 3) {
    console.log("Using custom conf file " + val + ".js");
    conf = val;
  }
});

conf = require(conf);

console.log("Running " + (type == "ls" ? "Lightstreamer" : "Socket.io") + " server");
console.log(conf);

var server = null;
if (type == "ls") {
  server = require("./LightstreamerAdapter");
} else { // "io"
  server = require("./SocketIoServer");
}

server.start(conf);