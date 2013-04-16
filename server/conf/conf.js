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
module.exports = {

  MEX_PER_SECOND:10,
  BURST: 1,
  LS_SERVER_HOST: "localhost",

  //socket.io only conf
  LISTEN_PORT: 8080,
  
  //ls adapter only conf
  LS_REQ_RESP_PORT: 12001,
  LS_WRITE_PORT: 12002,
  LS_META_PORT: 12003
  
};