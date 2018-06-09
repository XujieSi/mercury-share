package edu.gatech.mcc.objectTracker;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;

import edu.gatech.util.Utility;

import android.content.res.AssetManager;
import android.os.AsyncTask;
import boofcv.abst.tracker.Circulant_to_TrackerObjectQuad;
import boofcv.abst.tracker.ConfigCirculantTracker;
import boofcv.abst.tracker.TrackerObjectQuad;
import boofcv.alg.tracker.circulant.CirculantTracker;
import boofcv.factory.tracker.FactoryTrackerObjectAlgs;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;

public class MCCFactoryTrackerObjectQuad_UDP {
	
	public static AssetManager res;
	public static DatagramSocket socket;


	/**
	 * Creates the Circulant feature tracker.  Texture based tracker which uses the theory of circulant matrices,
	 * Discrete Fourier Transform (DCF), and linear classifiers to track a target.  Fixed sized rectangular target
	 * and only estimates translation.  Can't detect when it loses track or re-aquire track.
	 *
	 * @see CirculantTracker
	 *
	 * @param config Configuration
	 * @return CirculantTracker
	 */
	public static <T extends ImageSingleBand>
	TrackerObjectQuad<T> circulant( ConfigCirculantTracker config , Class<T> imageType ) {

		CirculantTracker<T> alg = FactoryTrackerObjectAlgs.circulant(config,imageType);

		if(Protocol.ifOffload){
			try {
				Protocol.registerKyro(res.open("classes.txt"));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return new MCCCirculant_to_TrackerObjectQuad_UDP<T>(alg,ImageType.single(imageType), createSocket(), config, imageType);
		}
		else{
			return new Circulant_to_TrackerObjectQuad<T>(alg,ImageType.single(imageType));
		}
	}
	
	private static DatagramSocket createSocket(){
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
	
	static class EstablishNetworkTask extends AsyncTask<String, Integer, DatagramSocket>{

		@Override
		protected DatagramSocket doInBackground(String... params) {
			while(socket == null)
			try {
				if(socket == null){
					socket = new DatagramSocket( Utility.UDP_CLIENT_PORT );
				}
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return socket;
		}

	}

}
