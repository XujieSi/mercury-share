import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

import com.example.filesearch.FileSearcher;
import com.example.filesearch.ProgressUpdater;
import com.example.filesearch.Protocol;

import edu.gatech.protocol.Log;
import edu.gatech.util.Utility;

public class FileSearchThread implements Runnable, ProgressUpdater {
	String TAG = "FileSearchThread";
	//public final static int iterations = 40;
	final static int HEADER_LEN = 10;
	Socket inSocket = null;
	private FileSearcher fs = new FileSearcher();
	private OutputStream out;


	public FileSearchThread(Socket _in) {
		TAG = TAG + ":" + _in.getPort();
		Log.d(TAG, "Start FileSearchThread");
		
		inSocket = _in;
	}
	
	public void run() {
		try {	
			inSocket.setTcpNoDelay(true);
			InputStream ins = inSocket.getInputStream();
			out = inSocket.getOutputStream();
			
			
			// while loop
			while(true){
				//String stations = readString(ins);
				//String pre = readString(ins);
				
				long start = System.nanoTime();
			
				int header = ins.read();
				Log.d(TAG, header+" to read.");
				
				if(header == Protocol.INIT){
					String files = Utility.simpleProtocolRead(ins, String.class, null);
					fs.setFileList(files);
					header = ins.read();
				}
				
				if (header != Protocol.SEARCH){
					throw new RuntimeException("Unexpected header!");
				}
				
				String search = Utility.simpleProtocolRead(ins, String.class,null);

				Log.d(TAG, "read search string(" + search +") is done.");

				long mid_start = System.nanoTime();
				
				List<String> result = fs.search(search, this, null);
				
				Log.d(TAG, "search file is done.");
				
				long mid_end = System.nanoTime();		
				
				//Utility.delay();
				out.write(Protocol.RETURN);
				String returnString = "null";
				if(result.size() != 0)
					returnString = Utility.join(result, FileSearcher.FILE_SEP);
				Utility.simpleProtocolWrite(out, returnString, null);
				Log.d(TAG, "write is done.");
				
				
				long end = System.nanoTime();
				
				Log.d(TAG, "All:\t" + (end - start));
				Log.d(TAG, "Essential:\t" + (mid_end - mid_start));
			}

		} catch (Exception e) {
			Log.d(TAG,"Error: StationFilterThread failed to run");
			e.printStackTrace();
		}
	}

	@Override
	public void update(int progress) {
		try {
			//Utility.halfDelay();
			out.write(Protocol.CALL_BACK);
			Utility.simpleProtocolWrite(out, new Integer(progress),null);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
