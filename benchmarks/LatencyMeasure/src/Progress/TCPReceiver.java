package Progress;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

import Latency.Utility;

public class TCPReceiver  extends Receiver {

	ServerSocket resultSocket;
	final static String TAG = "TCPReceiver: ";

	TCPReceiver() {
		try {
			resultSocket = new ServerSocket(Utility.TCP_SERVER_PORT);
			System.out.println("Now TCPServer is listening on "
					+ Utility.TCP_SERVER_PORT);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		try {
			Socket inSocket;
			System.out.println("Waiting for the first request...");
			while ((inSocket = resultSocket.accept()) != null) {
				System.out.println("Start handling request from " + inSocket.getRemoteSocketAddress());
				receive(inSocket);
				System.out.println("Done.");
				System.out.println("Now waiting for new request coming... ");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	void receive(Socket inSocket) {
		// TODO Auto-generated method stub
		
		
		try {
			
			// inSocket.setPerformancePreferences(0, 2, 1);
			inSocket.setTcpNoDelay(true);
			InputStream ins = inSocket.getInputStream();

			byte[] header = Utility.readNBytes(ins, Utility.HEADER_LEN);
			if(header == null){
				System.err.println(TAG + "failed to read header.");
			}
			
			if (header[Utility.HEADER_LEN - 1] != Utility.MagicV) {
				System.err
						.println(TAG
								+ "Header format is wrong, the last character is not "
								+ Utility.MagicV
								+ ", maybe package size is too large.");
			}

			int len = 0;
			for (int i = 0; i < Utility.HEADER_LEN - 1; ++i)
				len = 10 * len + (header[i] - '0');

			System.out.println(TAG + "Header length is " + len);

			// read client's data
			byte[] body = Utility.readNBytes(ins, len);
			
			//for(int i=0; i < body.length; ++i){
			//	if( body[i] != 'x'){
			//		System.err.println("The " + i + "-th is corrupted.");
			//	}
			//}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
