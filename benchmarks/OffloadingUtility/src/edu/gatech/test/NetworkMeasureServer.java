package edu.gatech.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import edu.gatech.protocol.Log;
import edu.gatech.util.Utility;

public class NetworkMeasureServer implements Runnable {
	final static String TAG = "NetworkMeasureServer";
	public final static int iterations = 5; //50;
	public final static int warmUp = 0;
	final static int HEADER_LEN = 10;
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
			
			Socket inSocket = resultSocket.accept();
//			inSocket.setPerformancePreferences(0, 2, 1);
			inSocket.setTcpNoDelay(true);
			InputStream ins = inSocket.getInputStream();
			OutputStream out = inSocket.getOutputStream();
			
			byte[] header = null;
			while( (header = Utility.readNBytes(ins, HEADER_LEN)) != null){
				
				if( header[HEADER_LEN-1] != '#' ){
					Log.d(TAG, "Header format is wrong, the last character is not '#', maybe package size is too large.");
				}
			
				int len = 0;
				for(int i=0;i<HEADER_LEN-1;++i) len = 10 * len + (header[i]-'0');
				
				Log.d(TAG, "Header length is " + len);
				
				int i =  iterations;
				while(--i >= 0){
//					inSocket = resultSocket.accept();
//					ins = inSocket.getInputStream();
//					out = inSocket.getOutputStream();

					// read client's data
					byte[] body = Utility.readNBytes(ins, len);
					
					if(body == null){
						Log.d(TAG, "error happen in  reading body");
						ins.close();
						out.close();
						return;
					}
					
					
					// send back the same data 
					out.write(body);
					out.flush();
					
					//Log.d(TAG, "send body back to client is done");
//					for(int j = 0; j < warmUp ; j++){
//						body = Utility.readNBytes(ins, len);
//						out.write(body);
//						out.flush();
//					}
				}
//				inSocket = resultSocket.accept();
//				ins = inSocket.getInputStream();
//				out = inSocket.getOutputStream();
			}

		} catch (IOException e) {
			Log.d(TAG, "Error happen on listen/accept port: 9110.");
			e.printStackTrace();
		}
	}

}
