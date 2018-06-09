package Latency;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class TCPServerSlave implements Runnable {
	Socket inSocket;
	static String TAG = "TCPServerSlave";
	
	TCPServerSlave(Socket s) {
		inSocket = s;
		System.out.println("TCPServerSlave start handling requests from " + inSocket.getRemoteSocketAddress());
	}

	@Override
	public void run() {
		try {
			inSocket.setTcpNoDelay(true);
			InputStream ins = inSocket.getInputStream();
			OutputStream out = inSocket.getOutputStream();

			byte[] header = null;
			while ((header = Utility.readNBytes(ins, Utility.HEADER_LEN)) != null) {

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

				System.out.println(TAG + "Packet length is " + len);

				int i = Utility.MeasureIterations;
				while (--i >= 0) {

					// read client's data
					byte[] body = Utility.readNBytes(ins, len);

					if (body == null) {
						System.err
								.println(TAG + "error happen in reading body");
						ins.close();
						out.close();
						throw new RuntimeException(TAG
								+ "Error happen in reading body");
					}

					// send back the same data
					out.write(body);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("Current TCPServerSlave is done.");
	}
}
