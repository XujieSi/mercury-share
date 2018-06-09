package edu.gatech.mcc.stabilization;

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

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;

import org.ddogleg.fitting.modelset.DistanceFromModel;
import org.ddogleg.fitting.modelset.ModelFitter;
import org.ddogleg.fitting.modelset.ModelGenerator;
import org.ddogleg.fitting.modelset.ModelManager;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.fitting.modelset.ransac.Ransac;

import android.R;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.tracker.PointTracker;
import boofcv.abst.sfm.d2.ImageMotion2D;
import boofcv.alg.sfm.d2.ImageMotionPointTrackerKey;
import boofcv.alg.sfm.d2.ImageMotionPtkSmartRespawn;
import boofcv.alg.sfm.robust.DistanceAffine2DSq;
import boofcv.alg.sfm.robust.DistanceHomographySq;
import boofcv.alg.sfm.robust.DistanceSe2Sq;
import boofcv.alg.sfm.robust.GenerateAffine2D;
import boofcv.alg.sfm.robust.GenerateHomographyLinear;
import boofcv.alg.sfm.robust.GenerateSe2_AssociatedPair;
import boofcv.factory.sfm.FactoryMotion2D;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageBase;

public class MCCFactoryMotion2D extends FactoryMotion2D {
	
	public static AssetManager res;

	private static Socket createSocket(){
		if(!Protocol.ifOffload)
			return null;
		try {
			EstablishNetworkTask netTask = new EstablishNetworkTask();
			netTask.execute("Net");
			return netTask.get();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {	
			throw new RuntimeException(e);
		}
	}

	/**
	 * Estimates the 2D motion of an image using different models.
	 *
	 * @param ransacIterations Number of RANSAC iterations
	 * @param inlierThreshold Threshold which defines an inlier.
	 * @param outlierPrune If a feature is an outlier for this many turns in a row it is dropped. Try 2
	 * @param absoluteMinimumTracks New features will be respawned if the number of inliers drop below this number.
	 * @param respawnTrackFraction If the fraction of current inliers to the original number of inliers drops below
	 *                             this fraction then new features are spawned.  Try 0.3
	 * @param respawnCoverageFraction If the area covered drops by this fraction then spawn more features.  Try 0.8
	 * @param refineEstimate Should it refine the model estimate using all inliers.
	 * @param tracker Point feature tracker.
	 * @param motionModel Instance of the model model used. Affine2D_F64 or Homography2D_F64
	 * @param <I> Image input type.
	 * @param <IT> Model model
	 * @return  ImageMotion2D
	 */
	public static <I extends ImageBase, IT extends InvertibleTransform>
	ImageMotion2D<I,IT> mccCreateMotion2D( int ransacIterations , double inlierThreshold,int outlierPrune,
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

		try {
			return new MCCWrapImageMotionPtkSmartRespawn<I, IT>(smartRespawn, createSocket(), trackerConfig, res.open("classes.txt"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	static class EstablishNetworkTask extends AsyncTask<String, Integer, Socket>{

		@Override
		protected Socket doInBackground(String... params) {
			try {
				Socket socket = new Socket(Protocol.serverIP, Protocol.serverListenPort);
				socket.setTcpNoDelay(true);
				return socket;
			} catch (UnknownHostException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

		}
	}

}
