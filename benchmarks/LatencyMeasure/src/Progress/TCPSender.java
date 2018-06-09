package Progress;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import Latency.Utility;

public class TCPSender extends Sender {

	Socket socket;
	byte[] header;
	byte[] body;
	OutputStream out;
	InputStream in;

	TCPSender() {
		try {
			socket = new Socket(ip, Utility.TCP_SERVER_PORT);
			socket.setTcpNoDelay(true);

			out = socket.getOutputStream();
			in = socket.getInputStream();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	void send(int packetSize) {
		try {
			header = (String.format("%09d", packetSize) + Utility.MagicV)
					.getBytes();
			out.write(header);

			body = new byte[packetSize];
			for (int i = 0; i < packetSize; ++i) {
				body[i] = (byte) (i%255);
			}

			out.write(body);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
