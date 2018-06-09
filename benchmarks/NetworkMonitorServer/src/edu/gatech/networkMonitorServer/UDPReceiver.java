package edu.gatech.networkMonitorServer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UDPReceiver extends Thread {
	static final int PORT = 8001;
	static final int UDPDATASIZE = 1500-20-8; 
	
	DatagramSocket socket = null;
	byte[] receiverBuffer = new byte[UDPDATASIZE];
	
	String	sessionID;
	
	long	startTime;
	long	lastTime;
	int		RSSI;
	double 	bandwidth;
	int		sampleID;
	int 	currentSampleSession = -1;
	
	int		count;
	int		total;
	boolean started = false;
	
	boolean isWifi = false;
	String 	apMac;
	double 	distance;
	
	public UDPReceiver(){
		
	}
	
	@Override
	public void run(){
		int currentSample = -1;
		
		while(true){
			try{
				socket = new DatagramSocket(PORT);
				while(true){
					DatagramPacket packet = new DatagramPacket(receiverBuffer, UDPDATASIZE);
					socket.receive(packet);
					
					String data = new String(packet.getData());
					String[] tokens =data.split(":");
					int sampleId = Integer.parseInt(tokens[0]);
					
					if(sampleId < currentSample){
						if(Integer.parseInt(tokens[2]) != currentSampleSession )
							//1.0. a new session, reset it
							currentSample = -1;
						else
							//1.1. this packet is time out, ignore it
							continue;
					}
					
					if(!started){
						if(sampleId <= currentSample)
							//1.1. this packet is time out, ignore it
							continue;
						if(!tokens[1].startsWith("address"))
							//2. out-of-order packets, ignore it
							continue;
						//3. a new sampling, start a new sampling
						currentSample = sampleId;
						newSession(data, packet);
						
						continue;
					}
					//4. check if currentSampling is time out
					if(sampleId > currentSample){
						//4.1. current sampling is time out, let's record it
						System.out.println("timeout");
						record(packet);
						started = false;
						//4.2. deal with the new packet
						if(!tokens[1].startsWith("address"))
							//2. this packet is the out-of-order
							continue;
						else
						{
							//3. a new sampling
							currentSample = sampleId;
							newSession(data, packet);
							continue;
						}
					}
					total++;
					//5. this packet belongs to current sampling
					if(tokens[1].startsWith("address")){
						//5.1 duplicated address packet, prepare for the packet train
						startTime = System.currentTimeMillis();
						lastTime = System.currentTimeMillis();
						continue;
					}
					//5.2 receive packet train, update the info
					lastTime = System.currentTimeMillis();
					count += UDPDATASIZE;
					
					//5.3 end packet
					if(tokens[1].startsWith("end")){
						//end of session
						record(packet);
						started = false;
					}
				}
			}
			catch(Exception e){}
			finally{
				socket.close();
			}
		}
	}
	private void newSession(String data, DatagramPacket packet){
		//this the first packet received
		started = true;
		startTime = System.currentTimeMillis();
		lastTime = System.currentTimeMillis();
		count = 0;
		total = 0;
		
		String[] loc = data.split(":");
		RSSI	= Integer.parseInt(loc[3]);
		sessionID = loc[2];
		currentSampleSession = Integer.parseInt(sessionID);
		sampleID = Integer.parseInt(loc[0]);
		
		//SUDPSender sender = new SUDPSender(socket,packet.getAddress(),packet.getPort());
		//sender.start();
	}
	

	public void record(DatagramPacket packet){
		UDPSender sender = new UDPSender(socket,packet.getAddress(),packet.getPort());
		sender.start();
		
		long date = startTime/(1000*3600*24)%365;
		
		bandwidth = count*1.0/(lastTime - startTime)*1000.0/1024.0;
		
		int packetNum = count/UDPDATASIZE;

//		File f = new File("./signalup-"+date+"-"+sessionID+".txt");
		File f = new File("./signalup.txt");
		try{
			BufferedWriter writer = new BufferedWriter(new FileWriter(f,true));
			writer.append(sampleID+"\t"+startTime+"\t"+bandwidth+"\t"
					+RSSI+"\t"+packetNum+"\t"+"\n");
			writer.flush();
			writer.close();
		}catch(Exception e){
		}
		
	}
}
