import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import pag.zxwrapper.Protocol;
import edu.gatech.protocol.Log;

public class ZxingServer implements Runnable{
	public static String TAG = "CollisionServer";
	public static final int serverListenPort = Protocol.serverListenPort;
	ServerSocket serverSocket;
	
	public ZxingServer() { //, Context _context) {
		
		Log.d (TAG, "ZxingServer starting ... ");
		
		try {
			serverSocket = new ServerSocket(serverListenPort);
            Log.d (TAG, "ZxingServer started on port " + serverListenPort);
        } catch (IOException e) {
            Log.d (TAG, "ZxingServer could not listen on port: " + serverListenPort);
            System.exit(-1);
        }
		
	}
	
	@Override
	public void run() {
        while (true){
		    try {
		    	Socket clientRequest = serverSocket.accept();
				new Thread(new ZXingThread(clientRequest), "ZxingThread").start();
			} catch (IOException e) {
				Log.d (TAG, "IO Ex in CollisionServer accept");
				e.printStackTrace();
			}
        }
	}
	
}
