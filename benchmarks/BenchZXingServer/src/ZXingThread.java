import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.EnumMap;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import edu.gatech.protocol.Log;
import edu.gatech.util.Utility;

public class ZXingThread implements Runnable{
	String TAG = "ZxingThread";
	Socket inSocket = null;
	OutputStream out;


	public ZXingThread(Socket _in) {
		TAG = TAG + ":" + _in.getPort();
		Log.d(TAG, "Start CollisionThread");
		
		inSocket = _in;
	}
	
	public void run() {
		try {	
			inSocket.setTcpNoDelay(true);
			InputStream ins = inSocket.getInputStream();
			out = inSocket.getOutputStream();
				
			PrintWriter logger = new PrintWriter(System.out);
			// while loop
			while(true){
				//String stations = readString(ins);
				//String pre = readString(ins);
//				int[] pixels = Utility.simpleProtocolRead(ins, int[].class, logger);
//				int width = Utility.simpleProtocolRead(ins, Integer.class, logger);
//				int height = Utility.simpleProtocolRead(ins, Integer.class, logger);
				RGBLuminanceSource src = Utility.simpleProtocolRead(ins, RGBLuminanceSource.class, logger);
				long start = System.nanoTime();
				String rs = null;
//				RGBLuminanceSource src = new RGBLuminanceSource(width,height,pixels);
//				BinaryBitmap bitmap1 = new BinaryBitmap(new HybridBinarizer(src));
				//			time1 =  android.os.Process.getElapsedCpuTime();
				try {
//					if(!ifTryHard){
//						Result r = new MultiFormatReader().decode(bitmap1);
//						if(r!=null)
//							rs = r.toString();
//					}
//					else{
						BinaryBitmap bitmap1 = new BinaryBitmap(new HybridBinarizer(src));
						EnumMap<DecodeHintType,Object> hints = new EnumMap(DecodeHintType.class);
						hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
						Result r = new MultiFormatReader().decode(bitmap1,hints);
						if (r != null)
							rs = r.toString();
//					}
				} catch (NotFoundException e) {
					rs = null;
				}
				Log.d(TAG, "Proccing time: "+(System.nanoTime()-start)/Utility.NANO_TO_MILI);
				//Utility.delay();
				Utility.simpleProtocolWrite(out, rs, logger);
			}
		} catch (Exception e) {
			Log.d(TAG,"Error: ZxingThread failed to run");
			e.printStackTrace();
		}
	}

}