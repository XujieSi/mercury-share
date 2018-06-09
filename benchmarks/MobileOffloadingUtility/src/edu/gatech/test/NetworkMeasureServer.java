package edu.gatech.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import edu.gatech.protocol.Log;

public class NetworkMeasureServer implements Runnable {
	final static String TAG = "NetworkMeasureServer";
	final static int iterations = 40;
	final static int MagicV = 255;
	int [] buf;
	
	public NetworkMeasureServer(){
		Log.d(TAG, "Start NetworkMeasureServer");
		buf = new int[1<<20];
		new Thread(this, "NetworkMeasureServer").start();
	}
	
	public void run() {
		// TODO Auto-generated method stub
		try {
			ServerSocket resultSocket = new ServerSocket(9220);
			Log.d(TAG, "Now NetworkMeasureServer is listening on 9220");
			
			while( true ){
				Socket inSocket = resultSocket.accept();
				inSocket.setTcpNoDelay(true);
				
				InputStream ins = inSocket.getInputStream();
				OutputStream out = inSocket.getOutputStream();
				
				int i = iterations;
				while(--i >= 0){
					// read client's data
					int j = 0;
					while(true){
						int t = ins.read();
						if(t == MagicV){
							break;
						}
						buf[j] = t;
						++j;
					}
					
					// send back the data inreverse order
					while(--j>=0) out.write(buf[j]);
					out.write(MagicV);
					out.flush();
				}
				
			}

		} catch (IOException e) {
			Log.d(TAG, "Error happen on listen/accept port: 9110.");
			e.printStackTrace();
		}
	}

}
