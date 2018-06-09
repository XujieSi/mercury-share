import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import edu.gatech.mcc.ametro.Protocol;
import edu.gatech.protocol.Log;


public class StationFilterServer implements Runnable{
	public static String TAG = "StationFilterServer";
	public static int serverListenPort;
	ServerSocket serverSocket;
	
	public StationFilterServer() { //, Context _context) {
		
		Log.d (TAG, "StationFilterServer starting ... ");
		
		try {
			serverListenPort = Protocol.port;
			serverSocket = new ServerSocket(serverListenPort);
            Log.d (TAG, "StationFilterServer started on port " + serverListenPort);
        } catch (IOException e) {
            Log.d (TAG, "StationFilterServer could not listen on port: " + serverListenPort);
            System.exit(-1);
        }
		
		new Thread(this, "StationFilterServer").start();
	}
	
	//@Override
	public void run() {
        while (true){
		    try {
		    	Socket clientRequest = serverSocket.accept();
		    
		    	new StationFilterThread(clientRequest);
			} catch (IOException e) {
				Log.d (TAG, "IO Ex in StationFilterServer accept");
				e.printStackTrace();
			}
        }
	}
	
}
