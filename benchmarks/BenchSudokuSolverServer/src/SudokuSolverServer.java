import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import com.musevisions.android.SudokuSolver.Protocol;

import edu.gatech.protocol.Log;


public class SudokuSolverServer implements Runnable{
	public static String TAG = "CollisionServer";
	public static final int serverListenPort = Protocol.serverListenPort;
	ServerSocket serverSocket;
	
	public SudokuSolverServer() { //, Context _context) {
		
		Log.d (TAG, "SudokuServer starting ... ");
		
		try {
			serverSocket = new ServerSocket(serverListenPort);
            Log.d (TAG, "SudokuServer started on port " + serverListenPort);
        } catch (IOException e) {
            Log.d (TAG, "SudokuServer could not listen on port: " + serverListenPort);
            System.exit(-1);
        }
		
	}
	
	@Override
	public void run() {
        while (true){
		    try {
		    	Socket clientRequest = serverSocket.accept();
				new Thread(new SudokuThread(clientRequest), "SudokuThread").start();
			} catch (IOException e) {
				Log.d (TAG, "IO Ex in CollisionServer accept");
				e.printStackTrace();
			}
        }
	}
	
}
