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

module.exports = function(mexPerSecond,burst,cb) {
  var interval = 1000/(mexPerSecond/burst);
  
  var lastTime = null;
  
  var intervalTot = 0;
  var intervalLast = 0;
  var num = 0;
  var numLast = 0;
  
  
  var intervalObj = setInterval(function() {
    var time = new Date().getTime();
    /*var timeArray = process.hrtime();
    var time = timeArray[0]*1e9+timeArray[1];*/
    
    for (var i=0; i<burst; i++) {
      cb(time);
    }
  },interval);
  
  return function() {
    clearTimeout(intervalObj);
  };
  
};


