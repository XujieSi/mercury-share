package Latency;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;


public class UDPServer extends Server {

	DatagramSocket inSocket;
	byte[] receiveData;

	UDPServer(){
		try{
			inSocket = new DatagramSocket( Utility.UDP_SERVER_PORT );
			receiveData = new byte[1<<20];
			System.out.println("UDPServer now starts learning on port: " + Utility.UDP_SERVER_PORT );
		}
		catch(Exception e){
			e.printStackTrace();
			System.err.println("Build UDP socket failed.");
		}
	}
	
	@Override
	public void run() {
		long ct = 0;
		while (true) {
			try {
				//inSocket.setSoTimeout(500);
				
				DatagramPacket recvPacket = new DatagramPacket(receiveData,
						receiveData.length);
				inSocket.receive(recvPacket);

				byte[] replyBuf = recvPacket.getData();
				int len = recvPacket.getLength();
				
				DatagramPacket replyPacket = new DatagramPacket(replyBuf, len, recvPacket.getAddress(), Utility.UDP_CLIENT_PORT);
				inSocket.send(replyPacket);
				
				if(ct % Utility.MeasureIterations == 0){
					System.out.println("Got packet with size: " + len);
				}
				++ct;
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		
	}

}
