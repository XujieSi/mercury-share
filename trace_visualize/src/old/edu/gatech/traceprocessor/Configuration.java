package old.edu.gatech.traceprocessor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

import edu.gatech.traceprocessor.utils.Utils;

public class Configuration {
	public final static double cloudSpeedupFactor = 10;
	public static double bandwidth = 10*1024*1024/8.0; //in bytes per second(bps)
	public static double latency = 37*1000; //in microseconds
	public static boolean inlineOpt = true;
	public static boolean mergeVar = true;
	public static boolean removeLocal = true;
	//public static double latency = 37*1000; // micros
	//Xin: It seems like the following fields are useless
	@Deprecated
	public final static int shiftFactor = 10000;
	@Deprecated
	public final static double SYSCALLDELAY = 0.0037;
	
	public static boolean useNetModel;
	public static long[][] transportTimes;
	
	public static int offloadingLimit = -1;

	
	/**
	 *	Read the network model. Its format: first line to be the latency, the following are the time without latency to
	 *  transfer the data. Require the network model file to be in milliseconds. 
	 * @param path
	 */
	public static void setNetworkModel(String path){
		useNetModel = true;
		bandwidth = 1024*1024*1024.0; //1GB/s
		ArrayList<Long> dataSize = new ArrayList<Long>();
		ArrayList<Long> transTime = new ArrayList<Long>();
		try{
			BufferedReader r = new BufferedReader(new FileReader(path));
			String line;
			while((line = r.readLine()) !=null){
				String[] items = line.split("\t");
				if(items.length < 2)
					throw new RuntimeException("Illegal network model files!");
				int fir = Integer.parseInt(items[0]);
				int sec = Integer.parseInt(items[1]);
				if(fir == 0){
					latency = sec*1000;
				}else{
					int ds = Integer.parseInt(items[0]);
					dataSize.add((long)ds);
					long ts = Long.parseLong(items[1])*1000;
					transTime.add(ts);
					if(ts!=0){
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
		transportTimes = new long[2][dataSize.size()];
		for(int i = 0 ; i < dataSize.size(); i++){
			transportTimes[0][i] = dataSize.get(i);
			transportTimes[1][i] = transTime.get(i);
		}
	}
	
	
	public static double getTransportTime(Function f){
	//	return ((f.getSubTreeInputSize() + f.getSubTreeOutputSize())/Configuration.bandwidth)*1000
	//			+ Configuration.latency; //(data in bytes, bw in bps. Hence, this computes time in ms)
		
		return applyModel(f.getSubtreeInputSize(), -1) + latency + applyModel(f.getSubtreeOutputSize(), -1) + latency;
	}
	
	public static double getTransportTime(VarEntry v){
	//	return (v.getSize()/Configuration.bandwidth)*1000; //(data in bytes, bw in Bps. Hence, this computes time in ms)
		return applyModel(v.getSize(), -1);
	}
	
	/*
	 * Return the sorted array of data values that triggers the start of a new linear
	 * region in the cost function. The first entry should be 1 i.e a[0] = 1
	 */
	public static long[] getTransportTimeCutOffs(){
		return transportTimes[0];
	}
	
	//Region indicates the linear region to choose;
	//Region ranges from 1 to N
	public static double getTransportTime(VarEntry v, int region){
		//return (v.getSize()/Configuration.bandwidth)*1000; //(data in bytes, bw in Bps. Hence, this computes time in ms)
		return applyModel(v.getSize(), region);
	}
	
	/**
	 * Return the data transfer time without latency
	 * @param size
	 * @param region
	 * @return
	 */
	public static double applyModel(int size, int region){
		if(!useNetModel){
			return (size/bandwidth)*1000*1000;
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
		
	public static double getLocalRemainderExecutionTime(Function f){
		return f.getRemainderExecutionTime();
	}
	
	public static double getRemoteRemainderExecutionTime(Function f){
		return f.getRemainderExecutionTime()/Configuration.cloudSpeedupFactor;
	}
	
	public static double getLocalExecutionTime(Function f){
		//	return fun.getRemoteExecutionTime()*SPEEDUP;
		return f.getExecutionTime();
	}

	public static double getRemoteExecutionTime(Function f){
		//	return fun.getRemoteExecutionTime()*SPEEDUP;
		return f.getExecutionTime()/Configuration.cloudSpeedupFactor;
	}
	
}
