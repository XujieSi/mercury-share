package edu.gatech.networkMonitor;

import java.net.*;

public class UDPSender extends Thread {
	static int PORT = 8001;
	static String ipAdd = MainPanel.mip;
	static int UDPDATASIZE = 1500-20-8;
	static int PACKETNUM = 20;
	static int ADDPACKET = 3;
	
	DatagramSocket socket;
	InetAddress ip;
	
	int	mRSSI;
	int sessionID;
	int sampleID;
	String APName;
	
	
	byte[] startBuffer = new byte[UDPDATASIZE];
	byte[] sendBuffer = new byte[UDPDATASIZE];
	byte[] endBuffer = new byte[UDPDATASIZE];
	
	DatagramPacket startPacket;
	DatagramPacket trainPacket;
	DatagramPacket endPacket;

	public UDPSender( int rssi, int sid, int sample, String ap){

		mRSSI = rssi;
		sessionID = sid;
		sampleID = sample;
		APName = ap;
		try{
			ip = InetAddress.getByName(ipAdd);
			socket = new DatagramSocket();
			socket.setSoTimeout(5000);
		}
		catch(Exception e){
			
		}
		
		for(int i = 0; i < UDPDATASIZE; i++){
			startBuffer[i]='a';
			sendBuffer[i] = 'a';
			endBuffer[i] = 'a';
		}
		endBuffer[0] = 'e';
		endBuffer[1] = 'n';
		endBuffer[2] = 'd';
		
		//start packet
		String data = sampleID+":address:"+sessionID+":"+mRSSI+":";
		String b = new String(startBuffer);
		data = data+b;
		data =data.substring(0, UDPDATASIZE);
		startBuffer = data.getBytes();
		startPacket = new DatagramPacket(startBuffer, UDPDATASIZE, ip, PORT);
		//2. packet train
		data = sampleID+":";
		b = new String(sendBuffer);
		data = data+b;
		data =data.substring(0, UDPDATASIZE);
		startBuffer = data.getBytes();
		trainPacket = new DatagramPacket(startBuffer, UDPDATASIZE, ip, PORT);
		//3. end
		data = sampleID+":";
		b = new String(endBuffer);
		data = data+b;
		data =data.substring(0, UDPDATASIZE);
		endBuffer = data.getBytes();
	    endPacket = new DatagramPacket(endBuffer, UDPDATASIZE,ip,PORT);
	}
	
	public DatagramSocket getSocket(){
		return socket;
	}
	
	@Override
	public void run(){
		//1. packets with infos
		
		long startTime = System.currentTimeMillis();
		for(int i = 0; i < ADDPACKET; i++){
			try{
				socket.send(startPacket);
			}catch(Exception e){}
		}
		//2. packet train
		for(int i = 0; i < PACKETNUM - ADDPACKET; i++)
			try{
				socket.send(trainPacket);
			}
			catch(Exception e){}
		//3. stop packets
		for(int i = 0; i < 2; i++)
			try{
				socket.send(endPacket);
			}
			catch(Exception e){}
	}

}
