package Progress;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import Latency.Utility;


public class UDPSender extends Sender {
	DatagramSocket socket;
	InetAddress serverIPAddr;
	byte[] receiveBuf;
	byte[] data;
	
	UDPSender(){
		try{
			socket = new DatagramSocket( Utility.UDP_CLIENT_PORT );
			serverIPAddr = InetAddress.getByName(ip);
			receiveBuf = new byte[Utility.BUF_SIZE];
			data = new byte[Utility.BUF_SIZE];
			
			for(int i=0; i < Utility.BUF_SIZE; ++i){
				data[i] = (byte) (i%255); //'x';
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	void notifyServer(String msg, String expectedResp, int repeatPerTime)
	{
		System.out.println("Notifying server msg: " + msg + " ...");
		byte[] buf = msg.getBytes();
		DatagramPacket initPacket = new DatagramPacket(buf, buf.length, serverIPAddr, Utility.UDP_SERVER_PORT);
		try {
			while(true){
				int i = repeatPerTime;
				while(--i >=0 ){
					socket.send(initPacket);
				}
				
				socket.setSoTimeout(10);
				DatagramPacket recvPacket = new DatagramPacket(receiveBuf, receiveBuf.length);
				
				try{
					socket.receive(recvPacket);
					
					String s = new String(recvPacket.getData(), recvPacket.getOffset(), recvPacket.getLength() );
					if(s.equalsIgnoreCase(expectedResp)){
						socket.setSoTimeout(0);
						System.out.println("Got confirmation from server: " + s);
						break; // server get ready, stop sending.
					}
				}
				catch(IOException e){
					e.printStackTrace();
					System.out.println("Fail to receive ready confirmation from server, will retry ...");
				}
			}
			
		} catch (Exception e) {
			System.err.println("Error happen in telling server the packet length.");
		}

	}
	
	@Override
	void send(int Size) {
		// repeat send the packet length to server until server confirms
		notifyServer("begin" + Size, "ready",1);
		
		long cnt = 0;
		long begin = System.nanoTime();
		// now send packet (1400 bytes per packet)
		int i = Size;
		while(i > 0){
			int sizeToSend  = 0;
			if(i >= Utility.UDP_PACKET_SIZE){
				sizeToSend = Utility.UDP_PACKET_SIZE;
			}
			else{
				sizeToSend = i;
			}
			
			i -= sizeToSend;
			
			DatagramPacket sndPacket = new DatagramPacket(data, sizeToSend, serverIPAddr, Utility.UDP_SERVER_PORT);
			
			try{
				socket.send(sndPacket);
				++cnt;
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		System.out.println("After_sent:\t" + cnt + "\t" + Size + "\t" + (System.nanoTime() - begin));
		
		// tells server that sending is finished.
		notifyServer("end", "done",5);		
		System.out.println("Got_done_confirmation:\t" + cnt + "\t" + Size + "\t" + (System.nanoTime() - begin) );

		socket.close();
	}
}
