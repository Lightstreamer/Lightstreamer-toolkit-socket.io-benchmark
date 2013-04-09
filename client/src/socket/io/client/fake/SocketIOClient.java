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
package socket.io.client.fake;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import loadtestrunner.ClientListener;

import com.netiq.websocket.WebSocketClient;

public class SocketIOClient extends WebSocketClient {

	protected ClientListener listener;
	protected Map<String, Long> requests = new HashMap<String, Long>();

	protected static int nextId = 0;

	protected int id;

	public SocketIOClient(URI server, ClientListener listener) {
		super(server);

		this.listener = listener;
		id = nextId;

		nextId++;
	}

	@Override
	public void onClose() {
		this.listener.onClose();
	}

	@Override
	public void onIOError(IOException arg0) {
		System.out.println("error: " + arg0);
	}

	@Override
	public void onMessage(String message) {
	    switch (message.toCharArray()[0]) {
            case '5':
                // We want to extract the actual message. Going to hack this shit.
                String[] messageParts = message.split(":");
                String lastPart = messageParts[messageParts.length - 1];
                String chatPayload = lastPart.substring(1, lastPart.length() - 2);
       
                this.listener.onMessage(chatPayload);
                
                break;
        }
	}

	@Override
	public void onOpen() {
		this.listener.onOpen();
	}

	public static URI getNewSocketURI(String server) {
		try {
			URL url = new URL("http://" + server + "/socket.io/1/");
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
			connection.setDoOutput(true);
			connection.setDoInput(true);
			connection.setRequestMethod("POST");

			DataOutputStream wr = new DataOutputStream(
					connection.getOutputStream());
			wr.flush();
			wr.close();

			BufferedReader rd = new BufferedReader(new InputStreamReader(
					connection.getInputStream()));
			String line = rd.readLine();
			String hskey = line.split(":")[0];
			return new URI("ws://" + server + "/socket.io/1/websocket/" + hskey);
		} catch (Exception e) {
			System.out.println("error: " + e);
			return null;
		}
	}
}