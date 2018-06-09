package Latency;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class Utility {
	
	public static final int MeasureIterations = 50;
	public static final int TCP_SERVER_PORT = 9220;
	
	public static final int UDP_SERVER_PORT = 8888;
	public static final int UDP_CLIENT_PORT = 9999;
	public static final int UDP_PACKET_SIZE = 1400;
	
	public static final int BUF_SIZE = 1<<20;
	
	public static final int HEADER_LEN = 10;
	
	public static final char MagicV = '#';

	

	public static void renameLogFile(String path, String oldName, String newName) {
		File sdcard_dir = new File( path );
		File from = new File(sdcard_dir, oldName);
		File to = new File(sdcard_dir, newName);
		from.renameTo(to);
	}
	
	
	public static byte[] readNBytes(InputStream is, int N) throws IOException {

		byte[] res = new byte[N];
		int current = 0;
		int bytesRead = 0;
		do {
			bytesRead = is.read(res, current, N - current);
			current += bytesRead;

			if (bytesRead == -1) {
				System.err
						.println("Utility.readNBytes: bytesRead=-1, inputstream read failed. current read = "
								+ current);
				return null;
			}

		} while (current < N);

		return res;
	}

}
