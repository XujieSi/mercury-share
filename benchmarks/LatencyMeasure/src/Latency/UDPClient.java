package Latency;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPClient extends Client{

	DatagramSocket socket;
	InetAddress serverIPAddr;
	byte[] receiveBuf;
	byte[] data;
	
	UDPClient(){
		try{
			logPath += "udp_client_log";

			socket = new DatagramSocket( Utility.UDP_CLIENT_PORT );
			serverIPAddr = InetAddress.getByName(ip);
			receiveBuf = new byte[Utility.BUF_SIZE];
			data = new byte[Utility.BUF_SIZE];
			
			for(int i=0; i < Utility.BUF_SIZE; ++i){
				data[i] = 'x';
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	
	@Override
	void setupCommunication(int packetSize) {
		// TODO Auto-generated method stub		
	}

	@Override
	long measureCurrentPoint(PrintWriter pr, int packetSize) {
		
		while (true) {
			try {
				
				socket.setSoTimeout(200);
				
				long begin = System.nanoTime();

				DatagramPacket sndPacket = new DatagramPacket(data, packetSize,
						serverIPAddr, Utility.UDP_SERVER_PORT);

				socket.send(sndPacket);
	
				DatagramPacket recvPacket = new DatagramPacket(receiveBuf,
						receiveBuf.length);
	
				socket.receive(recvPacket);
				long end = System.nanoTime();
				
				//pr.println("UDP_Actual_One_Time\t" + packetSize + "\t" + (end - begin));
				//break;
				//System.out.println("UDPClient finished measuring packet size: " + packetSize);
				return end - begin;
				
			} catch (IOException e) {
				e.printStackTrace();
				System.out
						.println("Fail to receive ready confirmation from server, will retry ...");
			}
		}

	}

}
