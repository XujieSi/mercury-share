package edu.gatech.uselessjunk;

import java.io.IOException;
import java.net.ServerSocket;

import edu.gatech.availability.StatusRespThread;

import android.content.Context;
//import android.os.NetworkOnMainThreadException;
import android.util.Log;

public class ResultServer implements Runnable {
	private static final String TAG = edu.gatech.main.MainActivity.TAG;
	private Thread t;
	private ServerSocket serverSocket = null;
	private Context context;
	public ResultServer(Context context) {
		Log.d (TAG, "Starting Result Server...");
		this.context = context;
		t = new Thread(this, "ResultServer");
		t.start();		
	}
	
	@Override
	public void run() {
		boolean listening = true;
		try {
            serverSocket = new ServerSocket(9110);
            Log.d (TAG, "DOne 9110");
        } catch (IOException e) {
            Log.d (TAG, "Could not listen on port: 9110.");
            System.exit(-1); 
        }
		while (listening){
		    try {
				new ResultServerThread(serverSocket.accept(), context);
			} catch (IOException e) {
				Log.d (TAG, "IOEx in Server accept");
				e.printStackTrace();
			} /*catch (NetworkOnMainThreadException n) {
				n.printStackTrace();
			}*/
		}
		Log.d (TAG, "Quitting Result Server");
        try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
