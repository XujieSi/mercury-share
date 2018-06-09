package edu.gatech.test;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import edu.gatech.protocol.Log;
import edu.gatech.util.Utility;

public class PowerMeasureServer implements Runnable {
	final static String TAG = "PowerMeasureServer";
	final static int HEADER_LEN = 10;
	public PowerMeasureServer(){
		Log.d(TAG, "Start PowerMeasureServer");
		new Thread(this, "PowerMeasureServer").start();
	}
	
	public void run() {
		try{
			ServerSocket resultSocket = new ServerSocket(9220);
			Log.d(TAG, "Now PowerMeasureServer is listening on 9220");
			Socket inSocket = resultSocket.accept();
			InputStream ins = inSocket.getInputStream();
			OutputStream out = inSocket.getOutputStream();
			
			byte [] header = Utility.readNBytes(ins, HEADER_LEN) ;
			
			Log.d(TAG, "Get connection from client : " + inSocket.getInetAddress());
			
			boolean clientUpload = false;
			if(header[0] == 'U'){
				clientUpload = true;
			}
			else if(header[0] == 'D'){
				clientUpload = false;
			}
			else{
				Log.d(TAG, "The first character should be U or D");
			}
			
			if( header[HEADER_LEN-1] != '#' ){
				Log.d(TAG, "Header format is wrong, the last character is not '#', maybe package size is too large.");
			}
			
			int len = 0;
			for(int i=1;i<HEADER_LEN-1;++i) len = 10 * len + (header[i]-'0');
			byte[] body = new byte[len];
			
			
			Log.d(TAG, "Packet length is " + len);
			
			while(true){
				if(clientUpload){
					//Log.d(TAG, "read from client ...");
					Utility.readNBytes(ins, len) ;
					out.write(body);
					//Log.d(TAG, "read is done.");
				}
				else{
					//Log.d(TAG, "write to client ...");
					out.write(body);
					Utility.readNBytes(ins, len) ;
					//Log.d(TAG, "write is done");
				}
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
