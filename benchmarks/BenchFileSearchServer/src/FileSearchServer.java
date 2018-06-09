import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import com.example.filesearch.MainActivity;

import edu.gatech.protocol.Log;


public class FileSearchServer implements Runnable{
	public static String TAG = "FileSearchServer";
	public static final int serverListenPort = MainActivity.serverListenPort;
	ServerSocket serverSocket;
	
	public FileSearchServer() { //, Context _context) {
		
		Log.d (TAG, "FileSearchServer starting ... ");
		
		try {
			serverSocket = new ServerSocket(serverListenPort);
            Log.d (TAG, "FileSearchServer started on port " + serverListenPort);
        } catch (IOException e) {
            Log.d (TAG, "FileSearchServer could not listen on port: " + serverListenPort);
            System.exit(-1);
        }
		
	}
	
	@Override
	public void run() {
        while (true){
		    try {
		    	Socket clientRequest = serverSocket.accept();
				new Thread(new FileSearchThread(clientRequest), "FileSearchThread").start();
			} catch (IOException e) {
				Log.d (TAG, "IO Ex in FileSearchServer accept");
				e.printStackTrace();
			}
        }
	}
	
}
