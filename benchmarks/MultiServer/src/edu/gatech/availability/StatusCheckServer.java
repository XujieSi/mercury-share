package edu.gatech.availability;

import java.io.IOException;
import java.net.ServerSocket;

import android.content.Context;
//import android.os.NetworkOnMainThreadException;
import android.util.Log;

public class StatusCheckServer implements Runnable{
	private static final String TAG = edu.gatech.main.MainActivity.TAG;
	private Thread t;
	private ServerSocket serverSocket = null; 
    //private Protocol protocol;
    private Context context;
	public StatusCheckServer(Context context) {
		Log.d (TAG, "Starting Control Server...");
		this.context = context;
		t = new Thread(this, "ControlServer"); 
		
		t.start(); 
	}
	
	@Override
	public void run() {
		boolean listening = true;
		//Protocol protocol = new Protocol(this.context);
		try {
            serverSocket = new ServerSocket(12345);
            Log.d (TAG, "ControlServer is listening port 12345");
        } catch (IOException e) {
            Log.d (TAG, "ControlServer could not listen on port: 12345.");
            System.exit(-1); 
        }
		
		
		while (listening){ 
		    try { 
		    	//new ServerThread(serverSocket.accept(), context, protocol);
		    	new StatusRespThread(serverSocket.accept(), context);
				
			} catch (IOException e) {
				Log.d (TAG, "IOEx in Server accept");
				e.printStackTrace();
			}
		}
		
		Log.d (TAG, "Quitting Control Server");
		
        try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
