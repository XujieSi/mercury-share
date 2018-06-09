package edu.gatech.mcc.objectTracker;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;

import android.content.res.AssetManager;
import android.os.AsyncTask;
import boofcv.abst.tracker.Circulant_to_TrackerObjectQuad;
import boofcv.abst.tracker.ConfigCirculantTracker;
import boofcv.abst.tracker.TrackerObjectQuad;
import boofcv.alg.tracker.circulant.CirculantTracker;
import boofcv.factory.tracker.FactoryTrackerObjectAlgs;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;

public class MCCFactoryTrackerObjectQuad {
	
	public static AssetManager res;
	public static Socket socket;

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
			return new MCCCirculant_to_TrackerObjectQuad<T>(alg,ImageType.single(imageType), createSocket(), config, imageType);
		}
		else{
			return new Circulant_to_TrackerObjectQuad<T>(alg,ImageType.single(imageType));
		}
	}
	
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
	
	static class EstablishNetworkTask extends AsyncTask<String, Integer, Socket>{

		@Override
		protected Socket doInBackground(String... params) {
			try {
				if(socket == null){
					socket = new Socket(Protocol.serverIP, Protocol.serverListenPort);	
				}
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
