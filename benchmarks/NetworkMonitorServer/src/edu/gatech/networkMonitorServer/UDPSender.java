package edu.gatech.networkMonitorServer;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPSender extends Thread {
	static final int PACKETNUM = 30;
	
	//byte[] startBuffer = new byte[UDPServer.UDPDATASIZE];
	byte[] sendBuffer = new byte[UDPReceiver.UDPDATASIZE];
	byte[] endBuffer = new byte[UDPReceiver.UDPDATASIZE];
	
	DatagramSocket mSocket;
	InetAddress mip;
	int mport;
	
	public UDPSender(DatagramSocket socket, InetAddress ip, int port)
	{
		for(int i = 0; i < UDPReceiver.UDPDATASIZE; i++){
			sendBuffer[i] = 'a';
			endBuffer[i] = 'a';
		}
		endBuffer[0] = 'e';
		endBuffer[1] = 'n';
		endBuffer[2] = 'd';
		
		mSocket = socket;
		mip = ip;
		mport = port;
	}
	
	@Override
	public void run(){
		
		for(int i = 0; i < PACKETNUM ; i++)
			try{
				if(i<10)
					sendBuffer[0] = (byte)(i+'0');
				else{
					sendBuffer[0] = (byte)(i/10+'0');
					sendBuffer[1] = (byte)(i%10+'0');
				}
				DatagramPacket packet = new DatagramPacket(sendBuffer, UDPReceiver.UDPDATASIZE,mip,mport);
				mSocket.send(packet);
			}
			catch(Exception e){
				e.printStackTrace();
			}
		DatagramPacket packet = new DatagramPacket(endBuffer, UDPReceiver.UDPDATASIZE,mip,mport);
		for(int i = 0; i < 3; i++)
			try{
				mSocket.send(packet);
			}
			catch(Exception e){
				e.printStackTrace();
			}
	}

}

