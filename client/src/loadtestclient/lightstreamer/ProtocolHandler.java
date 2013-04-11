package loadtestclient.client;


public abstract class ProtocolHandler {
    
    protected EventDispatcher dispatcher;
    protected WebSocketConnection networkHandler;
    
    protected static final int OFF = 1;
    protected static final int WAITING_SUB = 3;
    protected static final int ON = 2;
    protected static final int END = 4;
  
    protected int status = OFF;
    protected String server;
    
    public ProtocolHandler(BasicWSClient basicWSClient, String server) {
        this.dispatcher = new EventDispatcher(basicWSClient);
        
        this.networkHandler = new WebSocketConnection();
        this.networkHandler.setMessageListener(this);
        
        this.server = server;
      
        
    }
    
    
    protected void abortGateway() {
        if (this.status == ON || this.status == OFF) {
            this.dispatcher.createEvent(EventDispatcher.ERROR, null);
            
            this.closeGateway();
            
            networkHandler.close();
         }
    }
    
    protected void closeGateway() {
        if (this.status == ON || this.status == OFF) {
            this.status = END;

            this.dispatcher.createEvent(EventDispatcher.CLOSE, null);
        }
    }
    
    protected void openGateway() {
        if (this.status != ON) {
            this.status = ON;

            this.dispatcher.createEvent(EventDispatcher.OPEN, null);
        }
    }
    
    
    public synchronized void handleClose() {
        this.closeGateway();
    }
    
    public synchronized void handleException() {
        this.abortGateway();
    }
    
    public abstract void open();

    public void close() {
        this.networkHandler.close();
    }


    public abstract void handleMessage(String text);
    public abstract void sendMessage(String message);
    public abstract void handleSocketOpen();
    
    

}
