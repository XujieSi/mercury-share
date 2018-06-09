package Latency;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class TCPClient extends Client{

	Socket socket;
	byte[] header;
	byte[] body;
	OutputStream out; 
	InputStream in;

	TCPClient(){
		try{
			logPath += "tcp_client_log";
			
			socket = new Socket(ip, Utility.TCP_SERVER_PORT);
			socket.setTcpNoDelay(true);
						
			out = socket.getOutputStream();
			in = socket.getInputStream();
			
			System.out.println("TCPClient initialization is done.");
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	@Override
	void setupCommunication(int packetSize) throws IOException {
		header = (String.format("%09d", packetSize) +  Utility.MagicV ).getBytes();
		out.write(header);

		body = new byte[packetSize];
		for(int i=0; i < packetSize; ++i) body[i] = 'x';
		
		//System.out.println("TCPClient setupCommunication is done.");
	}

	@Override
	long measureCurrentPoint(PrintWriter pr, int packetSize) throws IOException {
		out.write(body);
		byte[] body2 = Utility.readNBytes(in, packetSize);
		//System.out.println("TCPClient measureCurrentPoint is done for size: " + packetSize);
		return 0;
	}

}
