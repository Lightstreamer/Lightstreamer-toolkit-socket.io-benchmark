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
package loadtestclient.client;

import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Security;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketVersion;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.util.CharsetUtil;

public class WebSocketConnection  {

    private static ExecutorService closeThread = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "Close thread");
            t.setDaemon(true);
            return t;
        }
    });
   

    
    private static ChannelFactory channelFactory = new ChannelFactory();
    private static ChannelFactory secureChannelFactory = new ChannelFactory();
    private ChannelFactory localChannelFactory = null;
    
    private static WebSocketClientHandshakerFactory webSocketClientHandshakerFactory = new WebSocketClientHandshakerFactory();
    private ProtocolHandler protocolHandler;


    private StringBuffer mexPiece = new StringBuffer();
    private Channel ch;
    private ClientBootstrap bootstrap;

    private URI targetServer;
    private boolean secure;


    
    public void setMessageListener(ProtocolHandler protocolHandler) {
        this.protocolHandler = protocolHandler;
    }
    

    public void open(URI uri) throws Exception {
        this.targetServer = uri;
        this.secure = this.targetServer.getScheme().equals("wss");
        try {
            final WebSocketClientHandshaker handshaker = webSocketClientHandshakerFactory.newHandshaker(this.targetServer, WebSocketVersion.V13, "js.lightstreamer.com", false, null);
            
            this.localChannelFactory = this.secure ? channelFactory : secureChannelFactory;
            
            final SslHandler sslHandler;
            if (this.secure) {
                SSLEngine sslEngine = SecureSslContextFactory.getClientContext().createSSLEngine();
                sslEngine.setUseClientMode(true);
                sslHandler = new SslHandler(sslEngine);
            } else {
                sslHandler = null;
            }
           
            this.bootstrap = new ClientBootstrap(this.localChannelFactory.getFactory());
            
            bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
                public ChannelPipeline getPipeline() throws Exception {
                    ChannelPipeline pipeline = Channels.pipeline();
                    if (sslHandler != null) {
                        pipeline.addLast("ssl", sslHandler);
                    }
                    pipeline.addLast("decoder", new HttpResponseDecoder());
                    pipeline.addLast("encoder", new HttpRequestEncoder());
                   
                    pipeline.addLast("ws-handler", new WebSocketClientHandler(handshaker));
                    return pipeline;
                }
            });
            
            ChannelFuture future = bootstrap.connect(new InetSocketAddress(this.targetServer.getHost(), this.targetServer.getPort()));
            if (this.secure) {
                future.addListener(new SslHandshaker(sslHandler, new WsHandshaker(handshaker)));
            } else {
                future.addListener(new WsHandshaker(handshaker));
            }
                
            
        
        } catch(Exception e) {
            initConnectionError();
        }
        
    }
    
    public void initConnectionError() {
        protocolHandler.handleException();
        this.close();
    }
    
    public void close() {
        
        closeThread.execute(new Thread(){
            public void run() {
                if (ch != null && ch.isWritable()) {
                    ch.write(new CloseWebSocketFrame());
                    
                    ChannelFuture closing = ch.getCloseFuture();
                    closing.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future)
                                throws Exception {
                            if (ch != null) {
                                ch.close();
                            }
                        }
                    });
                }
            }
        });
        
        
    }
    
    public void send(String message) {
        this.ch.write(new TextWebSocketFrame(message));
    }
    
    private void bufferMessagePiece(String message) {
        mexPiece.append(message);
    }
    
    private void handleMessage(String text) {
        if (mexPiece.length() > 0) {
            mexPiece.append(text);
            text = mexPiece.toString();
            mexPiece.setLength(0);
        } 
        
        this.protocolHandler.handleMessage(text);
    }
    
    private class WebSocketClientHandler extends SimpleChannelUpstreamHandler {
        private WebSocketClientHandshaker handshaker;

        public WebSocketClientHandler(WebSocketClientHandshaker handshaker) {
            this.handshaker = handshaker;
        }
        
        public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) {
            localChannelFactory.addChannel(e.getChannel());
        }
        
        public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            //is this called once and in any case? (exception/valid close/unexpected close...)
            protocolHandler.handleClose();
            
            localChannelFactory.disposeIfEmpty();
            
            
        }
        
        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
            Channel ch = ctx.getChannel();
            if (!handshaker.isHandshakeComplete()) {
                handshaker.finishHandshake(ch, (HttpResponse) e.getMessage());
                protocolHandler.handleSocketOpen();
                return;
            }
              
            if (e.getMessage() instanceof HttpResponse) {
                HttpResponse response = (HttpResponse) e.getMessage();
                throw new Exception("Unexpected HttpResponse (status=" + response.getStatus() + ", content="
                        + response.getContent().toString(CharsetUtil.UTF_8) + ")");
                
            }

            WebSocketFrame frame = (WebSocketFrame) e.getMessage();
            if (frame instanceof TextWebSocketFrame) {
                TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
                
                if (textFrame.isFinalFragment()) {
                    handleMessage(textFrame.getText());
                } else {
                    bufferMessagePiece(textFrame.getText());
                }

                
            } else if (frame instanceof ContinuationWebSocketFrame) {
                ContinuationWebSocketFrame textFrame = (ContinuationWebSocketFrame) frame;
                if (textFrame.isFinalFragment()) {
                    handleMessage(textFrame.getText());
                } else {
                    bufferMessagePiece(textFrame.getText());
                }
     
            } else if (frame instanceof CloseWebSocketFrame) {
                protocolHandler.handleClose();
                ch.close();
              
            }
        }
  
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
            protocolHandler.handleException();
            e.getChannel().close();
        }
    }





    public static class SecureSslContextFactory {
        
        private static final String PROTOCOL = "TLS";
        private static final SSLContext CLIENT_CONTEXT;
        
        private static final TrustManager[] tm = new TrustManager[] { new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
              
            public void checkServerTrusted(
                    X509Certificate[] chain, String authType) throws CertificateException {
                    // Always trust
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain,
                    String authType) throws CertificateException {
                // never used
                
            }
           
        }};
        
        static {
            String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
            if (algorithm == null) {
                algorithm = "SunX509";
            }
        

            SSLContext clientContext = null;
       
            try {
                clientContext = SSLContext.getInstance(PROTOCOL);
                clientContext.init(null, tm, null);
            } catch (Exception e) {
                throw new Error(
                        "Failed to initialize the client-side SSLContext", e);
            }

            CLIENT_CONTEXT = clientContext;
        }
        
        public static SSLContext getClientContext() {
            return CLIENT_CONTEXT;
        }
    }
    
    
    
    
    private class WsHandshaker implements ChannelFutureListener {

        private WebSocketClientHandshaker handshaker;

        public WsHandshaker(WebSocketClientHandshaker handshaker) {
            this.handshaker = handshaker;
        }
        
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if(!future.isSuccess()) {
                initConnectionError();
            } else {
                ch = future.getChannel();
                handshaker.handshake(ch).addListener(new ChannelFutureListener() {

                    @Override
                    public void operationComplete(ChannelFuture future) {
                        if(!future.isSuccess()) {
                            initConnectionError();
                        }
                    }
                        
                });
            }
            
        }
        
    }
    
    private class SslHandshaker implements ChannelFutureListener {

        private ChannelFutureListener chained;
        private SslHandler sslHandler;

        public SslHandshaker(SslHandler sslHandler, ChannelFutureListener chained) {
            this.sslHandler = sslHandler;
            this.chained = chained;
        }
        
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if(!future.isSuccess()) {
                initConnectionError();
            } else {
                this.sslHandler.handshake().addListener(this.chained);
            }
            
        }
        
    }
    
    private static class ChannelFactory {
        
        private ChannelGroup activeChannels = new DefaultChannelGroup();
        private NioClientSocketChannelFactory nioClientSocketChannelFactory = null;
        
        NioClientSocketChannelFactory getFactory() {
            
            synchronized(activeChannels) {
                if (nioClientSocketChannelFactory == null) {
                    nioClientSocketChannelFactory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(new ThreadFactory() {
                        public Thread newThread(Runnable r) {
                            Thread t = new Thread(r, "T1");
                            t.setDaemon(true);
                            return t;
                        }
                    }),Executors.newCachedThreadPool(new ThreadFactory() {
                        public Thread newThread(Runnable r) {
                            Thread t = new Thread(r, "T2");
                            t.setDaemon(true);
                            return t;
                        }
                    }));
                    
                }
                return nioClientSocketChannelFactory;
            }
        }

        public void disposeIfEmpty() {
            synchronized(activeChannels) {
                if (activeChannels.isEmpty()) {
                    
                    nioClientSocketChannelFactory.releaseExternalResources();
                    nioClientSocketChannelFactory = null;
                }
            }
        }

        public void addChannel(Channel channel) {
            synchronized(activeChannels) {
                activeChannels.add(channel);
            }
        }
    }


    
}
