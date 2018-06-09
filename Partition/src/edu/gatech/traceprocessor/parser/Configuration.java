package edu.gatech.traceprocessor.parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.gatech.traceprocessor.utils.ArraySet;
import edu.gatech.traceprocessor.utils.Utils;

public class Configuration {
	/*
	 * The read/write type constants
	 */
	public static final int OBJ_PARAM = 0;
	public static final int OBJ_STATIC = 1;
	public static final int OBJ_REF = 2;
	public static final int OBJ_JNI = 3;
	public static final int OBJ_JNI_STATIC = 4;
	public static final int OBJ_JNI_HOLD_ARRAY = 5;
	public static final int OBJ_JNI_REL_ARRAY = 6;
	public static final int OBJ_ARRAY = 7;
	public static final int OBJ_JNI_ARRAY = 8;
	public static final int OBJ_ARRAY_ALL = 9;
	public static final int OBJ_RET = 10;
	public static final int OBJ_DUMP = 11; // used to insert fake read/write for parameter passing and return values
	public static final int OBJ_NATIVE = 12; // native memory access

	public static Map<Character,Integer> primSizeMap;
	static{
		primSizeMap = new HashMap<Character,Integer>();
		primSizeMap.put('B', 1);
		primSizeMap.put('C', 2);
		primSizeMap.put('D', 8);
		primSizeMap.put('F', 4);
		primSizeMap.put('I', 4);
		primSizeMap.put('J', 8);
		primSizeMap.put('S', 2);
		primSizeMap.put('V', 0);
		primSizeMap.put('Z', 1);
		primSizeMap.put('L', 4);
	}
	
	public static double cloudSpeedupFactor = 10;
	public final static int defFldSize = 4;
	@Deprecated
	public static double bandwidth = 10*1024*1024/8.0; //in bytes per second(bps)
	private static double latency = 37*1000; //in microseconds
	public static double NEXUS_INST_TIME = 0.016;
	public static double KVM_TIME = 0.0058;
//	public static double instrTime = 0.016; // on nexus 7 2013
	public static double instrTime = 0.0058; 
	public final static int OBJ_LEVEL = 0;
	public final static int FLD_LEVEL = 1;
	public static int dataLevel = OBJ_LEVEL;
	public static boolean DEF_PIN = true;
	
	public static boolean useNetModel;
	public static double[][] transportTimes;
	
	public static int offloadingLimit = -1;
	
	public static Map<String,Integer> localMethMap = new HashMap<String, Integer>();
	
	private static List<ColocItem> colocItems = new ArrayList<ColocItem>();
	
	static Set<String> pinByDefMethods;
	
	//The gui threads, the first thread of the app is a GUI thread by default.
	public static Set<Integer> guiThreads = new HashSet<Integer>();
	
	public static boolean pinGUI = true;
	
	public static boolean performTune = false;
	
	public static String PSEUDO_FLOW_PATH = "./pseudo_fifo";
	
	public static int MINCUT_SOLVER = 1;
	
	public final static int PSUEDO_FLOW = 1;
	public final static int LP = 2;
	
	public static double runningTimeScale = 1.0;
	
	//Whether ignore all the data transfer time and network latency
	public static boolean ignoreNetworkLatency = false;
		
	public static String[] bannedMethodPrefix = {
		"Ljava/lang/ref/",
		"Ljava/lang/Daemons$ReferenceQueueDaemon",
		"Ljava/lang/Daemons$Finalizer"
	};
	
	public static String[] libPrefix = {
		"Ljava/",
		"Ljavax/",
		"Landroid/",
		"Lsun/",
		"Lsunw/",
		"Llibcore/",
		"Lorg/apache/",
		"Lorg/w3c/",
		"Lorg/xml/"
	};
	
	public static void setLibPrefixes(String line){
		String tokens[] = line.trim().split(",");
		libPrefix = new String[tokens.length];
		for(int i = 0 ; i < tokens.length; i++){
			String token = tokens[i];
			token = token.trim().replace(".", "/");
			libPrefix[i] = "L"+token;
		}
	}
	
	public static boolean isLibraryMethod(String methName){
		for(String pre : libPrefix)
			if(methName.startsWith(pre))
				return true;
		return false;
	}
	
	public static boolean isMethodBanned(String mName){
		for(String prefix : bannedMethodPrefix)
			if(mName.startsWith(prefix))
				return true;
		return false;
	}
	
	/**
	 *	Read the network model. Its format: first line to be the latency, the following are the time without latency to
	 *  transfer the data. Require the network model file to be in milliseconds. 
	 * @param path
	 */
	public static void loadNetworkModel(String path){
		useNetModel = true;
		bandwidth = 1024*1024*1024.0; //1GB/s
		ArrayList<Double> dataSize = new ArrayList<Double>();
		ArrayList<Double> transTime = new ArrayList<Double>();
		try{
			BufferedReader r = new BufferedReader(new FileReader(path));
			String line;
			while((line = r.readLine()) !=null){
				String[] items = line.split("\t");
				if(items.length < 2)
					throw new RuntimeException("Illegal network model files!");
				double fir = Double.parseDouble(items[0]);
				//load the transfer time for fir (in millisecs)
				double sec = Double.parseDouble(items[1]);
				if(fir == 0){
					latency = sec*1000;
				}else{
//					int ds = Integer.parseInt(items[0]);
					double ds = fir;
					dataSize.add(ds);
//					long ts = Long.parseLong(items[1])*1000;
					//convert millisecs to microsecs
					double ts = sec*1000;
					transTime.add(ts);
					if(ts!=0){
						//TODO: potential bug. Don't use code related to bandwidth
						double transRate = (ds*1000.0)/ts;
						if(transRate < bandwidth)
							bandwidth=transRate;
					}
				}
			}
			r.close();
		}catch(Exception e){
			Utils.printError("Path to network model file incorrect");
			System.exit(1);
		}
		if(dataSize.size() == 0)
			throw new RuntimeException("The network model should contain at least one region");
		transportTimes = new double[2][dataSize.size()];
		for(int i = 0 ; i < dataSize.size(); i++){
			transportTimes[0][i] = dataSize.get(i);
			transportTimes[1][i] = transTime.get(i);
		}
	}
	
	public static void loadLocalMethods(String path){
		String line = null;
		try{
			BufferedReader r = new BufferedReader(new FileReader(path));
			while((line = r.readLine()) != null){
				if(line.startsWith("#"))
					continue;
				String[] items = line.split("\\s+");
				localMethMap.put(items[0],Integer.parseInt(items[1]));
			}
		}catch(Exception e){
			System.out.println(line);
			throw new RuntimeException(e);
		}
	}
	
	public static void loadCoLocMethods(String path){
		try{
			BufferedReader r = new BufferedReader(new FileReader(path));
			String line;
			colocItems = new ArrayList<ColocItem>();
			ColocItem curItem = new ColocItem();
			colocItems.add(curItem);
			while((line = r.readLine()) != null){
				if(line.startsWith("#"))
					continue;
				line = line.trim();
				if(line.equals("")){
					curItem = new ColocItem();
					colocItems.add(curItem);
				}else{
					curItem.interp(line);
				}
			}
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
//	public static void loadCoLocMethodInstances(String path){
//		try{
//			BufferedReader r = new BufferedReader(new FileReader(path));
//			String line;
//			while((line = r.readLine()) != null){
//				if(line.startsWith("#"))
//					continue;
//				String lineNums[] = line.split("\\s+");
//				Set<Integer> coSet = new HashSet<Integer>();
//				for(String lineNum : lineNums){
//					coSet.add(Integer.parseInt(lineNum));
//				}
//				coLocMethLines.add(coSet);
//			}
//		}catch(Exception e){
//			throw new RuntimeException(e);
//		}
//	}
	
	public static int getMethodPinTag(String methName){
		if(localMethMap.containsKey(methName))
			return localMethMap.get(methName);
		else
			return Method.UNDEF;
	}
	
	public static boolean isMethodPinned(Method m){
		Method parent = m;
		while(parent!=null){
			if(isSubtreeUnpinned(parent))
				return false;
			parent = parent.getCaller();
		}
		int pinTag = getMethodPinTag(m.methName());
		if(pinTag == Method.PINNED || pinTag == Method.SPEC_PINNED)
			return true;
//		if(pinTag == Method.STATEFULL && ifPinCol)
//			return true;
		if(pinTag == Method.UNDEF && m.isNative())
			return DEF_PIN;
		return false;
	}
	
	
	private static boolean isSubtreeUnpinned(Method m){
		int pinTag = getMethodPinTag(m.methName());
		if(pinTag == Method.UNPIN_TREE)
			return true;
		return false;
	}
	
	public static Set<ColocKey> getColocKey(Method m){
		Set<ColocKey> ret = new ArraySet<ColocKey>();
		for(ColocItem ci : colocItems){
			ret.addAll(ci.match(m));
		}
		return ret;
	}
	
//	public static Set<String> getColNameSet(String methName){
//		for(Set<String> colSet : coLocMethNames)
//			if(colSet.contains(methName))
//				return colSet;
//		return null;
//	}
	
//	public static Set<Integer> getColLineSet(int line){
//		for(Set<Integer> colSet : coLocMethLines)
//			if(colSet.contains(line))
//				return colSet;
//		return null;
//	}
	
	public static int getDataSize(char type){
		return primSizeMap.get(type);
	}
	
	public static double getTransportTime(Program p, Method m){
		if(ignoreNetworkLatency)
			return 0;
		else
			return applyModel(p.getInputSize(m), -1) + latency + applyModel(p.getOutputSize(m), -1) + latency;
	}
	
	public static double getTransportTime(Data d){
		if(ignoreNetworkLatency)
			return 0;
		else
			return applyModel(d.getSize(), -1);
	}
	
	public static double getTransportTime(int dsize){
		if(ignoreNetworkLatency)
			return 0;
		else
			return applyModel(dsize, -1);
	}
	
	/*
	 * Return the sorted array of data values that triggers the start of a new linear
	 * region in the cost function. The first entry should be 1 i.e a[0] = 1
	 */
	public static double[] getTransportTimeCutOffs(){
		if(ignoreNetworkLatency)
			throw new RuntimeException("IgnoreNetworkLatency is set.");
		else
			return transportTimes[0];
	}
	
	//Region indicates the linear region to choose;
	//Region ranges from 1 to N
	public static double getTransportTime(Data d, int region){
		if(ignoreNetworkLatency)
			return 0;
		else
			return applyModel(d.getSize(), region);
	}
	
	/**
	 * Return the data transfer time without latency
	 * @param size
	 * @param region
	 * @return
	 */
	public static double applyModel(int size, int region){
		if(ignoreNetworkLatency)
			return 0;	
		if(!useNetModel){
//			return (size/bandwidth)*1000*1000;
			throw new RuntimeException("Please load a network model!");
		}
		else{
			//Assuming that first non-latency line in the network model file
			//is for data of size non-zero.
			int numRegs = transportTimes[0].length;
			if(region < 1){
				int i = 0;
				for(; i < numRegs; i++){
					if(size < transportTimes[0][i])
						break;
				}
				i = i - 1;
				if(i < 0)
					i = 0;
				if(i >= numRegs)
					i = numRegs - 1;
				return (((double)size)/transportTimes[0][i]) * transportTimes[1][i] ;
			}else{
				if(region > numRegs){
					return (((double)size)/transportTimes[0][numRegs-1])*transportTimes[1][numRegs-1];
				}else if (region == 1){
					return (((double)size)/transportTimes[0][0]) * transportTimes[1][0] ;
				}else{
			//		return transportTimes[1][region-1] * (0.5) + transportTimes[1][lp+1] * (lp+1-p); 
					return (((double)size)/transportTimes[0][region-2]) * transportTimes[1][region-2] ;	
				}
			}
		}
	}
		
	public static double getLocalExclusiveExecutionTime(Method m){
		return m.getExclusiveTime() * Configuration.runningTimeScale;
	}
	
	public static double getRemoteExclusiveExecutionTime(Method m){
		return m.getExclusiveTime() * Configuration.runningTimeScale/Configuration.cloudSpeedupFactor;
	}
	
	public static double getLocalInclusiveTime(Method m){
		return m.getInclusiveTime() * Configuration.runningTimeScale;
	}

	public static double getRemoteInclusiveTime(Method m){
		return m.getInclusiveTime() * Configuration.runningTimeScale/Configuration.cloudSpeedupFactor;
	}
	
	//return latency, in micro-secs
	public static double getLatency(){
		if(Configuration.ignoreNetworkLatency)
			return 0;
		else
			return Configuration.latency;
	}
}
