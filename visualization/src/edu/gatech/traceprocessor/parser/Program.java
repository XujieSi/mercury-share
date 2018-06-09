package edu.gatech.traceprocessor.parser;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.gatech.traceprocessor.utils.ArraySet;
import edu.gatech.traceprocessor.utils.InstBitSet;
import edu.gatech.traceprocessor.utils.Pair;
import edu.gatech.traceprocessor.utils.Utils;


/**
 * The normal flow of using this class:
 * 1. loadBinary
 * 2. calculateInOutSet
 * 3. 
 * @author xin
 *
 */
public class Program {
	List<Method> threads;
	String traceName;
	int lastThread = -1;
	int curLineNum = 1;
//	public Map<ColocKey,Map<DataKey,Integer>> jniArrayMap;
	Map<DataKey, Data> addrToData;
	ArrayList<Instruction> instDomain;
	InstBitSet insts;
	Map<Method,Set<Data>> inputMap;
	Map<Method,Set<Data>> outputMap;
	Set<Method> pinnedMethods;
	Set<Method> unpinnedMethods;
	Map<ColocKey,Set<Method>> configedColocMap;
	Set<Set<Method>> colocSet;
	Set<Method> noninlinableSet;
	Map<Integer,Integer> threadBanStack = new HashMap<Integer,Integer>();
	Map<Long,Integer> addrGCMap;
		
	public Program(){
		threads = new ArrayList<Method>();
		addrToData = new HashMap<DataKey,Data>();
//		jniArrayMap = new HashMap<ColocKey, Map<DataKey,Integer>>();
		instDomain = new ArrayList<Instruction>();
		instDomain.add(null);
		this.pinnedMethods = new HashSet<Method>();
		this.colocSet = new HashSet<Set<Method>>();
		this.unpinnedMethods = new HashSet<Method>();
		this.noninlinableSet = new HashSet<Method>();
		insts = new InstBitSet(instDomain,10000);
		addrGCMap = new HashMap<Long,Integer>();
		configedColocMap = new HashMap<ColocKey,Set<Method>>();
	}
	
	public boolean addThread(Method m){
		for(Method em : threads)
			if(em.getThreadID() == m.getThreadID())
				return false;
		threads.add(m);
		return true;
	}
	
	public Method getThread(int id){
		for(Method m : threads)
			if(m.getThreadID() == id)
				return m;
		return null;
	}
	
	/**
	 * Release memory by destructive udpate. Only keep the callgraph information.
	 */
	public void destory(){
		instDomain.clear();
		for(Method t : threads)
			destroyMethodRecursively(t);
	}
	
	private void destroyMethodRecursively(Method m){
		for(Method c : m.getCallees())
			destroyMethodRecursively(c);
		m.destory();
	}
	
	public Method getThreadTopMethod(int id){
		for(Method m : threads)
			if(m.getThreadID() == id){
				return findTopMethod(m);
			}
		return null;
	}
	
	public Map<DataKey,Data> getData(){
		return Collections.unmodifiableMap(addrToData);
	}
	
	private Method findTopMethod(Method cur){
		while(cur.callees.size() != 0)
			cur = cur.callees.get(cur.callees.size() - 1);
		while(cur.isClosed())
			cur = cur.caller;
		return cur;
	}
	
	public void loadBinary(String inputPath){
		Utils.printLogWithTime("Begin to load binary trace file: "+inputPath);
		this.traceName = inputPath;
		try{
			DataInputStream r = new DataInputStream(new BufferedInputStream(new FileInputStream(inputPath)));
			
			out:while(true){
				try{
					int type = (int)r.readByte();
					int tid = 1;
					Method curMeth = null;
					if(type != 5) // not native lib information
					{	
						tid = (int)r.readShort();
						curMeth = this.getThreadTopMethod(tid);
						if(curMeth == null){
							Utils.printLogWithTime("Dummy thread top level method inserted at line "+curLineNum+1+" for Thread "+tid);
							this.insertMethEntry(tid, null, "thread_"+tid+"(V)", Method.genFakeAddr(), 0, 0, false);
							curMeth = this.getThreadTopMethod(tid);
						}
					}

					switch(type){
					case 1://the method entry
					{
						long time = Utils.intToUnsigned(r.readInt()); // no unsigned int in java
						long count = Utils.intToUnsigned(r.readInt()); // no unsigned int in java
						long addr = Utils.intToUnsigned(r.readInt());
						Byte isNative = r.readByte();
						boolean nativeMeth = (isNative !=0);
						String name = Utils.readStringUntilSpace(r);
						if(curMeth != null && curMeth.getStartTime() > time ){
							throw new RuntimeException(curLineNum+"\tStart time in trace of caller is after start time of callee!");
						}
						if(curMeth != null && curMeth.getStartCount() > count){
							throw new RuntimeException(curLineNum+"\tStart count in trace of caller is after start count of callee!");
						}
						this.insertMethEntry(tid, curMeth, name, addr, count, time, nativeMeth);
					}
						break;
					case 2://the method exit
					{
						long time = Utils.intToUnsigned(r.readInt());
						long count = Utils.intToUnsigned(r.readInt());
						long addr = Utils.intToUnsigned(r.readInt());
						this.insertMethExit(tid, curMeth, addr, time, count);
					}
						break;
					case 3: //read
					{
						byte obj_type = r.readByte();
						long addr = Utils.intToUnsigned(r.readInt());
						int size = r.readInt(); // well, in the dalvik side, the size is written as unsigned int
						int offset = r.readInt();
						this.insertRead(tid, curMeth, obj_type, addr, offset, size, Configuration.defFldSize);
					}
						break;
					case 4: //write
					{
						byte obj_type = r.readByte();
						long addr = Utils.intToUnsigned(r.readInt());
						int size = r.readInt(); // well, in the dalvik side, the size is written as unsigned int
						int offset = r.readInt();// currently not used
						this.insertWrite(tid, curMeth, obj_type, addr, offset, size, Configuration.defFldSize);
					}
						break;
					case 5: //native lib
						parseNativeBin(r);
						break;
					case 6:
						long addr = Utils.intToUnsigned(r.readInt());
						int size = r.readInt();
						this.insertAlloc(tid, curMeth, addr, size);
						break;
					case 37:
						System.out.println("Find the magic number "+37+". terminate.");
						break out;
					default:
						System.out.println("Unknown event type: "+(int)type);
						System.exit(1);
					}
				}catch(EOFException e){
					System.out.println("Reach the end of the file");
					break;
				}
			}
			r.close();
			closeMethods();
			fixTime();
		}catch(IOException e){
			throw new RuntimeException(e);
		}
	}
	
	public void load(String inputPath){
		try{
			this.loadPlain(inputPath);
		}catch(RuntimeException e){
			this.loadBinary(inputPath);
		}
	}
	
	public void loadPlain(String inputPath){
		Utils.printLogWithTime("Begin to load plain trace file: "+inputPath);
		this.traceName = inputPath;
		try{
			BufferedReader r = new BufferedReader(new FileReader(inputPath));
			String line;
			
			out:while((line = r.readLine())!= null){
					if(line.startsWith("1<nl")){
						System.out.println("Native method informatioN: "+line);
						continue;
					}
				
					int i = line.indexOf("<")+1;
					int tid = Integer.parseInt(line.substring(0, i-1));
	
					Method curMeth = null;
					curMeth = this.getThreadTopMethod(tid);
					if(curMeth == null){
						Utils.printLogWithTime("Dummy thread top level method inserted at line "+curLineNum+1+" for Thread "+tid);
						this.insertMethEntry(tid, null, "thread_"+tid+"(V)", Method.genFakeAddr(), 0, 0, false);
						curMeth = this.getThreadTopMethod(tid);
					}
					
					line = line.substring(i);
					if(line.startsWith("fun")){
						long time = 0L; // no unsigned int in java
						long count = 0L; // no unsigned int in java
						long addr = 0L;
						boolean nativeMeth = false;
						String name = null;
						line=line.substring(0, line.lastIndexOf(">"));
						String[] params = line.split(" ");
						for(int k = 0; k < params.length; k++){
							String[] items = params[k].split("=");
							if(items.length <= 1)
								continue;
							if(items[0].compareTo("n") == 0){
								//function name;
								name = items[1];
							}
							else if(items[0].compareTo("a") == 0){
								//function address
								addr = Long.parseLong(items[1]);
							}
							else if(items[0].compareTo("nt") == 0){
								if(Integer.parseInt(items[1]) == 1)
									nativeMeth = true;
							}
							else if(items[0].compareTo("t") == 0){
								time = Long.parseLong(items[1]);
							}
							else if(items[0].compareTo("c") == 0){
								count = Long.parseLong(items[1]);
							}
						}
						if(curMeth.getStartTime() > time ){
							throw new RuntimeException(curLineNum+"\tStart time in trace of caller is after start time of callee!");
						}
						if(curMeth.getStartCount() > count){
							throw new RuntimeException(curLineNum+"\tStart count in trace of caller is after start count of callee!");
						}
						this.insertMethEntry(tid, curMeth, name, addr, count, time, nativeMeth);
					}
					else if(line.startsWith("/fun")){
						long time = 0L;
						long count = 0L;
						long addr = 0L;
						line=line.substring(0, line.lastIndexOf(">"));
						String[] params = line.split(" ");
						for(int k = 0; k < params.length; k++){
							String[] items = params[k].split("=");
							if(items.length <= 1)
								continue;
							else if(items[0].compareTo("a") == 0){
								//function address
								addr = Long.parseLong(items[1]);
							}
							else if(items[0].compareTo("t") == 0){
								time = Long.parseLong(items[1]);
							}
							else if(items[0].compareTo("c") == 0){
								count = Long.parseLong(items[1]);
							}
						}
						this.insertMethExit(tid, curMeth, addr, time, count);
					}
					else if(line.startsWith("w")){
						int obj_type = 0;
						long addr = 0;
						int size = 0; // well, in the dalvik side, the size is written as unsigned int
						int offset = 0;// currently not used
						line=line.substring(0, line.lastIndexOf("/>"));
						String[] params = line.split(" ");
						for(int k = 0; k < params.length; k++){
							String[] items = params[k].split("=");
							if(items.length <= 1)
								continue;
							else if(items[0].compareTo("t") == 0){
								//object type
								obj_type = Integer.parseInt(items[1]);
							}
							else if(items[0].compareTo("v") == 0){
								//object address
								addr = Long.parseLong(items[1]);
							}
							else if(items[0].compareTo("p") == 0){
								//offset
								offset = Integer.parseInt(items[1]);
							}
							else if(items[0].compareTo("s") == 0){
								//size
								size = Integer.parseInt(items[1]);
							}
						}
						this.insertWrite(tid, curMeth, obj_type, addr, offset, size, Configuration.defFldSize);
					}
					else if(line.startsWith("r")){
						int obj_type = 0;
						long addr = 0;
						int size = 0; // well, in the dalvik side, the size is written as unsigned int
						int offset = 0;// currently not used
						line=line.substring(0, line.lastIndexOf("/>"));
						String[] params = line.split(" ");
						for(int k = 0; k < params.length; k++){
							String[] items = params[k].split("=");
							if(items.length <= 1)
								continue;
							else if(items[0].compareTo("t") == 0){
								//object type
								obj_type = Integer.parseInt(items[1]);
							}
							else if(items[0].compareTo("v") == 0){
								//object address
								addr = Long.parseLong(items[1]);
							}
							else if(items[0].compareTo("p") == 0){
								//offset
								offset = Integer.parseInt(items[1]);
							}
							else if(items[0].compareTo("s") == 0){
								//size
								size = Integer.parseInt(items[1]);
							}
						}
						this.insertRead(tid, curMeth, obj_type, addr, offset, size, Configuration.defFldSize);
					}else if(line.startsWith("a")){
						long addr = 0;
						int size = 0; // well, in the dalvik side, the size is written as unsigned int
						line=line.substring(0, line.lastIndexOf("/>"));
						String[] params = line.split(" ");
						for(int k = 0; k < params.length; k++){
							String[] items = params[k].split("=");
							if(items.length <= 1)
								continue;
							else if(items[0].compareTo("v") == 0){
								//object address
								addr = Long.parseLong(items[1]);
							}
							else if(items[0].compareTo("s") == 0){
								//size
								size = Integer.parseInt(items[1]);
							}
						}					
						this.insertAlloc(tid, curMeth, addr, size);
					}
			}
			r.close();
			closeMethods();
			fixTime();
		}catch(IOException e){
			throw new RuntimeException(e);
		}
	}
	
	public void removeThreadLocalInsts(){
		InstBitSet insToRm = new InstBitSet(instDomain,10000);
		for(Method t : this.threads)
			this.removeThreadLocalRecursively(t,insToRm);
		this.insts.removeAll(insToRm);
		Set<DataKey> addrsForDataRM = new HashSet<DataKey>();
		for(Map.Entry<DataKey, Data> entry : addrToData.entrySet()){
			if(!entry.getValue().isThreadShared())
				addrsForDataRM.add(entry.getKey());
		}
		for(DataKey addr : addrsForDataRM)
			addrToData.remove(addr);
	}
	
	public void inlineUnColoc(){
		for(Method t : this.getThreads()){
			this.inlineUnColocRecusively(t);
		}
	}
	
	/**
	 * The rule is to inline a method which satisfies both of the following conditions:
	 * 1. itself is not colocated
	 *2. its parent is not colocated(disabled)
	 * 
	 * @param m
	 * @param isRootPinned
	 * @return
	 */
	private boolean inlineUnColocRecusively(Method m){
		boolean ret = true;
		List<Method> methToInline = new ArrayList<Method>();
		boolean isCurrentColoc = this.isMethodColocated(m);
		for(Method c : m.getCallees()){
			boolean isCInlined = this.inlineUnColocRecusively(c);
			if(isCInlined)
				methToInline.add(c);
			ret &= isCInlined;
		}
		for(Method c : methToInline)
			m.inline(c);
		ret &= (!isCurrentColoc);
		return ret;	
	}
	
	public void removeUnColoc(){
		for(Method t : this.getThreads()){
			this.removeUnColocRecusively(t);
		}
	}
	
	/**
	 * The rule is to remove a method which satisfies both of the following conditions:
	 * 1. itself is not colocated
	 *2. its parent is not colocated(disabled)
	 * 
	 * @param m
	 * @param isRootPinned
	 * @return
	 */
	private boolean removeUnColocRecusively(Method m){
		boolean ret = true;
		List<Method> methToRemove = new ArrayList<Method>();
		boolean isCurrentColoc = this.isMethodColocated(m);
		for(Method c : m.getCallees()){
			boolean isCRemoved = this.removeUnColocRecusively(c);
			if(isCRemoved)
				methToRemove.add(c);
			ret &= isCRemoved;
		}
		for(Method c : methToRemove)
			m.removeMethod(c);
		ret &= (!isCurrentColoc);
		return ret;	
	}
	
	public void inlineUnpinned(){
		for(Method t : this.getThreads())
			this.inlineUnpinnedRecursively(t, false);
	}
	
	/**
	 * The rule is to inline a method which satisfies both of the following conditions:
	 * 1. itself is not pinned
	 * 2. its parent is not pinned
	 * 
	 * @param m
	 * @param isRootPinned
	 * @return
	 */
	private boolean inlineUnpinnedRecursively(Method m, boolean isRootPinned){
		boolean ret = true;
		List<Method> methToInline = new ArrayList<Method>();
		boolean isCurrentPinned = this.isMethodPinned(m);
		for(Method c : m.getCallees()){
			boolean isCInlined = inlineUnpinnedRecursively(c, isCurrentPinned);
			if(isCInlined && !isRootPinned)
				methToInline.add(c);
			ret &= isCInlined;
		}
		for(Method c : methToInline)
			m.inline(c);
		ret &= (!isCurrentPinned);
		return ret;
	}
		
	public void removeUnpinned(){
		for(Method t : this.getThreads())
			this.removeUnpinnedRecursively(t, false);
	}
	
	/**
	 * The rule is to inline a method which satisfies both of the following conditions:
	 * 1. itself is not pinned
	 * 2. its parent is not pinned
	 * 
	 * @param m
	 * @param isRootPinned
	 * @return
	 */
	private boolean removeUnpinnedRecursively(Method m, boolean isRootPinned){
		boolean ret = true;
		List<Method> methToRemove = new ArrayList<Method>();
		boolean isCurrentPinned = this.isMethodPinned(m);
		for(Method c : m.getCallees()){
			boolean isCRemoved = removeUnpinnedRecursively(c, isCurrentPinned);
			if(isCRemoved && !isRootPinned)
				methToRemove.add(c);
			ret &= isCRemoved;
		}
		for(Method c : methToRemove)
			m.removeMethod(c);;
		ret &= (!isCurrentPinned);
		return ret;
	}
	public void writePlain(String path) throws FileNotFoundException{
		PrintWriter pw = new PrintWriter(new File(path));
		for(Instruction inst : this.insts)
			pw.println(inst.toPlainFormat());
		pw.flush();
		pw.close();
	}
	
	public List<Method> getThreads(){
		return Collections.unmodifiableList(threads);
	}
	
	private boolean removeThreadLocalRecursively(Method m, InstBitSet instsToRmSet){
		boolean isThreadLocal = true;
		List<Method> cToRm = new ArrayList<Method>(); 
		for(Method child : m.callees){
			if(removeThreadLocalRecursively(child, instsToRmSet))
				cToRm.add(child);
			else
				isThreadLocal = false;
		}
		m.callees.removeAll(cToRm);
		SortedSet<Instruction> instToRm = new TreeSet<Instruction>();
		SortedSet<Instruction> newInsts = new TreeSet<Instruction>();
		for(Instruction inst : m.instructions)
			if(inst instanceof Read){
				Read r = (Read)inst;
				if(!r.data.isThreadShared())
					instToRm.add(r);
				else{
					isThreadLocal = false;
					newInsts.add(r);
				}
			}else
				if(inst instanceof Write){
					Write w = (Write)inst;
					if(!w.data.isThreadShared())
						instToRm.add(w);
					else{
						isThreadLocal = false;
						newInsts.add(w);
					}
				}
				else if(inst instanceof Alloc){
					Alloc a = (Alloc)inst;
					if(!a.d.isThreadShared){
						instToRm.add(a);
					}else{
						isThreadLocal = false;
						newInsts.add(a);
					}
				}
				else if(inst instanceof MethodEntry){
					MethodEntry entry = (MethodEntry)inst;
					if(!m.callees.contains(entry.curMeth))
						instToRm.add(entry);
					else
						newInsts.add(entry);
				}
				else if(inst instanceof MethodExit){
					MethodExit exit = (MethodExit)inst;
					if(!m.callees.contains(exit.curMeth))
						instToRm.add(exit);
					else
						newInsts.add(exit);
				}
				else
					throw new RuntimeException("Unknown instruction type: " + inst);
		m.instructions = newInsts;
		instsToRmSet.addAll(instToRm);
		return isThreadLocal;
	}
	
	private void closeMethods(){
		this.resetBanStack();
		for(Method t : threads){
			Method tom = this.findTopMethod(t);
			while(tom != null){
				if(tom.isClosed())
					throw new RuntimeException("How can method "+tom + "be closed?");
				int numCallees = tom.callees.size();
				if(numCallees != 0){
					MethodExit cexit = tom.callees.get(numCallees-1).exit;
					this.insertMethExit(tom.getThreadID(), tom, tom.getAddr(), cexit.time, cexit.count+1);
				}else{
					MethodEntry entry = tom.entry;
					this.insertMethExit(tom.getThreadID(), tom, tom.getAddr(), entry.time, entry.count+1);
				}
				tom = tom.caller;
			}
		}
	}
	
	private Map<Method,Long> execTimeMap;
	private Map<Method,Long> startTimeMap;
	
	private void fixTime(){
		execTimeMap = new HashMap<Method,Long>();
		startTimeMap = new HashMap<Method,Long>();
		for(Method t : threads){
			calExecTimeRecursively(t);
			startTimeMap.put(t, t.getStartTime());
			calStartTimeRecursively(t);
		}
		for(Map.Entry<Method, Long> execEntry : execTimeMap.entrySet()){
			Method m = execEntry.getKey();
			long startTime = startTimeMap.get(m).longValue();
			m.setStartTime(startTime);
			m.setEndTime(startTime+execEntry.getValue());
		}	
	}
	
	public void fixConcurrencyWithColoc(boolean ifPinColoc){
		Utils.printLogWithTime("Building hyper edges.");
		//Map from thread pairs to the set of writer-reader pairs connecting them
		Map<Pair<Method,Method>,Set<Pair<Method,Method>>> hyperEdgeMap = new HashMap<Pair<Method,Method>,Set<Pair<Method,Method>>>();
		List<Method> threads = this.threads;
		for(Method t : threads){
			Set<Data> outputSet = this.getOutput(t);
			if(outputSet != null)
			for(Data d : outputSet){
				for(List<Instruction> accessGroup : d.groupAccessors()){
					Iterator<Instruction> iter = accessGroup.iterator();
					Instruction writer = iter.next();
					if(writer.getThreadID() != t.getThreadID())
						continue;
					while(iter.hasNext()){
						Instruction r = iter.next();
						if(r.getThreadID() != writer.getThreadID()){
							Pair<Method,Method> wrTPair = new Pair<Method,Method>(t, this.getThread(r.getThreadID()));
							Pair<Method,Method> wrPair = new Pair<Method,Method>(writer.getMethod(),r.getMethod());
							Set<Pair<Method,Method>> edges = hyperEdgeMap.get(wrTPair);
							if(edges == null){
								edges = new HashSet<Pair<Method,Method>>();
								hyperEdgeMap.put(wrTPair, edges);
							}
							edges.add(wrPair);
						}
					}
				}
			}
		}
		Utils.printLogWithTime("Building reachable threads relation.");	
		Map<Method,Set<Method>> reachableT = new HashMap<Method,Set<Method>>();
		for(Method t : threads){
			Set<Method> reachSet = new HashSet<Method>();
			reachSet.add(t);
			this.buildReachMRecursively(t, reachSet, hyperEdgeMap);
			reachableT.put(t, reachSet);
		}
		Utils.printLogWithTime("Building SCCs of threads");
		Set<Set<Method>> threadSCC= new HashSet<Set<Method>>();
		Set<Method> workList = new HashSet<Method>(threads);
		while(!workList.isEmpty()){
			Method seed = workList.iterator().next();
			Set<Method> scc = new HashSet<Method>();
			scc.add(seed);
			for(Method f : reachableT.get(seed)){
				Set<Method> fReach = reachableT.get(f);
				if(fReach.contains(seed))
					scc.add(f);
			}
			threadSCC.add(scc);
			workList.removeAll(scc);
		}
		Utils.printLogWithTime("Colocating methods");
		for(Set<Method> scc : threadSCC){
			if(scc.size() <= 1)
				continue;
			Set<Method> sccColoc = new HashSet<Method>();
			for(Method f1 : scc){
				Set<Pair<Method,Method>> writerEdgeSet = new HashSet<Pair<Method,Method>>();
				Set<Pair<Method,Method>> readerEdgeSet = new HashSet<Pair<Method,Method>>();
				for(Method f2 : scc){
					if(f2.equals(f1))
						continue;
					Set<Pair<Method,Method>> writerEdges = hyperEdgeMap.get(new Pair<Method,Method>(f1,f2));
					if(writerEdges!=null)
						writerEdgeSet.addAll(writerEdges);
					Set<Pair<Method,Method>> readerEdges = hyperEdgeMap.get(new Pair<Method,Method>(f2,f1));
					if(readerEdges!=null)
						readerEdgeSet.addAll(readerEdges);
				}
				sccColoc.addAll(this.findColocateMethods(readerEdgeSet, writerEdgeSet));
			}
			this.colocSet.add(sccColoc);
			if(ifPinColoc){
				this.pinnedMethods.addAll(sccColoc);
			}
		}
	}
	
	private Method findCommonParent(Method f1, Method f2){
		while(!f2.getAncestors().contains(f1)&&f1.getCaller()!=null)
			f1 = f1.getCaller();
		return f1;	
	}
	
	/**
	 * ps1: t1(w)->t2(r), ps2: t2(w)->t3(r). colocate w and r
	 * @param p1
	 * @return
	 */
	private Set<Method> findColocateMethods(Set<Pair<Method,Method>> ps1, Set<Pair<Method,Method>> ps2){
		Set<Method> ret = new HashSet<Method>();
		for(Pair<Method,Method> p1 : ps1)
			for(Pair<Method,Method> p2 : ps2){
				ret.add(this.findCommonParent(p1.getSecond(), p2.getFirst()));
//				ret.add(p1.getSecond());
//				ret.add(p2.getFirst());
			}
		return ret;
	}
	
	private void buildReachMRecursively(Method cf,Set<Method> reachSet, Map<Pair<Method,Method>,Set<Pair<Method,Method>>> hyperEdgeMap){
		for(Map.Entry<Pair<Method,Method>, Set<Pair<Method,Method>>> entry : hyperEdgeMap.entrySet()){
			Pair<Method,Method> wrTPair = entry.getKey();
			if(wrTPair.getFirst().equals(cf)&&!entry.getValue().isEmpty()){
				if(reachSet.add(wrTPair.getSecond())){
					buildReachMRecursively(wrTPair.getSecond(),reachSet,hyperEdgeMap);
				}
			}
		}
	}
	
	
	private long calExecTimeRecursively(Method m){
		long execTime = 0;
		if(m.isNative()){
			long lastEndTime = m.getStartTime();
			for(Method c : m.callees){
				long callInterval = c.getStartTime()-lastEndTime;
				if(callInterval < 0)
					throw new RuntimeException("Error: check child method sequence");
				execTime += callInterval;
				execTime += calExecTimeRecursively(c);
				lastEndTime = c.getEndTime();
			}
			long callInterval = m.getEndTime() - lastEndTime;
			if(callInterval < 0)
				throw new RuntimeException("Error: how can child call end before parent?");
			execTime+=callInterval;
		}else{
			long lastEndCount = m.getStartCount();
			for(Method c : m.callees){
				long callInterval = (long)((c.getStartCount() - lastEndCount)*Configuration.instrTime);
				if(callInterval < 0)
					throw new RuntimeException("Error: check child method sequence");
				execTime += callInterval;
				execTime += calExecTimeRecursively(c);
				lastEndCount = c.getEndCount();
			}
			long callInterval = (long)((m.getEndCount() - lastEndCount)*Configuration.instrTime);
			if(callInterval < 0)
				throw new RuntimeException("Error: how can child call end before parent?");
			execTime+=callInterval;
		}
		execTimeMap.put(m, execTime);
		return execTime;
	}
	
	/**
	 * Update the starting time recursively, note root is not taken care
	 * @param f
	 * @return
	 */
	private void calStartTimeRecursively(Method m){
		long startTime = startTimeMap.get(m);
		if(m.isNative()){
			long lastTraceTime = m.getStartTime();
			for(Method c : m.callees){
				long cStartTime = startTime+c.getStartTime()-lastTraceTime;
				if(cStartTime < 0)
					throw new RuntimeException("Negative start time.");
				startTimeMap.put(c, cStartTime);
				startTime = cStartTime + execTimeMap.get(c);
				lastTraceTime = c.getEndTime();
			}
		}else{
			long lastTraceCount = m.getStartCount();
			for(Method c : m.callees){
				long cStartTime = (long)(startTime+(c.getStartCount()-lastTraceCount)*Configuration.instrTime);
				if(cStartTime < 0)
					throw new RuntimeException("Negative start time.");
				startTimeMap.put(c, cStartTime);
				startTime = cStartTime + execTimeMap.get(c);
				lastTraceCount = c.getEndCount();
			}
		}
		for(Method c : m.callees)
			calStartTimeRecursively(c);
	}
	
	private void resetBanStack(){
		this.threadBanStack.clear();
	}
	
	private boolean isThreadBanned(int tid){
		Integer bannDepth = this.threadBanStack.get(tid);
		if(bannDepth == null || bannDepth == 0)
			return false;
		if(bannDepth > 0)
			return true;
		throw new RuntimeException("somthing wrong with the method ban code");
	}
	
	private void increaseBannDepth(int tid){
		Integer bannDepth = this.threadBanStack.get(tid);	
		if(bannDepth == null)
			bannDepth = 0;
		bannDepth++;
		this.threadBanStack.put(tid, bannDepth);
	}
	
	private void decreaseBannDepth(int tid){
		Integer bannDepth = this.threadBanStack.get(tid);	
		if(bannDepth == null || bannDepth == 0)
			return;
		if(bannDepth > 0)
			bannDepth--;
		this.threadBanStack.put(tid, bannDepth);	
	}
	
	private void insertMethEntry(int threadID, Method parent, String name, long addr, long count, long startTime, boolean isNative){
		if(Configuration.isMethodBanned(name) || this.isThreadBanned(threadID)){
			this.increaseBannDepth(threadID);
		}
		if(this.isThreadBanned(threadID)){
			Utils.printLogWithTime("Ignore method "+name+ " in thread "+threadID+" starting at "+startTime);
			return;
		}
		List<DataKey> datas = null;
		if(parent != null){
			datas = this.insertParaWrites(parent, name);
		}
		MethodEntry entry = new MethodEntry(curLineNum, threadID, parent, name, addr, count, startTime, isNative);
		curLineNum++;
		instDomain.add(entry);
		this.insts.add(entry);
		Method meth = new Method(parent, entry,this);
		entry.curMeth = meth;
		if(parent != null){
			parent.addCallee(meth);
			parent.addInstruction(entry);
			this.insertParaReads(meth, datas);
		}else{//The top level thread method
			if(!this.addThread(meth))
				throw new RuntimeException("Trying to add an existing thread!");
		}
	}

	private void checkPinAndColoc(Method meth){
		
	}
	
	private void insertMethExit(int threadID, Method curMeth,long addr, long time, long count){
		if(this.isThreadBanned(threadID)){
			this.decreaseBannDepth(threadID);
			return;
		}
//		if(curMeth.isNative()){
//			Set<String> colSet = Configuration.getColNameSet(meth.methName());
//			Map<DataKey,Integer> arraySet = jniArrayMap.get(colSet);
//			if(arraySet != null)
//				for(Map.Entry<DataKey,Integer> arrayInfo : arraySet.entrySet()){
//					if(arrayInfo.getValue() > 0){
//						DataKey dataAddr = arrayInfo.getKey();
//						Data d = addrToData.get(dataAddr);
//						this.insertRead(threadID, meth, Read.OBJ_ARRAY_ALL, addr, 0, d.size, d.size);
//						this.insertWrite(threadID, meth, Read.OBJ_ARRAY_ALL, addr, 0, d.size, d.size);
//					}
//				}
//		}
		curMeth.checkHeldArrays();
		Method parent = curMeth.caller;
		long retAddr = 0;
		if(parent != null){
			retAddr = this.insertRetWrite(curMeth);
		}
		MethodExit exit = new MethodExit(curLineNum,threadID,parent,addr,time,count);
		curMeth.setExit(exit);
		exit.curMeth = curMeth;
		curLineNum++;
		this.instDomain.add(exit);
		this.insts.add(exit);
		if(parent!=null){
			parent.addInstruction(exit);
			this.insertRetRead(parent, curMeth.methName(), retAddr);
		}
	}
	
	private void insertAlloc(int threadID, Method method, long addr, int objSize){
		if(this.isThreadBanned(threadID))
			return;
		Alloc a = new Alloc(curLineNum,threadID, method, addr, objSize);
		curLineNum++;
		method.addInstruction(a);
		this.instDomain.add(a);
		this.insts.add(a);
	}
	
	private void insertWrite(int threadID, Method method, int type, long addr, int offset, int objSize, int fldSize){
		if(this.isThreadBanned(threadID))
			return;
		if(type == Read.OBJ_RET){
			method.addRet(addr);
//			curLineNum++;
			return;
		}
		if(type == Read.OBJ_PARAM){
//			curLineNum++;
			return;
		}
		Write w = new Write(curLineNum, threadID, method, type, addr, offset, objSize, fldSize);
		curLineNum++;
		method.addInstruction(w);
		this.instDomain.add(w);
		this.insts.add(w);
	}
	
	private void insertRead(int threadID, Method method, int type, long addr, int offset, int objSize, int fldSize){
		if(this.isThreadBanned(threadID))
			return;
		if(type == Read.OBJ_RET){
//			curLineNum++;
			return;
		}
		if(type == Read.OBJ_PARAM){
			method.addParam(addr);
//			curLineNum++;
			return;
		}
		
		/*
		 *Through Get**ArrayElements in JNI, the native code can get a C pointer of a certain array.
		 *Depending the implementation of the jvm, the raw C pointer of the java array or a copy might
		 *be returned.
		 *It releases the array by Release**ArrayElements.
		 */
		if(type == Read.OBJ_JNI_HOLD_ARRAY){//view hold as a read
			this.insertRead(threadID, method, Read.OBJ_ARRAY_ALL, addr, 0, objSize, objSize);	
			DataKey dk = this.getDataKey(addr);
			method.increaseHoldCount(dk);
			return;
		}else if(type == Read.OBJ_JNI_REL_ARRAY){//view release as a write
			this.insertWrite(threadID, method, Read.OBJ_ARRAY_ALL, addr, 0, objSize, objSize);
			DataKey dk = this.getDataKey(addr);
			method.decreaseHoldCount(dk);
			return;
		}
		Read r = new Read(curLineNum, threadID, method, type, addr, offset, objSize, fldSize);
		curLineNum++;
		method.addInstruction(r);
		this.instDomain.add(r);
		this.insts.add(r);
	}
	
	private List<DataKey> insertParaWrites(Method caller, String funName){
		int start = funName.indexOf("(")+1;
		int end = funName.length() - 1;
		String sig = funName.substring(start,end);
		List<DataKey> datas = new ArrayList<DataKey>();
		for(int i = 1; i < sig.length(); i++){
			char parType = sig.charAt(i);
			Integer size = Configuration.getDataSize(parType);
			if(size == null || size == 0){
				throw new RuntimeException("Ill formated method name: "+funName);
			}
			long addr = Read.genFakeAddr();
			this.insertWrite(caller.getThreadID(), caller, Read.OBJ_DUMP, addr, 0, size, size);
			datas.add(this.getDataKey(addr));
		}
		return datas;
	}
	
	private void insertParaReads(Method callee, List<DataKey> datas){
		String funName = callee.methName();
		int start = funName.indexOf("(")+1;
		int end = funName.length() - 1;
		String sig = funName.substring(start,end);
		for(int i = 1; i < sig.length(); i++){
			char parType = sig.charAt(i);
			Integer size = Configuration.getDataSize(parType);
			if(size == null || size == 0){
				throw new RuntimeException("Ill formated method name: "+funName);
			}
			DataKey addr = datas.get(i-1);
			this.insertRead(callee.getThreadID(), callee, Read.OBJ_DUMP, addr.address, 0, size, size);
		}
	}
	
	private long insertRetWrite(Method callee){
		String name = callee.methName();
		int start = name.indexOf("(")+1;
		int end = name.length() - 1;
		String sig = name.substring(start,end);	
		char ret = sig.charAt(0);
		int size = Configuration.getDataSize(ret);
		if(size != 0){
			long addr = Read.genFakeAddr();
			int offset = 0;
			this.insertWrite(callee.getThreadID(), callee, Read.OBJ_DUMP, addr, offset, size, size);
			return addr;
		}
		return 0L;
	}
	
	private void insertRetRead(Method caller, String funName, long addr){
		int start = funName.indexOf("(")+1;
		int end = funName.length() - 1;
		String sig = funName.substring(start,end);	
		char ret = sig.charAt(0);
		int size = Configuration.getDataSize(ret);
		if(size != 0){
			int offset = 0;
			this.insertRead(caller.getThreadID(), caller, Read.OBJ_DUMP, addr, offset, size, size);
		}
	}
	
	private void parseNativeBin(DataInputStream r) throws IOException{
		String fName = Utils.readStringUntilSpace(r);
		String libName = Utils.readStringUntilSpace(r);
		System.out.println("Function name: "+fName+", Library name: "+libName);
	}
	
	public void calculateInOutSet(){
		inputMap = new HashMap<Method,Set<Data>>();
		outputMap = new HashMap<Method, Set<Data>>();
		for(Map.Entry<DataKey,Data> dataEntry : addrToData.entrySet()){
			Data d = dataEntry.getValue();
			if(d instanceof ObjData)
				this.processAccessorsObj((ObjData)d);
			else
				this.processAccessorsGen(d);
		}
	}
	
	/**
	 * Process object-type data
	 * @param d
	 */
	private void processAccessorsObj(ObjData d){
		if(Configuration.dataLevel == Configuration.FLD_LEVEL){
			for(Map.Entry<Integer, FldData> entry : d.getFields().entrySet()){
				this.processAccessorsGen(entry.getValue());
			}
		}else{
			this.processAccessorsGen(d);
		}
	}
	
	/**
	 * Process non-obj-type data
	 * @param d
	 */
	private void processAccessorsGen(Data d){
		for(List<Instruction> ssaList : d.groupAccessors()){
			Method directWriter = ssaList.get(0).getMethod();
			List<Method> directReaders = new ArrayList<Method>();
			for(int i = 1; i < ssaList.size(); i++)
				directReaders.add(ssaList.get(i).getMethod());
			List<Method> potWriters = directWriter.getAncestorAndSelf();
			for(Method wm : potWriters){
				if(!wm.getReachableMethods().containsAll(directReaders)){//if all reads are children of current write, then this write is local
					Set<Data> outSet = outputMap.get(wm);
					if(outSet == null){
						outSet = new HashSet<Data>();
						outputMap.put(wm, outSet);
					}
					outSet.add(d);
				}else
					break;
			}
			for(int k = 1; k < ssaList.size(); k++){
				Instruction r = ssaList.get(k);
				for(Method rm : r.getEncloseMethods()){
					if(!potWriters.contains(rm)){//if current read is one ancester of the write, then it is local 
						Set<Data> inSet = inputMap.get(rm);
						if(inSet == null){
							inSet = new HashSet<Data>();
							inputMap.put(rm, inSet);
						}
						inSet.add(d);
					}
					else
						break;
				}
			}
		}
	}

	public int getOutputSize(Method m){
		if(inputMap == null || outputMap == null)
			this.calculateInOutSet();
		Set<Data> outputSet = outputMap.get(m);
		int ret = 0;
		if(outputSet != null)
			for(Data d : outputSet)
				ret+=d.getSize();
		return ret;
	}
	
	public int getInputSize(Method m){
		if(inputMap == null || outputMap == null)
			this.calculateInOutSet();
		Set<Data> inputSet = inputMap.get(m);
		int ret = 0;
		if(inputSet!=null)
			for(Data d : inputSet)
				ret+=d.getSize();
		return ret;
	}
	
	public Set<Data> getOutput(Method m){
		if(inputMap == null || outputMap == null)
			this.calculateInOutSet();
		Set<Data> outputSet = outputMap.get(m);	
		if(outputSet == null){
			outputSet = new HashSet<Data>();
			outputMap.put(m, outputSet);
		}
		return Collections.unmodifiableSet(outputSet);
	}

	public Set<Data> getInput(Method m){
		if(inputMap == null || outputMap == null)
			this.calculateInOutSet();	
		Set<Data> inputSet = inputMap.get(m);
		if(inputSet == null){
			inputSet = new HashSet<Data>();
			inputMap.put(m, inputSet);
		}
		return Collections.unmodifiableSet(inputSet);
	}
	
	public void updatePinAndColocMethods(){
		colocSet.clear();
		for(Method t : this.getThreads())
			this.updatePinnedAndColocRecursively(t, new ArraySet<ColocKey>());
		colocSet.addAll(configedColocMap.values());
	}
	
	public void cleanSCCs(){
		this.colocSet.clear();
	}
	
	public void mergeSCCs(){
		boolean hasChanged = false;
		do{
			hasChanged = false;
			Set<Set<Method>> nColocSet = new HashSet<Set<Method>>();
			while(!colocSet.isEmpty()){
				Set<Set<Method>> csToRm = new HashSet<Set<Method>>();
				Iterator<Set<Method>> iter = colocSet.iterator();
				Set<Method> first = iter.next();
				csToRm.add(first);
				Set<Method> seed = new HashSet<Method>(first);
				while(iter.hasNext()){
					Set<Method> current = iter.next();
					for(Method m : current){
						if(seed.contains(m)){
							seed.addAll(current);
							csToRm.add(current);
							hasChanged = true;
							break;
						}
					}
				}
				colocSet.removeAll(csToRm);
				nColocSet.add(seed);
			}
			this.colocSet = nColocSet;
		}while(hasChanged);
	}
	
	private void updatePinnedAndColocRecursively(Method m, Set<ColocKey> cks){
		if(Configuration.isMethodPinned(m))
			this.pinnedMethods.add(m);
		Set<ColocKey> cks1 = Configuration.getColocKey(m);
		Set<ColocKey> cks2 = new HashSet<ColocKey>(cks);
		for(ColocKey ck : cks){
			if(m.isNative()){
				if(Configuration.getMethodPinTag(m.methName()) != Method.STATELESS)
					if(!Configuration.isMethodPinned(m) || Configuration.getMethodPinTag(m.methName())==Method.SPEC_PINNED){
						this.addColocMethod(ck, m);
					}else{
						Utils.printLogWithTime("WARNING: We chose not to co-locate a pinned methods: "+m);
					}
			}
		}
		for(ColocKey ck : cks1){
			if(ck.ifColocSub)
				cks2.add(ck);
			else
				this.addColocMethod(ck, m);
		}
		for(Method c : m.getCallees()){
			updatePinnedAndColocRecursively(c,cks2);
		}
	}
	
	private void addColocMethod(ColocKey ck, Method m){
		ColocKey nck = ck.normalize();
		Set<Method> ckms = configedColocMap.get(nck);
		if(ckms == null){
			ckms = new HashSet<Method>();
			configedColocMap.put(nck, ckms);
		}
		ckms.add(m);	
	}
	
	public void inlineOptimize(boolean ifStateful, boolean ifNested){
		noninlinableSet = new HashSet<Method>();
		for(Method t : this.threads)
			this.inlineRecursively(t, ifNested);
	}
	
	public boolean isMethodPinned(Method m){
		return pinnedMethods.contains(m);
	}
	
	public boolean isMethodColocated(Method m){
		for(Set<Method> colScc : colocSet){
			if(colScc.contains(m))
				return true;
		}
		return false;
	}
	
	public boolean inlinable(Method f, boolean ifNested){
		if(noninlinableSet.contains(f))
			return false;
		if(this.isMethodPinned(f)){
			if(ifNested){
				noninlinableSet.add(f);
				noninlinableSet.addAll(f.getAncestors());
			}
			return false;
		}
		if(this.isMethodColocated(f)){//Will only reach here in a stateful setting
			noninlinableSet.add(f);
			noninlinableSet.addAll(f.getAncestors());
			return false;
		}
		if(Configuration.getLocalInclusiveTime(f) + Configuration.applyModel(this.getInputSize(f), -1)+Configuration.applyModel(this.getOutputSize(f), -1) 
				<= 2*Configuration.latency+Configuration.getRemoteInclusiveTime(f))
			return true;
		return false;
	}
	
	private void inlineRecursively(Method f, boolean ifNested){
		Set<Method> childrenToInline = new HashSet<Method>();
		for(Method c : f.getCallees()){
			inlineRecursively(c,ifNested);
				if(inlinable(c,ifNested)){
					childrenToInline.add(c);
				}
		}
		if(childrenToInline.size() > 0){
			for(Method c : childrenToInline){
				f.inline(c);
			}
		}
	}
	
	public String getTraceName() {
		return traceName;
	}

	public Set<Set<Method>> getColocSet() {
		return colocSet;
	}

	public Set<Method> getPinnedMethods() {
		return pinnedMethods;
	}
	
	public Method findCommonAncestor(Set<Method> mSet){
		if(mSet.size() == 0)
			return null;
		Iterator<Method> mIter = mSet.iterator();
		Method commonAnces = mIter.next();
		while(mIter.hasNext()&&commonAnces!=null){
			Method cur = mIter.next();
			while(commonAnces!=null&&!commonAnces.getReachableMethods().contains(cur))
				commonAnces = commonAnces.getCaller();
		}
		return commonAnces;
	}
	
	public DataKey getDataKey(long addr){
		Integer index = this.addrGCMap.get(addr);
		if(index == null)
			index = 0;
		if(index == 0)
			return null;
		return new DataKey(addr, index);
	}
	
	public DataKey getOrGenDataKey(long addr){
		Integer index = this.addrGCMap.get(addr);
		if(index == null)
			index = 0;
		if(index == 0){
			index++;
		}
		this.addrGCMap.put(addr, index);
		return new DataKey(addr, index);	
	}
	
	public int newAddr(long addr){
		Integer index = this.addrGCMap.get(addr);
		if(index == null)
			index = 0;
		index++;
		this.addrGCMap.put(addr, index);
		return index;
	}
}
