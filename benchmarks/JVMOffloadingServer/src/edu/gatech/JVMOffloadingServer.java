package edu.gatech;

import java.util.Hashtable;

import edu.gatech.jobinstance.JobInstance;
import edu.gatech.offloading.ClientOffloadingTask;
import edu.gatech.offloading.OffloadExecutionServer;
import edu.gatech.offloading.ResultListener;
import edu.gatech.test.LatencyTestListener;
import edu.gatech.test.NetworkMeasureServer;
import edu.gatech.test.NetworkMeasureServerUDT;
import edu.gatech.test.NetworkMeasureServer_UDP;
import edu.gatech.test.PowerMeasureServer;

public class JVMOffloadingServer {

	public static Hashtable<String, ClientOffloadingTask> remoteTaskTable = new Hashtable<String, ClientOffloadingTask>();
	public static Hashtable<String, JobInstance> jobTable = new Hashtable<String, JobInstance>();

	public static void main(String[] args){		
		//new ResultListener(remoteTaskTable);
		//new OffloadExecutionServer(jobTable);	
		//new LatencyTestListener();
		//new NetworkMeasureServer();
		new NetworkMeasureServer_UDP();
		//new NetworkMeasureServerUDT();
		
		//new PowerMeasureServer();
	}

}
