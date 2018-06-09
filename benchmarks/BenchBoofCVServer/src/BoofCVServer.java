import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import edu.gatech.mcc.stabilization.Protocol;
import edu.gatech.protocol.Log;


public class BoofCVServer implements Runnable{
	public static String TAG = "BoofCVServer";
	public static final int serverListenPort = Protocol.serverListenPort;
	ServerSocket serverSocket;
	
	public BoofCVServer() { //, Context _context) {
		
		Log.d (TAG, "BoofCVServer starting ... ");
		
		try {
			serverSocket = new ServerSocket(serverListenPort);
            Log.d (TAG, "BoofCVServer started on port " + serverListenPort);
        } catch (IOException e) {
            Log.d (TAG, "BoofCVServer could not listen on port: " + serverListenPort);
        }
		
	}
	
	@Override
	public void run() {
        while (true){
		    try {
		    	Socket clientRequest = serverSocket.accept();
				new Thread(new BoofCVThread(clientRequest), "BoofCVThread").start();
			} catch (IOException e) {
				Log.d (TAG, "IO Ex in BoofCVServer accept");
				e.printStackTrace();
			}
        }
	}
	
}
