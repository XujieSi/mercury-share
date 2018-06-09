package edu.gatech.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import edu.gatech.protocol.Log;
import edu.gatech.util.Utility;

public class NetworkMeasureServer_UDP implements Runnable {
	final static String TAG = "NetworkMeasureServer";
	public final static int iterations = 5; //50;
	public final static int warmUp = 0;
	final static int HEADER_LEN = 10;
	int [] buf;
	
	public NetworkMeasureServer_UDP(){
		Log.d(TAG, "Start NetworkMeasureServer");
		buf = new int[1<<20];
		new Thread(this, "NetworkMeasureServer").start();
	}
	
	public void run() {
		// TODO Auto-generated method stub
		try {
			//ServerSocket resultSocket = new ServerSocket(9220);

			Log.d(TAG, "Now NetworkMeasureServer_UDP is listening on " + Utility.UDP_SERVER_PORT);
			
			//Socket inSocket = resultSocket.accept();
			DatagramSocket inSocket = new DatagramSocket( Utility.UDP_SERVER_PORT );

			//inSocket.setPerformancePreferences(0, 2, 1);
			//inSocket.setTcpNoDelay(true);
			//InputStream ins = inSocket.getInputStream();
			//OutputStream out = inSocket.getOutputStream();
			
			DatagramSocket ins = inSocket;
			DatagramSocket out = inSocket;

			
			byte[] header = null;
			//while( (header = Utility.readNBytes(ins, HEADER_LEN)) != null){
			//while( (header = Utility.readNBytesUDP(ins, HEADER_LEN)) != null){
			while(true){

				DatagramPacket recvPacket = Utility.readByteUDP(ins, null);
				InetAddress clientAddr = recvPacket.getAddress();
				Log.d(TAG, "Got client addr: " + clientAddr);

				/*if( header[HEADER_LEN-1] != '#' ){
					Log.d(TAG, "Header format is wrong, the last character is not '#', maybe package size is too large.");
				}
				int len = 0;
				for(int i=0;i<HEADER_LEN-1;++i) len = 10 * len + (header[i]-'0');
				
				Log.d(TAG, "Header length is " + len);
				*/
				
				int i =  iterations;
				while(--i >= 0){
//					inSocket = resultSocket.accept();
//					ins = inSocket.getInputStream();
//					out = inSocket.getOutputStream();

					// read client's data
					//byte[] body = Utility.readNBytes(ins, len);
					
					//byte[] body = Utility.readNBytesUDP(ins, len);
					byte[] body = Utility.simpleProtocolReadUDP(ins, byte[].class, null);
					Log.d(TAG, "read " + body.length + " bytes from client.");
					
					if(body == null){
						Log.d(TAG, "error happen in  reading body");
						ins.close();
						out.close();
						return;
					}
					
					
					// send back the same data 
					//out.write(body);
					//out.flush();
					
					Utility.simpleProtocolWriteUDP(out, clientAddr, Utility.UDP_CLIENT_PORT, body,null);
					Log.d(TAG, "send " + body.length + " bytes to client.");

					
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
