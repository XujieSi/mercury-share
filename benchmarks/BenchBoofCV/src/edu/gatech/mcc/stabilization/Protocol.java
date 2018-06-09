package edu.gatech.mcc.stabilization;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.Scanner;

import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.objenesis.strategy.StdInstantiatorStrategy;

import boofcv.alg.transform.pyramid.PyramidDiscreteSampleBlur;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageType;
import edu.gatech.util.Utility;



public class Protocol {
	public static int serverListenPort = 9903;
//	public final static String serverIP = "10.0.2.2"; // emulator to host
//	public final static String serverIP = "128.61.241.226"; //pag01
//	public final static String serverIP = "192.168.1.122";
	public final static String serverIP = "192.168.2.1";
	//public final static String serverIP = "128.61.241.236"; // fir06
	
//	public final static String serverIP = "192.168.1.105";
//	public static String serverIP = "143.215.206.59"; //lte macbook

	public static int INIT_STATEFUL = 1;
	public static int PROCESS_STATEFUl = 2;
	public static int RESET_STATEFUL = 3;
	public static int SET_TO_FIRST_STATEFUL = 4;

	public static int INIT_STATELESS = 5;
	public static int PROCESS_STATELESS = 6;
	public static boolean hasInit = false;
		
	public static boolean ifOffload =   true; // false; //
	public static boolean ifStateful = false; //true; // 

	public static void registerKyro(InputStream configFile){		
		if(hasInit)
			return;
		Utility.kryo.register(QueueCorner.class, new QueueCornerSerializer());
		Utility.kryo.register(Random.class, new RandomSerializer());
		Utility.kryo.register(Constructor.class, new ConstructorSerializer());
		Utility.kryo.register(Method.class, new MethodSerializer());
//	    Utility.kryo.register(ImageUInt8.class, new JavaSerializer());
//	    Utility.kryo.register(SingleBandGenerator.class, new SingleBandGeneratorSerializer());
	    StdInstantiatorStrategy stdStr = new StdInstantiatorStrategy();
	    Utility.kryo.getRegistration(Ransac.class).setInstantiator(stdStr.newInstantiatorOf(Ransac.class));
//		Utility.kryo.getRegistration(PointTrackerKltPyramid.class).setInstantiator(stdStr.newInstantiatorOf(PointTrackerKltPyramid.class));
	    Utility.kryo.getRegistration(PyramidDiscreteSampleBlur.class).setInstantiator(stdStr.newInstantiatorOf(PyramidDiscreteSampleBlur.class));
//	    Utility.kryo.getRegistration(GenericConvolveDown.class).setInstantiator(stdStr.newInstantiatorOf(GenericConvolveDown.class));
	    Utility.kryo.getRegistration(ImageType.class).setInstantiator(stdStr.newInstantiatorOf(ImageType.class));
//	    Utility.kryo.getRegistration(boofcv.alg.transform.pyramid.PyramidDiscreteSampleBlur.class).setInstantiator(stdStr.newInstantiatorOf(boofcv.alg.transform.pyramid.PyramidDiscreteSampleBlur.class));
//	    Utility.kryo.getRegistration(boofcv.core.image.inst.SingleBandGenerator.class).setInstantiator(stdStr.newInstantiatorOf(boofcv.core.image.inst.SingleBandGenerator.class));
//	    Utility.kryo.getRegistration(boofcv.abst.feature.detect.extract.WrapperNonMaximumBlock.class).setInstantiator(stdStr.newInstantiatorOf(boofcv.abst.feature.detect.extract.WrapperNonMaximumBlock.class));
//	    Utility.kryo.getRegistration(boofcv.abst.feature.detect.intensity.WrapperGradientCornerIntensity.class).setInstantiator(stdStr.newInstantiatorOf(boofcv.abst.feature.detect.intensity.WrapperGradientCornerIntensity.class));
//	    Utility.kryo.getRegistration(boofcv.alg.feature.detect.intensity.impl.ImplShiTomasiCorner_S16.class).setInstantiator(stdStr.newInstantiatorOf(boofcv.alg.feature.detect.intensity.impl.ImplShiTomasiCorner_S16.class));
//	    Utility.kryo.getRegistration(boofcv.alg.feature.detect.extract.SelectNBestFeatures.class).setInstantiator(stdStr.newInstantiatorOf(boofcv.alg.feature.detect.extract.SelectNBestFeatures.class));
//	    Utility.kryo.getRegistration(boofcv.abst.filter.derivative.ImageGradient_Reflection.class).setInstantiator(stdStr.newInstantiatorOf(boofcv.abst.filter.derivative.ImageGradient_Reflection.class));
	    
//	    List<ClassLoader> classLoadersList = new LinkedList<ClassLoader>();
//	    classLoadersList.add(ClasspathHelper.contextClassLoader());
//	    classLoadersList.add(ClasspathHelper.staticClassLoader());
	    
//	    String[] prefixes = {"boofcv.alg","boofcv.abst", "boofcv.core"};
//
//	    for(String pre: prefixes){
//	    	Reflections reflections = new Reflections(new ConfigurationBuilder()
//	    	.setScanners(new SubTypesScanner(false /* don't exclude Object.class */), new ResourcesScanner())
//	    	.setUrls(ClasspathHelper.forClassLoader(classLoadersList.toArray(new ClassLoader[0])))
//	    	.filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix(pre))));
//
//	    	Set<Class<?>> classes = reflections.getSubTypesOf(Object.class);
//	    	for(Class c : classes){
//	    		System.out.println(c.getName());
//	    		Utility.kryo.getRegistration(c).setInstantiator(stdStr.newInstantiatorOf(c));
//	    	}
//	    }
	    try {
			Scanner sc = new Scanner(configFile);
			while(sc.hasNextLine()){
				Class c = Class.forName(sc.nextLine());
				Utility.kryo.getRegistration(c).setInstantiator(stdStr.newInstantiatorOf(c));
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

////	    Utility.kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
////	    InstantiatorStrategy defaultInstantiatorStrategy = new DefaultInstantiatorStrategy();
////	    Utility.kryo.getRegistration(ArrayList.class).setInstantiator(defaultInstantiatorStrategy.newInstantiatorOf(ArrayList.class));
		
//		Utility.kryo.register(GeneralFeatureDetector.class, new ImageMotionPointTrackerKeySerializer());
	    hasInit = true;
	}

}
