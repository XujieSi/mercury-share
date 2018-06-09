import edu.gatech.mcc.stabilization.Protocol;
import edu.gatech.protocol.Log;
import edu.gatech.util.Utility;
import georegression.fitting.MotionTransformPoint;
import georegression.fitting.affine.ModelManagerAffine2D_F64;
import georegression.fitting.homography.ModelManagerHomography2D_F64;
import georegression.fitting.se.ModelManagerSe2_F64;
import georegression.fitting.se.MotionSe2PointSVD_F64;
import georegression.struct.InvertibleTransform;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se2_F64;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import org.ddogleg.fitting.modelset.DistanceFromModel;
import org.ddogleg.fitting.modelset.ModelFitter;
import org.ddogleg.fitting.modelset.ModelGenerator;
import org.ddogleg.fitting.modelset.ModelManager;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.fitting.modelset.ransac.Ransac;

import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.tracker.PointTracker;
import boofcv.alg.sfm.d2.ImageMotionPointTrackerKey;
import boofcv.alg.sfm.d2.ImageMotionPtkSmartRespawn;
import boofcv.alg.sfm.robust.DistanceAffine2DSq;
import boofcv.alg.sfm.robust.DistanceHomographySq;
import boofcv.alg.sfm.robust.DistanceSe2Sq;
import boofcv.alg.sfm.robust.GenerateAffine2D;
import boofcv.alg.sfm.robust.GenerateHomographyLinear;
import boofcv.alg.sfm.robust.GenerateSe2_AssociatedPair;
import boofcv.factory.feature.tracker.FactoryPointTracker;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageUInt8;


public class BoofCVThread implements Runnable{
	String TAG = "BoofCVThread";
	Socket inSocket = null;
	OutputStream out;
	ImageMotionPtkSmartRespawn<ImageUInt8,Affine2D_F64> smartRespawn = null;
	PrintWriter logger = new PrintWriter(System.out);


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
			
				int header = ins.read();
				
				Log.d(TAG, "Read header time: " + (System.nanoTime()-start)/1000000.0);
				
				Log.d(TAG, header+" to read.");
				if(header != Protocol.INIT_STATEFUL && header!=Protocol.PROCESS_STATEFUl && header!= Protocol.RESET_STATEFUL &&
						header != Protocol.SET_TO_FIRST_STATEFUL && header != Protocol.INIT_STATELESS && header != Protocol.PROCESS_STATELESS){
					Log.d(TAG, "Unknown message type: "+header);
					throw new RuntimeException();
				}
				boolean ret = true;
				ImageMotionPtkSmartRespawn<ImageUInt8,Affine2D_F64> talgo = null;
				if(header == Protocol.INIT_STATELESS){
					talgo = Utility.simpleProtocolRead(ins, ImageMotionPtkSmartRespawn.class, logger);
					ImageUInt8 image = Utility.simpleProtocolRead(ins, ImageUInt8.class, logger);
					talgo.process(image);	
					talgo.getMotion().changeKeyFrame();
					talgo.getMotion().resetTransforms();
				}
				if(header == Protocol.PROCESS_STATELESS){
					talgo = Utility.simpleProtocolRead(ins, ImageMotionPtkSmartRespawn.class, logger);
					ImageUInt8 image = Utility.simpleProtocolRead(ins, ImageUInt8.class, logger);
					talgo.process(image);		
				}
				if(header == Protocol.RESET_STATEFUL){
					long start2 = System.nanoTime();
					this.smartRespawn.getMotion().reset();
					Log.d(TAG, "RESET time: "+(System.nanoTime() - start2)/1000000.0);
				}
				
				if(header == Protocol.SET_TO_FIRST_STATEFUL){
					this.smartRespawn.getMotion().changeKeyFrame();
					this.smartRespawn.getMotion().resetTransforms();
				}
				
				if(header == Protocol.INIT_STATEFUL && this.smartRespawn == null){
					ConfigGeneralDetector config = Utility.simpleProtocolRead(ins, ConfigGeneralDetector.class, logger);
					PointTracker<ImageUInt8> tracker = FactoryPointTracker.
							klt(new int[]{1, 2,4}, config, 3, ImageUInt8.class, ImageSInt16.class);
					this.smartRespawn = this.createSmartRespawn(100, 1.5, 2, 40,
							0.5, 0.6, false, tracker, new Affine2D_F64(), config);
				}
				
				if(header == Protocol.INIT_STATEFUL || header == Protocol.PROCESS_STATEFUl){
					ImageUInt8 image = Utility.simpleProtocolRead(ins, ImageUInt8.class, logger);
					long start1 = System.nanoTime();
					ret = smartRespawn.process(image);
					Log.d(TAG, "Process time: "+(System.nanoTime()-start1)/1000000.0);
				}
				
				if(header == Protocol.INIT_STATEFUL){
					smartRespawn.getMotion().changeKeyFrame();
					smartRespawn.getMotion().resetTransforms();
					ret = true;
				}
				
				Utility.delay();
				Utility.simpleProtocolWrite(out, ret, logger);
				if(header == Protocol.INIT_STATELESS || header == Protocol.PROCESS_STATELESS){
					Utility.simpleProtocolWrite(out, talgo, logger);
//					System.out.println("World to current: "+talgo.getMotion().getWorldToCurr());
//					System.out.println("Number of frames processed: "+talgo.getMotion().getTotalFramesProcessed());
				}else{
					Utility.simpleProtocolWrite(out, smartRespawn.getMotion().getWorldToCurr(), logger); // data1
					Utility.simpleProtocolWrite(out, smartRespawn.getMotion().getTotalFramesProcessed(), logger); //data2
					//				Utility.simpleProtocolWrite(out, smartRespawn.getMotion().getTracker().getActiveTracks(null), logger);
				}
				Log.d(TAG, "One iteration time: "+(System.nanoTime()-start)/1000000.0);
			}
		} catch (Exception e) {
			Log.d(TAG,"Error: StationFilterThread failed to run");
			e.printStackTrace();
		}
	}

	public <I extends ImageBase, IT extends InvertibleTransform>
	ImageMotionPtkSmartRespawn<I,IT> createSmartRespawn( int ransacIterations , double inlierThreshold,int outlierPrune,
			int absoluteMinimumTracks, double respawnTrackFraction,
			double respawnCoverageFraction,
			boolean refineEstimate ,
			PointTracker<I> tracker , IT motionModel,
			ConfigGeneralDetector trackerConfig) {

		ModelManager<IT> manager;
		ModelGenerator<IT,AssociatedPair> fitter;
		DistanceFromModel<IT,AssociatedPair> distance;
		ModelFitter<IT,AssociatedPair> modelRefiner = null;

		if( motionModel instanceof Homography2D_F64) {
			GenerateHomographyLinear mf = new GenerateHomographyLinear(true);
			manager = (ModelManager)new ModelManagerHomography2D_F64();
			fitter = (ModelGenerator)mf;
			if( refineEstimate )
				modelRefiner = (ModelFitter)mf;
			distance = (DistanceFromModel)new DistanceHomographySq();
		} else if( motionModel instanceof Affine2D_F64) {
			manager = (ModelManager)new ModelManagerAffine2D_F64();
			GenerateAffine2D mf = new GenerateAffine2D();
			fitter = (ModelGenerator)mf;
			if( refineEstimate )
				modelRefiner = (ModelFitter)mf;
			distance =  (DistanceFromModel)new DistanceAffine2DSq();
		} else if( motionModel instanceof Se2_F64) {
			manager = (ModelManager)new ModelManagerSe2_F64();
			MotionTransformPoint<Se2_F64, Point2D_F64> alg = new MotionSe2PointSVD_F64();
			GenerateSe2_AssociatedPair mf = new GenerateSe2_AssociatedPair(alg);
			fitter = (ModelGenerator)mf;
			distance =  (DistanceFromModel)new DistanceSe2Sq();
			// no refine, already optimal
		} else {
			throw new RuntimeException("Unknown model type: "+motionModel.getClass().getSimpleName());
		}

		ModelMatcher<IT,AssociatedPair>  modelMatcher =
				new Ransac(123123,manager,fitter,distance,ransacIterations,inlierThreshold);

		ImageMotionPointTrackerKey<I,IT> lowlevel =
				new ImageMotionPointTrackerKey<I, IT>(tracker,modelMatcher,modelRefiner,motionModel,outlierPrune);

		ImageMotionPtkSmartRespawn<I,IT> smartRespawn =
				new ImageMotionPtkSmartRespawn<I, IT>(lowlevel,
						absoluteMinimumTracks,respawnTrackFraction,respawnCoverageFraction );

		return smartRespawn;
	}



}