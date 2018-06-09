import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import boofcv.abst.tracker.ConfigCirculantTracker;
import boofcv.alg.tracker.circulant.CirculantTracker;
import boofcv.factory.tracker.FactoryTrackerObjectAlgs;
import boofcv.struct.image.ImageUInt8;
import edu.gatech.mcc.objectTracker.Protocol;
import edu.gatech.protocol.Log;
import edu.gatech.util.Utility;
import georegression.struct.shapes.Rectangle2D_F64;
import georegression.struct.shapes.RectangleLength2D_F32;

public class BoofCVThread implements Runnable{
	String TAG = "ObjectTrackerThread";
	Socket inSocket = null;
	OutputStream out;
	PrintWriter logger = new PrintWriter(System.out);
	CirculantTracker<ImageUInt8> alg;

	public BoofCVThread(Socket _in) {
		TAG = TAG + ":" + _in.getPort();
		Log.d(TAG, "Start BoofCVThread");
		
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

				int header = Utility.readByte(ins, logger);	
				Log.d(TAG, "Read header time: " + (System.nanoTime()-start)/1000000.0);	
				if(header == Protocol.INIT_STATEFUL){
					ConfigCirculantTracker config = Utility.simpleProtocolRead(ins, ConfigCirculantTracker.class, logger);
					Class<ImageUInt8> imageType = Utility.simpleProtocolRead(ins, Class.class, logger);
					ImageUInt8 image = Utility.simpleProtocolRead(ins, ImageUInt8.class, logger);
					Rectangle2D_F64 rect = Utility.simpleProtocolRead(ins, Rectangle2D_F64.class, logger);
					
					long start1 = System.nanoTime();
					
					alg = FactoryTrackerObjectAlgs.circulant(config,imageType);
					int width = (int)(rect.p1.x - rect.p0.x);
					int height = (int)(rect.p1.y - rect.p0.y);
					alg.initialize(image,(int)rect.p0.x,(int)rect.p0.y,width,height);
					Log.d(TAG, "Processing time: " + (System.nanoTime()-start1)/1000000.0);	
				}else{
					RectangleLength2D_F32 r = null;
					long start1;
					if(header == Protocol.PROCESS_STATELESS){
						alg = Utility.simpleProtocolRead(ins, CirculantTracker.class, logger);
						ImageUInt8 image = Utility.simpleProtocolRead(ins, ImageUInt8.class, logger);
						start1 = System.nanoTime();
						alg.performTracking(image);
					}else
						if(header == Protocol.PROCESS_STATEFUl){
							ImageUInt8 image = Utility.simpleProtocolRead(ins, ImageUInt8.class, logger);
							start1 = System.nanoTime();
							alg.performTracking(image);
						}
						else
							throw new RuntimeException("Unknown header "+header+".");
					r = alg.getTargetLocation();
					Log.d(TAG, "Processing time: " + (System.nanoTime()-start1)/1000000.0);	
					if(header == Protocol.PROCESS_STATELESS)
						Utility.simpleProtocolWrite(out, alg, logger);
					Utility.simpleProtocolWrite(out, r, logger);
				}
				
				Log.d(TAG, "One iteration time: "+(System.nanoTime()-start)/1000000.0);
			}
		} catch (Exception e) {
			Log.d(TAG,"Error: StationFilterThread failed to run");
			e.printStackTrace();
		}
	}

}