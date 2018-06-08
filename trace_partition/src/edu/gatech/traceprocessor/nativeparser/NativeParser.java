package edu.gatech.traceprocessor.nativeparser;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.gatech.traceprocessor.parser.Program;
import edu.gatech.traceprocessor.utils.NativeInstBitSet;
import edu.gatech.traceprocessor.utils.Utils;

public class NativeParser {
	ArrayList<NativeInstruction> instDomain;
	NativeInstBitSet insts;
	List<NativeMethod> threads;
	int lineNum;
	Set<Integer> jniMethIdSet;
	Map<NativeMethod,Set<Integer>> syscallMap;
	int fakeAddrSeed = 0;
	Map<Long, Integer> addrMap;
	Map<Integer, List<NativeInstruction>> accessors;

	public NativeParser() {
		instDomain = new ArrayList<NativeInstruction>();
		instDomain.add(null);// we don't want index 0
		insts = new NativeInstBitSet(instDomain, 100000);
		threads = new ArrayList<NativeMethod>();
		jniMethIdSet = new HashSet<Integer>();
		syscallMap = new HashMap<NativeMethod, Set<Integer>>();
		accessors = new HashMap<Integer, List<NativeInstruction>>();
		addrMap = new HashMap<Long, Integer>();
	}
	
	public void addJniMethId(int id){
		this.jniMethIdSet.add(id);
	}
	
	public boolean isJniMeth(int id){
		return jniMethIdSet.contains(id);
	}

	public void parse(String path) throws IOException{
		DataInputStream r = new DataInputStream(new BufferedInputStream(new FileInputStream(path)));
		lineNum = 0;
		try {
			while(true){
				long type = Utils.intToUnsigned(r.readInt());
				long osId = Utils.intToUnsigned(r.readInt());
				long data1 = Utils.intToUnsigned(r.readInt());
				long data2 = Utils.intToUnsigned(r.readInt());
				long data3 = r.readLong();
				int tid = (int)osId;
				NativeMethod topMeth = this.getOrCreateThreadTopMethod(tid);
				NativeInstruction ins = null;
				switch((int)type){
				case NativeInstruction.ENTRANCE:
					lineNum++;
					ins = new NativeInstruction(lineNum,(int)type,tid,data1,data2,data3, topMeth);
					topMeth.addInstruction(ins);
					ins.meth = topMeth;
					int mid = (int)data1;
					NativeMethod callee = new NativeMethod(isJniMeth(mid), topMeth, tid, (int)data2, data3, mid);
					callee.entrance = ins;
					topMeth.addCalless(callee);
					this.addInstToDomain(ins);
					break;
				case NativeInstruction.EXIT:
					topMeth.close(data3);
					mid = (int)data1;
					if(topMeth.methId != mid)
						throw new RuntimeException("Call and return do not match!");
					if(topMeth.parent != null){
						lineNum++;
						ins = new NativeInstruction(lineNum,(int)type,tid,data1,data2,data3, topMeth.parent);
						topMeth.parent.addInstruction(ins);
						topMeth.exit = ins;
						ins.meth = topMeth.parent;
						this.addInstToDomain(ins);
					}
					break;
				case NativeInstruction.READ:
					for(int i = 0; i < data2; i++){
						long addr = data1+i;
						Integer faddr = addrMap.get(addr);//get the fake address of the data
						if(faddr == null){
							faddr = this.genFakeAddr();
							addrMap.put(addr, faddr);
						}
						lineNum++;
						ins = new NativeInstruction(lineNum, (int)type,tid,faddr,1,data3, topMeth);
						this.addInstToDomain(ins);
						this.accessors.get(faddr).add(ins);
						topMeth.addInstruction(ins);
					}
					break;
				case NativeInstruction.WRITE:
					for(int i = 0; i < data2; i++){
						long addr = data1+i;
						Integer faddr = this.genFakeAddr();
						addrMap.put(addr, faddr);
						lineNum++;
						ins = new NativeInstruction(lineNum, (int)type,tid,faddr,1,data3, topMeth);
						this.addInstToDomain(ins);
						this.accessors.get(faddr).add(ins);
						topMeth.addInstruction(ins);
					}
					break;
				case NativeInstruction.SYS_CALL:
					Set<Integer> syscallSet = syscallMap.get(topMeth);
					if(syscallSet == null){
						syscallSet = new HashSet<Integer>();
						syscallMap.put(topMeth, syscallSet);
					}
					syscallSet.add((int)data1);
					break;
				}
			}
		} catch (EOFException e) {
			
		}
		r.close();
		for(NativeMethod t : threads)
			this.closeMethodRecursively(t);
	}
	
	public NativeMethod getOrCreateThreadTopMethod(int tid){
		for(NativeMethod root : this.threads){
			if(root.tid == tid){
				return getTopMethod(root);
			}
		}
		NativeMethod root = new NativeMethod(true, null, tid, 0, 0, 0);
		lineNum++;
		NativeInstruction ins = new NativeInstruction(lineNum, NativeInstruction.ENTRANCE, tid, 0, 0, 0, null);
		this.addInstToDomain(ins);
		root.entrance = ins;
		this.threads.add(root);
		return root;
	}
	
	private int genFakeAddr(){
		fakeAddrSeed++;
		List<NativeInstruction> aInses = new ArrayList<NativeInstruction>();
		accessors.put(fakeAddrSeed, aInses);
		return fakeAddrSeed;
	}

	private NativeMethod getTopMethod(NativeMethod m){
		if(m.callees.size() > 0){
			NativeMethod lm = m.callees.get(m.callees.size() - 1);
			if(lm.isClosed())
				return m;
			else
				return getTopMethod(lm);
		}
		return m;
	}
	
	private void addInstToDomain(NativeInstruction ins){
		this.instDomain.add(ins);
		if(this.instDomain.size() != lineNum + 1)
			throw new RuntimeException("Error in construct the domain of native instructions!");
		this.insts.add(ins);
	}
	
	private void closeMethodRecursively(NativeMethod top){
		if(top.isClosed())
			return;
		for(NativeMethod c : top.callees)
			closeMethodRecursively(c);
		if(top.callees.size() == 0)
			top.close(top.sIC);
		else{
			NativeMethod lastChild = top.callees.get(top.callees.size()-1);
			top.close(lastChild.eIC);
			lineNum++;
			NativeInstruction exit = new NativeInstruction(lineNum, NativeInstruction.EXIT, top.tid, top.methId, top.javaIC,top.eIC,null);
			this.addInstToDomain(exit);
			if(top.parent!=null){
				exit.meth = top.parent;
				top.parent.addInstruction(exit);
			}
		}
	}
	
	public void removeUnmatchedMethods(Program jp){
		for(NativeMethod t : this.threads){
			this.removeUnmatchedMethodsRecursively(t, jp);
		}
	}
	
	private void removeUnmatchedMethodsRecursively(NativeMethod m, Program jp){
		for(NativeMethod c : m.callees){
			this.removeUnmatchedMethodsRecursively(c, jp);
			if(jp.findMethByTidAndIC(c.tid, c.javaIC) == null){//fail to find the native method in java trace, remove it
				if(c.callees.size() != 0)
					throw new RuntimeException("Something wrong with the Java and native trace matching!");
				m.callees.remove(c);
				m.body.remove(c.entrance);
				m.body.remove(c.exit);
				insts.remove(c.entrance);
				insts.remove(c.exit);
				for(NativeInstruction ins: c.body)
					insts.remove(ins);
			}
		}
	}
	
	/**
	 * Remove memory accesses local to a method
	 */
	private void removeMethodLocalAccesses(){
		
	}
}