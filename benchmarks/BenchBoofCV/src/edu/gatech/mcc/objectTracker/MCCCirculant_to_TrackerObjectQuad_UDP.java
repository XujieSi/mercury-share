package edu.gatech.mcc.objectTracker;

import edu.gatech.util.Utility;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.struct.shapes.Quadrilateral_F64;
import georegression.struct.shapes.RectangleLength2D_F32;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.boofcv.android.DemoMain;

import boofcv.abst.tracker.Circulant_to_TrackerObjectQuad;
import boofcv.abst.tracker.ConfigCirculantTracker;
import boofcv.alg.tracker.circulant.CirculantTracker;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;

public class MCCCirculant_to_TrackerObjectQuad_UDP<T extends ImageSingleBand> extends
		Circulant_to_TrackerObjectQuad<T> {
	
	//private Socket socket;
	private DatagramSocket socket;
	
	//private OutputStream output;
	private DatagramSocket output;
	
	//private InputStream input;
	private DatagramSocket input;
	
	private InetAddress serverIPAddr;
	
	private ConfigCirculantTracker trackerConfig;
	private Class<T> trackerImageType; 

	public MCCCirculant_to_TrackerObjectQuad_UDP(CirculantTracker<T> tracker,
			ImageType<T> imageType, DatagramSocket socket, ConfigCirculantTracker config, Class<T> trackerImageType) {
		super(tracker, imageType);
		this.socket = socket;
		this.input = socket;
		this.output = socket;
		
		this.trackerConfig = config;
		this.trackerImageType = trackerImageType;
		
		
		try {
			this.serverIPAddr = InetAddress.getByName(Protocol.serverIP);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public boolean initialize(T image, Quadrilateral_F64 location) {
		UtilPolygons2D_F64.bounding(location, rect);

		int width = (int)(rect.p1.x - rect.p0.x);
		int height = (int)(rect.p1.y - rect.p0.y);

		if(!Protocol.ifStateful){
			tracker.initialize(image,(int)rect.p0.x,(int)rect.p0.y,width,height);
		}
		else{
			//Utility.writeByteWithoutFlush(output, Protocol.INIT_STATEFUL, DemoMain.logger);
			Utility.writeByteUDP(output, serverIPAddr, Utility.UDP_SERVER_PORT,  Protocol.INIT_STATEFUL, DemoMain.logger);
			
			//Utility.simpleProtocolWrite(output, trackerConfig, DemoMain.logger);
			//Utility.simpleProtocolWrite(output, trackerImageType, DemoMain.logger);
			//Utility.simpleProtocolWrite(output, image, DemoMain.logger);
			//Utility.simpleProtocolWrite(output, rect, DemoMain.logger);

			Utility.simpleProtocolWriteUDP(output, serverIPAddr, Utility.UDP_SERVER_PORT, trackerConfig, DemoMain.logger);
			Utility.simpleProtocolWriteUDP(output, serverIPAddr, Utility.UDP_SERVER_PORT, trackerImageType, DemoMain.logger);
			Utility.simpleProtocolWriteUDP(output, serverIPAddr, Utility.UDP_SERVER_PORT, image, DemoMain.logger);
			Utility.simpleProtocolWriteUDP(output, serverIPAddr, Utility.UDP_SERVER_PORT, rect, DemoMain.logger);
			
		}

		return true;	
	}

	@Override
	public boolean process(T image, Quadrilateral_F64 location) {

//		tracker.performTracking(image);
//		RectangleLength2D_F32 r = tracker.getTargetLocation();
		
		RectangleLength2D_F32 r = null;
		
		if(Protocol.ifStateful){
			//Utility.writeByteWithoutFlush(output, Protocol.PROCESS_STATEFUl, DemoMain.logger);
			Utility.writeByteUDP(output, serverIPAddr, Utility.UDP_SERVER_PORT, Protocol.PROCESS_STATEFUl, DemoMain.logger);
			
			//Utility.simpleProtocolWrite(output, image, DemoMain.logger);
			Utility.simpleProtocolWriteUDP(output, serverIPAddr, Utility.UDP_SERVER_PORT, image, DemoMain.logger);
		}else{
			//Utility.writeByteWithoutFlush(output, Protocol.PROCESS_STATELESS, DemoMain.logger);
			Utility.writeByteUDP(output, serverIPAddr, Utility.UDP_SERVER_PORT, Protocol.PROCESS_STATELESS, DemoMain.logger);
			
			//Utility.simpleProtocolWrite(output, tracker, DemoMain.logger);
			//Utility.simpleProtocolWrite(output, image, DemoMain.logger);

			Utility.simpleProtocolWriteUDP(output, serverIPAddr, Utility.UDP_SERVER_PORT, tracker, DemoMain.logger);
			Utility.simpleProtocolWriteUDP(output, serverIPAddr, Utility.UDP_SERVER_PORT, image, DemoMain.logger);
			
			//tracker = Utility.simpleProtocolRead(input, tracker.getClass(), DemoMain.logger);
			tracker = Utility.simpleProtocolReadUDP(input, tracker.getClass(), DemoMain.logger);
		}
		
		//r = Utility.simpleProtocolRead(input, RectangleLength2D_F32.class, DemoMain.logger);
		r = Utility.simpleProtocolReadUDP(input, RectangleLength2D_F32.class, DemoMain.logger);

		if( r.x0 >= image.width || r.y0 >= image.height )
			return false;
		if( r.x0+r.width < 0 || r.y0+r.height < 0 )
			return false;

		float x0 = r.x0;
		float y0 = r.y0;
		float x1 = r.x0 + r.width;
		float y1 = r.y0 + r.height;

		location.a.x = x0;
		location.a.y = y0;
		location.b.x = x1;
		location.b.y = y0;
		location.c.x = x1;
		location.c.y = y1;
		location.d.x = x0;
		location.d.y = y1;

		return true;	}
	
}
