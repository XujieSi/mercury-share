import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import edu.gatech.mcc.collision.AnimationView;
import edu.gatech.protocol.Log;


public class CollisionServer implements Runnable{
	public static String TAG = "CollisionServer";
	public static final int serverListenPort = AnimationView.serverListenPort;
	ServerSocket serverSocket;
	
	public CollisionServer() { //, Context _context) {
		
		Log.d (TAG, "CollisionServer starting ... ");
		
		try {
			serverSocket = new ServerSocket(serverListenPort);
            Log.d (TAG, "CollisionServer started on port " + serverListenPort);
        } catch (IOException e) {
            Log.d (TAG, "CollisionServer could not listen on port: " + serverListenPort);
            System.exit(-1);
        }
		
	}
	
	@Override
	public void run() {
        while (true){
		    try {
		    	Socket clientRequest = serverSocket.accept();
				new Thread(new CollisionThread(clientRequest), "CollisionThread").start();
			} catch (IOException e) {
				Log.d (TAG, "IO Ex in CollisionServer accept");
				e.printStackTrace();
			}
        }
	}
	
}
