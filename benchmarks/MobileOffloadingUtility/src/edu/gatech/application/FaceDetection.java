package edu.gatech.application;

import java.io.*;
import java.util.Random;

import edu.gatech.offloading.ClientExecutionController;
import edu.gatech.offloading.OffloadExecutionServer;
import edu.gatech.offloading.Offloadable;
import edu.gatech.offloading.RemoteProxyWrapper;
import edu.gatech.protocol.OffloadingMode;
import edu.gatech.protocol.Utility;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;

//import android.util.Log;
import edu.gatech.protocol.Log;

public class FaceDetection extends Offloadable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	byte[] mfigure;
	byte[] transferLoadingControl;
	
	OffloadingMode offMode;

	public FaceDetection() {
		super(null, null);
	}

	public FaceDetection(ClientExecutionController ec, Context context, OffloadingMode off,
			String path) {
		super(ec, context);

		offMode = off;
		File f = new File(path);
		int size = (int) f.length();
		mfigure = new byte[size];
		InputStream ins = null;
		int len = 0;
		try {
			ins = new FileInputStream(f);
			len = ins.read(mfigure);
			ins.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		transferLoadingControl = new byte[10 * len];
	}

	@Override
	public void runOffloading() {

		FaceInfo[] faces = detectFaces();

	}

	public FaceInfo[] detectFaces() {
		//long start = System.currentTimeMillis();
		long start = System.nanoTime();
		Log.d("FaceDetection", " detectFaces start to run at " + start);
		
		Utility.logTime("" + offMode, "faceDetection-begin", start);
		
		Class<?>[] paramTypes = { int.class };
		Object[] paramValues = { 0 };
		FaceInfo[] results = null;
		try {
			
			//if(Utility.isLocal(offMode)){
			//	testParamTrans(0);
			//}
			//else
			
			for (int i = 1; i <= 3; ++i) {
				if (Utility.isLocal(offMode)) {
					results = nonOffloadingExecution(0);
				} else if (Utility.isBidirectional(offMode)) {
					results = (FaceInfo[]) exectl.execute("localDetectFacesBi",
							paramTypes, paramValues, this);
				} else {
					results = localDetectFacesUni();
				}

				Log.d("detectFaces", "The " + i + "-th round is done");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		long end = System.nanoTime();
		String msg = "running duration :" + (end - start);

		Log.d("FaceDetection", " detectFaces ended at " + end + ", duration=" + (end-start));
		Utility.logTime("faceDetection", "faceDetection-end", end);
		
		notifyJobIsDone(msg);

		return results;
	}

	public void perf() {
		long begin = System.nanoTime();
		// decode file
		ByteArrayInputStream in = new ByteArrayInputStream(mfigure);
		Bitmap myBitmap = BitmapFactory.decodeStream(in);
		// detect faces
		int imageWidth = myBitmap.getWidth();
		int imageHeight = myBitmap.getHeight();
		int numberOfFace = 100;
		Face[] myFace = new FaceDetector.Face[numberOfFace];
		FaceDetector myFaceDetect = new FaceDetector(imageWidth, imageHeight,
				numberOfFace);

		for (int i = 0; i < 20; ++i) {
			myFaceDetect.findFaces(myBitmap, myFace);
		}
		
		long end = System.nanoTime();
		Log.d("facedetection_perf", "" + (end - begin));
	}
	
	// totally local execution
	public FaceInfo[] nonOffloadingExecution(int num) {
		FaceInfo [] res = null;
		int i = 10;
		while (--i >= 0) res = localDetectFaces_orig(0);

		readSomeMagicNumber(0);
		return res;
	}

	// schedule at server side
	public FaceInfo[] localDetectFacesBi(int num) {
		FaceInfo [] res = null;
		int i = 10;
		while (--i >= 0) res = localDetectFaces_orig(0);

		// No matter whether stateful or stateless, client call back is exactly the same, 
		// no need to send whole object back
		RemoteProxyWrapper<FaceDetection> wrap = new RemoteProxyWrapper<FaceDetection>(true); // assume call static method from client
		wrap.callRemote(this, "readSomeMagicNumber", 0);

		return res;
	}

	// schedule at client side
	public FaceInfo[] localDetectFacesUni() {

		Class<?>[] paramTypes = { int.class };
		Object[] paramValues = { 0 };
		FaceInfo[] res = null;
		
		try {
			
			int i = 10;
			while (--i >= 0)
				res = (FaceInfo[]) exectl.execute("localDetectFaces_orig", paramTypes,
						paramValues, this);
		} catch (Exception e) {
			Log.d("localDetectFaceUni", "exectl execute localDetectFaces_orig failed");
		}

		readSomeMagicNumber(0);
		
		return res;
	}
	
	public Face[] K_getFaces(int noUse){
		// decode file
		ByteArrayInputStream in = new ByteArrayInputStream(mfigure);
		Bitmap myBitmap = BitmapFactory.decodeStream(in);

		// detect faces
		int imageWidth = myBitmap.getWidth();
		int imageHeight = myBitmap.getHeight();
		int numberOfFace = 100;
		Face[] myFace = new FaceDetector.Face[numberOfFace];
		FaceDetector myFaceDetect = new FaceDetector(imageWidth, imageHeight,
				numberOfFace);

		myFaceDetect.findFaces(myBitmap, myFace);

		return myFace;
	}
	

	public static FaceInfo []  extractFaceInfo(Face[] myFace){
		// construct results
		int numberOfFaceDetected = 100;
		FaceInfo[] results = new FaceInfo[numberOfFaceDetected];
		PointF myMidPoint = new PointF();

		for (int i = 0; i < myFace.length; i++) {
			if(myFace[i] == null) continue;
			myFace[i].getMidPoint(myMidPoint);
			
			results[i] = new FaceInfo(myMidPoint.x, myMidPoint.y,
					myFace[i].eyesDistance());
		}
		return results;
	}
	
	public static FaceInfo[] doubleFaces(FaceInfo[] faces){
		FaceInfo[] res = new FaceInfo[ 2* faces.length ];
		for(int i=0; i < 2 * faces.length; ++i){
			int j = i % faces.length;
			res[i] = faces[j];
		}
		return res;
	}
	
	
	public FaceInfo[] testParamTrans(int noUseParam) {
		
		RemoteProxyWrapper<FaceDetection> wrap = new RemoteProxyWrapper<FaceDetection>(true); // assume call static method from client
		//wrap.callRemote(this, "readSomeMagicNumber", 0);
		
		FaceInfo[] a = new FaceInfo[1];
		a[0] = new FaceInfo(0,0,0);
		
		int i = 20;
		while( --i >= 0){
			FaceInfo[] b = null;
			int j = 20;
			while(--j >=0)
				b = (FaceInfo[]) wrap.callRemote(this, "doubleFaces", (Object) a);
		
			a = b;
		}
		
		//Face[] myFace = getFaces(mfigure);
		//Face[] myFace = (Face[]) wrap.callRemote(this, "getFaces", mfigure);
		
		//Integer magicValue = readSomeMagicNumber(0);
		
		//FaceInfo[] res = (FaceInfo[]) wrap.callRemote(this, "extractFaceInfo", (Object) myFace);

		Log.d("TestDoubleFaces", "a.size = " + a.length);
		
		return a;
		//return extractFaceInfo(myFace);
	}
	
	
	public  FaceInfo[] localDetectFaces_orig(int num) {
		// decode file
		ByteArrayInputStream in = new ByteArrayInputStream(mfigure);
		Bitmap myBitmap = BitmapFactory.decodeStream(in);

		// detect faces
		int imageWidth = myBitmap.getWidth();
		int imageHeight = myBitmap.getHeight();
		int numberOfFace = 100;
		Face[] myFace = new FaceDetector.Face[numberOfFace];
		FaceDetector myFaceDetect = new FaceDetector(imageWidth, imageHeight,
				numberOfFace);

		int numberOfFaceDetected = 0;
		numberOfFaceDetected = myFaceDetect.findFaces(myBitmap, myFace);
		
		// construct results
		FaceInfo[] results = new FaceInfo[numberOfFaceDetected];
		PointF myMidPoint = new PointF();

		for (int i = 0; i < numberOfFaceDetected; i++) {
			myFace[i].getMidPoint(myMidPoint);
			results[i] = new FaceInfo(myMidPoint.x, myMidPoint.y,
					myFace[i].eyesDistance());
		}

		return results;
	}
	
	public static Integer readSomeMagicNumber(int x) {
		Random random = new Random();
		Integer res = random.nextInt();
		
		try{
			FileOutputStream out = new FileOutputStream("/sdcard/a.txt");
			out.write(res);
			out.close();
		}
		catch(IOException e){
			e.printStackTrace();
		}
		
		Log.d("readSomeMagicNumber", "the magic number is " + res);
		return res;
	}

}

class FaceInfo implements Serializable {
	public float x;
	public float y;
	public float eyedistance;

	public FaceInfo(float px, float py, float distance) {
		x = px;
		y = py;
		eyedistance = distance;
	}

}