package edu.gatech.traceprocessor.offloadingalgorithms.solver.mincut;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.gatech.traceprocessor.parser.Configuration;
import edu.gatech.traceprocessor.parser.Data;
import edu.gatech.traceprocessor.parser.FldData;
import edu.gatech.traceprocessor.parser.Instruction;
import edu.gatech.traceprocessor.parser.Method;
import edu.gatech.traceprocessor.parser.ObjData;
import edu.gatech.traceprocessor.parser.Program;

// uni-directional stateful
public class MinCutOffloadingAlgorithm3 extends MinCutOffloadingAlgorithm {

	public MinCutOffloadingAlgorithm3(Program p) {
		super(p);
	}


	@Override
	public boolean isStateful() {
		return true;
	}

	private void computeCapsControlDependencyRecursively(Method m){
		Method caller = m.getCaller();
		double lcap = Configuration.getLocalExclusiveExecutionTime(m);
		super.addSrcCapacity(m, lcap);
		double rcap = Configuration.getRemoteExclusiveExecutionTime(m);
		super.addDstCapacity(m, rcap);
		double transferTime = Configuration.getLatency()*2;
		if(caller == null){
			super.addDstCapacity(m, transferTime);
		}else{
			double localTime = this.getLocalTime();
			super.addCapacity(caller, m, localTime);
			super.addCapacity(m, caller, transferTime);
		}
		for(Method c : m.getCallees())
			this.computeCapsControlDependencyRecursively(c);
	}

	private void computeCapsDataDependency(){
		Collection<Data> datas = program.getData().values();
		Map<SimpleDataKey, Integer> dataMap = new HashMap<SimpleDataKey,Integer>();
		for(Data d : datas){
			if((!(d instanceof ObjData)) || Configuration.dataLevel == Configuration.OBJ_LEVEL){
				for(List<Instruction> accessors : d.groupAccessors()){
					Method writer = accessors.get(0).getMethod();
					Set<Method> readers = new HashSet<Method>();
					for(int i = 1 ; i < accessors.size(); i ++){
						readers.add(accessors.get(i).getMethod());
					}
					readers.remove(writer);
					if(readers.size() == 0)
						continue;
					SimpleDataKey dk = new SimpleDataKey();
					dk.writer = writer;
					dk.readers = readers;
					Integer edSize = dataMap.get(dk);
					if(edSize == null)
						edSize = 0;
					dataMap.put(dk, edSize + d.getSize());
				}
			}else{ //field level
				ObjData od = (ObjData)d;
				for(FldData fd : od.getFields().values()){
					for(List<Instruction> accessors : fd.groupAccessors()){
						Method writer = accessors.get(0).getMethod();
						Set<Method> readers = new HashSet<Method>();
						for(int i = 1 ; i < accessors.size(); i ++){
							readers.add(accessors.get(i).getMethod());
						}
						readers.remove(writer);
						if(readers.size() == 0)
							continue;
						SimpleDataKey dk = new SimpleDataKey();
						dk.writer = writer;
						dk.readers = readers;
						Integer edSize = dataMap.get(dk);
						if(edSize == null)
							edSize = 0;
						dataMap.put(dk, edSize + fd.getSize());
					}				
				}
			}
		}
		
		for(Map.Entry<SimpleDataKey, Integer> entry : dataMap.entrySet()){
			SimpleDataKey dk = entry.getKey();
			double transportTime = Configuration.getTransportTime(entry.getValue());
			
			//if(dk.readers.size() == 1){
			//	Method writer = dk.writer;
			//	Method reader = dk.readers.iterator().next();
			//	super.addCapacity(writer, reader, transportTime);
			//	super.addCapacity(reader, writer, transportTime);
			//}
			//else{
				int cloudDataNode = super.generateAuxiliaryNodeId();
				int mobileDataNode = super.generateAuxiliaryNodeId();
				double localTime = super.getLocalTime();
				for(Method r : dk.readers){
					int rid = super.getMethId(r);
					//mobileDataNode --infinity--> reader
					super.addCapacity(mobileDataNode, rid, localTime);
					//reader --infinity--> cloudDataNode
					super.addCapacity(rid, cloudDataNode, localTime);
				}
				// cloudDataNode --data transfer time--> writer
				int wid = super.getMethId(dk.writer);
				super.addCapacity(cloudDataNode, wid, transportTime);
				// writer --data transer time--> mobileDataNode
				super.addCapacity(wid, mobileDataNode, transportTime);
			//}
		}
	}
	
	@Override
	protected void populateGraph() {
		processPinAndColocation();
		/*(
		//pin
		for(Method m : program.getPinnedMethods()){
			this.pinMethod(m);
		}

		//colocation
		for(Set<Method> colSet : program.getColocSet()){
			Method last = null;
			double localTime = super.getLocalTime();
			for(Method m : colSet){
				if(last != null){
					super.addCapacity(m, last, localTime);
					super.addCapacity(last, m, localTime);
				}
				last = m;
			}
		}*/

		for(Method t : program.getThreads())
			this.computeCapsControlDependencyRecursively(t);
		this.computeCapsDataDependency();
	}
	
}
