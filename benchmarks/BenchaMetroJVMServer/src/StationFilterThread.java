import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

import edu.gatech.mcc.ametro.Protocol;
import edu.gatech.protocol.Log;
import edu.gatech.util.Utility;

public class StationFilterThread implements Runnable {
	String TAG = "StationFilterThread";
	//public final static int iterations = 40;
	final static int HEADER_LEN = 10;
	int[] buf;
	Socket inSocket = null;
	String[] stations = null;

	public StationFilterThread(Socket _in) {
		TAG = TAG + ":" + _in.getPort();
		Log.d(TAG, "Start StationFilterThread");
		
		inSocket = _in;
		
		new Thread(this, "StationFilterThread").start();
	}
	
	String readString(InputStream ins) {
		byte[] header = null;
		header = Utility.readNBytes(ins, HEADER_LEN);

		if (header[HEADER_LEN - 1] != '#') {
			Log.d(TAG,
					"Header format is wrong, the last character is not '#', maybe package size is too large.");
		}

		int len = 0;
		for (int i = 0; i < HEADER_LEN - 1; ++i)
			len = 10 * len + (header[i] - '0');

		return Utility.readNBytes(ins, len).toString();
	}

	int[] filter(String[] ss, String prefix){
		//if(ss.length > 1){
		//	int [] a = {1};
		//	return a;
		//}
		
		ArrayList<Integer> res = new ArrayList<Integer>();
		
		//for(int J=0; J < 100; ++J){
		for(int J=0; J < 1; ++J){

			for (int i = 0; i < ss.length; ++i) {
				if (StringUtil.startsWithoutDiacritics(ss[i], prefix)) {
					if(J == 0)
						res.add(i);
				} else {
					final String[] words = ss[i].split(" ");
					for (int k = 0; k < words.length; ++k) {
						if (StringUtil
								.startsWithoutDiacritics(words[k], prefix)) {

							if(J == 0)
								res.add(i);
							break;
						}
					}
				}
			}

		}
		
		int [] result = new int[ res.size() ];
		for(int i = 0; i < res.size(); ++i) result[i] = res.get(i);
		
		return result;
	}
	
	public void run() {
		// TODO Auto-generated method stub
		try {
			
			inSocket.setTcpNoDelay(true);
			InputStream ins = inSocket.getInputStream();
			OutputStream out = inSocket.getOutputStream();
			
			
			// while loop
			while(true){
				//String stations = readString(ins);
				//String pre = readString(ins);
				
				int header = Utility.readByte(ins, null);
				long start = System.nanoTime();
				if(header == Protocol.INIT){
					String s = Utility.simpleProtocolRead(ins, String.class,null);
					//Utility.delay();
					stations = s.split("\n");
					Log.d(TAG, "read station names is done.");
				}
				
				if(header == Protocol.FAKE){
					//Utility.delay(); //we only insert one delay in per iteration to simulate latency*2
					Utility.writeByte(out, Protocol.FAKE, null);
					continue;
				}

				if(header == Protocol.SEARCH){
					String pre = Utility.simpleProtocolRead(ins, String.class, null);
					//Utility.delay();
					Log.d(TAG, "read prefix(" + pre +") is done.");

					long mid_start = System.nanoTime();
					//ArrayList<Integer> res = filter(stations.split("\n"), pre);
					int[] res = filter(stations, pre);
					Log.d(TAG, "filter is done.");
					long mid_end = System.nanoTime();

					//StringBuilder sb = new StringBuilder();
					//for(int x : res){
					//	sb.append(x + ", ");
					//}
					//Log.d(TAG, "res=[" + sb.toString() + "]");

					Utility.simpleProtocolWrite(out, res,null);
					Log.d(TAG, "write is done.");
					Log.d(TAG, "Essential:\t" + (mid_end - mid_start));
				}		
				
				if(header != Protocol.INIT && header != Protocol.SEARCH)
					throw new RuntimeException("Unknown message type: "+header);
				long end = System.nanoTime();

				Log.d(TAG, "All:\t" + (end - start));
			}

		} catch (Exception e) {
			Log.d(TAG,"Error: StationFilterThread failed to run");
			e.printStackTrace();
		}
	}

}
