package Latency;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPServer extends Server {

	ServerSocket resultSocket;
	final static String TAG = "TCPServer: ";

	TCPServer() {
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
		Socket inSocket;
		try {
			while( (inSocket = resultSocket.accept()) != null ){
				new TCPServerSlave(inSocket).run();
			}
			// inSocket.setPerformancePreferences(0, 2, 1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
