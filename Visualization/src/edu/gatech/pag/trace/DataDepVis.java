package edu.gatech.pag.trace;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.gatech.traceprocessor.parser.Configuration;
import edu.gatech.traceprocessor.parser.Data;
import edu.gatech.traceprocessor.parser.DataKey;
import edu.gatech.traceprocessor.parser.Instruction;
import edu.gatech.traceprocessor.parser.Method;
import edu.gatech.traceprocessor.parser.Program;
import edu.gatech.traceprocessor.parser.Read;
import edu.gatech.traceprocessor.parser.Write;

public class DataDepVis {
	static String inputFile;
	static String localMethodsPath;
	static String coLocMethodsPath;
	static String networkModelPath;
	static String outputFile;
	
	public static void main(String[] args) throws FileNotFoundException {
		inputFile = System.getProperty("mcc.path.input", null);
		outputFile = System.getProperty("mcc.path.output",null);
		localMethodsPath = System.getProperty("mcc.path.localmethods", null);
		coLocMethodsPath = System.getProperty("mcc.path.coLocMethods", null);
		networkModelPath = System.getProperty("mcc.path.networkModel", null);
		Configuration.loadNetworkModel(networkModelPath);
		Configuration.loadCoLocMethods(coLocMethodsPath);
		Configuration.loadLocalMethods(localMethodsPath);
		Program p = new Program();
		p.loadBinary(inputFile);
		Map<DataKey,Data> dataMap = p.getData();
		SortedSet<Data> orderedData = new TreeSet<Data>(new DataCMP());
		orderedData.addAll(dataMap.values());
		PrintWriter pw = new PrintWriter(new File(outputFile));
		for(Data d : orderedData)
			printDataAccessors(d,pw);
		pw.flush();
		pw.close();
	}
	
	private static void printDataAccessors(Data d, PrintWriter pw){
		Map<Integer,List<Instruction>> offsetMap = new HashMap<Integer,List<Instruction>>();
		for(Write w : d.getWriters()){
			int offset = w.offset;
			List<Instruction> insts = offsetMap.get(offset);
			if(insts == null){
				insts = new ArrayList<Instruction>();
				offsetMap.put(offset, insts);
			}
			insts.add(w);
		}
		for(Read r : d.getReaders()){
			int offset = r.offset;
			List<Instruction> insts = offsetMap.get(offset);
			if(insts == null){
				insts = new ArrayList<Instruction>();
				offsetMap.put(offset, insts);
			}
			insts.add(r);
		}
		List<List<Instruction>> fieldAccess = new ArrayList<List<Instruction>>(offsetMap.values());
		Collections.sort(fieldAccess, new ListRevCMP());
		pw.println("Data address: "+d.getAddr()+", data size: "+d.getSize());
		for(List<Instruction> fieldEntry : fieldAccess){
			for(Instruction i : fieldEntry){
				pw.println(i.getLineNum()+": "+i.toPlainFormat());
				pw.println(i.getMethod().toString());
			}
			pw.println();
		}
		pw.println("##########################################################");
		pw.println("##########################################################");
	}

}

class ListRevCMP implements Comparator<List<? extends Object>>{

	@Override
	public int compare(List<? extends Object> o1, List<? extends Object> o2) {
		return o2.size() - o1.size();
	}
	
}

class DataCMP implements Comparator<Data>{
	static Map<Data,Integer> dataSizeMap = new HashMap<Data,Integer>(); 
	
	@Override
	public int compare(Data o1, Data o2) {
		Integer s1 = dataSizeMap.get(o1);
		Integer s2 = dataSizeMap.get(o2);
		if(s1 == null){
			s1 = this.countNumOfAccessedMethods(o1);
			dataSizeMap.put(o1, s1);
		}
		if(s2 == null){
			s2 = this.countNumOfAccessedMethods(o2);
			dataSizeMap.put(o2, s2);
		}
		if(s1 != s2)
			return s2 - s1;
		if(o1.getAddr() == o2.getAddr())
			return 0;
		return (o2.getAddr() - o1.getAddr()>0)?1:-1;
	}
	
	private int countNumOfAccessedMethods(Data d){
		Set<Method> accessedMethods = new HashSet<Method>();
		for(Read r : d.getReaders())
			accessedMethods.add(r.getMethod());
		for(Write w : d.getWriters())
			accessedMethods.add(w.getMethod());
		return accessedMethods.size();
	}
	
}