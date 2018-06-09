package edu.gatech.mcc.stabilization;

import edu.gatech.util.Utility;
import georegression.struct.InvertibleTransform;
import georegression.struct.affine.Affine2D_F64;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

import org.boofcv.android.DemoMain;

import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.tracker.PointTrack;
import boofcv.abst.sfm.d2.WrapImageMotionPtkSmartRespawn;
import boofcv.alg.sfm.d2.AssociatedPairTrack;
import boofcv.alg.sfm.d2.ImageMotionPtkSmartRespawn;
import boofcv.struct.image.ImageBase;

public class MCCWrapImageMotionPtkSmartRespawn<T extends ImageBase, IT extends InvertibleTransform>
extends WrapImageMotionPtkSmartRespawn<T, IT> {
	private Socket socket = null;
	private InputStream in = null;
	private OutputStream out = null;
	private ConfigGeneralDetector trackerConfig;
	private IT worldToCurr = null; // data1
	private Class<IT> modelType = null; //constant
	private long ticks; //data2
	private List<PointTrack> activeTracks; //data3

	public MCCWrapImageMotionPtkSmartRespawn(
			ImageMotionPtkSmartRespawn<T, IT> alg,Socket socket, ConfigGeneralDetector config, InputStream kryoConfig) {
		super(alg);
		this.socket = socket;
		if(socket != null){
			try {
				in = socket.getInputStream();
				out = socket.getOutputStream();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		this.trackerConfig = config;
		this.modelType = alg.getMotion().getModelType();
		Protocol.registerKyro(kryoConfig);
	}

	@Override
	public boolean process(T input) {
		boolean ret;
		if(this.ifOffload()){
			ret = this.processRemote(input);
		}
		else 
			ret = this.processLocal(input);
		return ret;
	}

	public boolean ifOffload(){
		return socket != null;
	}
	
	private boolean processLocal(T input){
		return super.process(input);
	}
	
	private boolean processRemote(T input){
		inliersMarked = false;
		if(Protocol.ifStateful){
			if(first){
				Utility.writeByte(out, Protocol.INIT_STATEFUL, DemoMain.logger);
				if(this.alg != null){
					Utility.simpleProtocolWrite(out, trackerConfig, DemoMain.logger);
				}
				Utility.simpleProtocolWrite(out, input, DemoMain.logger);
				this.alg = null;
				first = false;
			}else{
				Utility.writeByte(out, Protocol.PROCESS_STATEFUl, DemoMain.logger);
				Utility.simpleProtocolWrite(out, input, DemoMain.logger);
			}
		}else{
			if(first){
				Utility.writeByte(out, Protocol.INIT_STATELESS, DemoMain.logger);
				Utility.simpleProtocolWrite(out, alg, DemoMain.logger);
				Utility.simpleProtocolWrite(out, input, DemoMain.logger);
				first = false;
			}else{
				Utility.writeByte(out, Protocol.PROCESS_STATELESS, DemoMain.logger);
				Utility.simpleProtocolWrite(out, alg, DemoMain.logger);
				Utility.simpleProtocolWrite(out, input, DemoMain.logger);
			}		
		}
		return this.readRet();
	}
	
	@Override
	public Class<IT> getTransformType() {
		if(alg != null){
			return super.getTransformType();
		}else{
			return this.modelType;
		}
	}

	@Override
	public IT getFirstToCurrent() {
		if(alg != null){
			return super.getFirstToCurrent();
		}
		else{
			return this.worldToCurr;
		}
	}

	@Override
	public void reset() {
		first = true;
		if(alg != null){
			super.reset();
		}
		else{
			Utility.writeByte(out, Protocol.RESET_STATEFUL, DemoMain.logger);
			this.readRet();
		}
	}

	@Override
	public void setToFirst() {
		if(this.alg != null){
			super.setToFirst();
		}else{
			if(Protocol.ifStateful)
				Utility.writeByte(out, Protocol.SET_TO_FIRST_STATEFUL, DemoMain.logger);
			this.readRet();
		}
	}

	@Override
	protected void checkInitialize() {
		if(this.alg != null)
			super.checkInitialize();
		else{
		if( !inliersMarked ) {
			inliersMarked = true;

			List<PointTrack> active = this.activeTracks;

			allTracks.clear();

			long tick = this.ticks;
			inliers.resize(active.size());

			for( int i = 0; i < active.size(); i++ ) {
				PointTrack t = active.get(i);
				AssociatedPairTrack info = t.getCookie();
				allTracks.add(t);
				// if it was used in the previous update then it is in the inlier set
				inliers.data[i] = info.lastUsed == tick;
			}
		}
		throw new RuntimeException("We assume that activeTracks are not accessed!");
		}
	}
	
	private boolean readRet(){
		Boolean ret = Utility.simpleProtocolRead(in, Boolean.class, DemoMain.logger);
		if(Protocol.ifStateful){
			this.worldToCurr = (IT)Utility.simpleProtocolRead(in, Object.class, DemoMain.logger); //data1
			this.ticks = Utility.simpleProtocolRead(in, Integer.class, DemoMain.logger);
			//		this.activeTracks = Utility.simpleProtocolRead(in, List.class, DemoMain.logger);
		}else{
			this.alg = Utility.simpleProtocolRead(in, ImageMotionPtkSmartRespawn.class, DemoMain.logger);
		}
		return ret;
	}
}
