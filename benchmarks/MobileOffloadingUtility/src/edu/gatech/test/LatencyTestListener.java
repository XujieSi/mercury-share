package edu.gatech.test;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

//import android.util.Log;
import edu.gatech.protocol.Log;


public class LatencyTestListener implements Runnable {
	
	final static String TAG = "LatencyTestListener";
	
	public LatencyTestListener(){
		Log.d(TAG, "Start LatencyTestListener");
		new Thread(this, "LatencyTestListener").start();
	}
	
	//@Override
	public void run() {
		try {
			ServerSocket resultSocket = new ServerSocket(9110);
			Log.d(TAG, "Now LatencyTestListner is listening on 9110");
			
			while( true ){
				Socket inSocket = resultSocket.accept();
				BufferedReader in = new BufferedReader( 
    				    new InputStreamReader(
    				    inSocket.getInputStream()));
				
				Log.d(TAG, "Get latency test input from mobile : " + in.readLine());
				
				PrintWriter out = new PrintWriter(inSocket.getOutputStream(), true);
				out.println("1");
			}

		} catch (IOException e) {
			Log.d(TAG, "Error happen on listen/accept port: 9110.");
			e.printStackTrace();
		}
	}
}