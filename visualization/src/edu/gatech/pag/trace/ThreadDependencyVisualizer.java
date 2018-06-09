package edu.gatech.pag.trace;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.gatech.traceprocessor.parser.Configuration;
import edu.gatech.traceprocessor.parser.Data;
import edu.gatech.traceprocessor.parser.Instruction;
import edu.gatech.traceprocessor.parser.Method;
import edu.gatech.traceprocessor.parser.MethodEntry;
import edu.gatech.traceprocessor.parser.Program;
import edu.gatech.traceprocessor.parser.Read;
import edu.gatech.traceprocessor.parser.Write;
import edu.gatech.traceprocessor.utils.Pair;

public class ThreadDependencyVisualizer {
	static String inputFile;
	static String plainOut;
	static String trunkedPlainOut;
	static String localMethodsPath;
	static String coLocMethodsPath;
	static String networkModelPath;
	static String dotOut;
	static String outDir;
	
	public static void main(String[] args) throws IOException {
		inputFile = System.getProperty("mcc.path.input", null);
		outDir = System.getProperty("mcc.path.outputDir", null);
		plainOut = System.getProperty("mcc.path.plainOut",null);
		dotOut = System.getProperty("mcc.path.dotOut",null);
		trunkedPlainOut = System.getProperty("mcc.path.trunkedPlain",null);
		localMethodsPath = System.getProperty("mcc.path.localmethods", null);
		coLocMethodsPath = System.getProperty("mcc.path.coLocMethods", null);
		networkModelPath = System.getProperty("mcc.path.networkModel", null);
		
		Configuration.loadNetworkModel(networkModelPath);
		Configuration.loadCoLocMethods(coLocMethodsPath);
		Configuration.loadLocalMethods(localMethodsPath);
		Program p = new Program();
		p.load(inputFile);
		if(plainOut!=null)
			p.writePlain(plainOut);
		p.removeThreadLocalInsts();
		if(trunkedPlainOut!=null)
			p.writePlain(trunkedPlainOut);
		HtmlVisualizer.visualize(p, outDir,true);
		drawDepGraph(p, dotOut);
		drawThreadDepGraph(p,dotOut);
	}
	
	
	private static void drawThreadDepGraph(Program p, String dotOut) throws FileNotFoundException{
		PrintWriter pw = new PrintWriter(new File(dotOut));
		Set<Pair<Integer,Integer>> wr = new HashSet<Pair<Integer,Integer>>();
		pw.println("digraph G{");
		for(Data d : p.getData().values()){
			if(d.isThreadShared()){
				Write lw = null;
				for(Instruction i : d.getAccessors()){
					if(i instanceof Write){
						lw = (Write)i;
					}else
						if(i instanceof Read)
							if(lw != null){
								if(i.getThreadID() != lw.getThreadID()){
									wr.add(new Pair<Integer,Integer>(lw.getThreadID(), i.getThreadID()));
								}
							}
				}
			}
		}
		for(Pair<Integer,Integer> pair : wr)
			pw.println(pair.getFirst()+"->"+pair.getSecond());
		pw.println("}");
		pw.flush();
		pw.close();
	}
	
	static Instruction last;
	
	private static void drawDepGraph(Program p, String dotOut) throws FileNotFoundException{
		PrintWriter pw = new PrintWriter(new File(dotOut));
		pw.println("digraph G{");
		for(Method m : p.getThreads()){
			last = null;
			drawMethodRecur(m,pw);
		}
		drawData(p,pw);
		pw.println("}");
		pw.flush();
		pw.close();
	}

	private static void drawMethodRecur(Method m,PrintWriter pw){
		pw.println("subgraph cluster"+m.getMethodID()+"{");
		for(Instruction i : m.getInstructions())
			if(i instanceof Write){
				Write w = (Write)i;
				pw.println(w.getLineNum()+"[label=\"w, addr="+w.data.getAddr()+"\"];");
				if(last != null){
					pw.println(last.getLineNum()+"->"+w.getLineNum());
				}
				last = w;
			}
			else if(i instanceof Read){
				Read r = (Read)i;
				pw.println(r.getLineNum()+"[label=\"r, addr="+r.data.getAddr()+"\"];");
				if(last != null){
					pw.println(last.getLineNum()+"->"+r.getLineNum());
				}
				last = r;
			}
			else if(i instanceof MethodEntry){
				MethodEntry entry = (MethodEntry)i;
				drawMethodRecur(entry.curMeth, pw);
			}
		pw.println("label = \""+m.toString()+"\";");
		pw.println("}");
	}

	private static void drawData(Program p, PrintWriter pw){
		for(Data d : p.getData().values())
			if(d.isThreadShared()){
				List<Instruction> accessors = d.getAccessors();
				Write lastWrite = null;
				for(Instruction inst : accessors){
					if(lastWrite !=null && lastWrite.getThreadID() != inst.getThreadID()){
						pw.println(lastWrite.getLineNum()+"->"+inst.getLineNum());
					}
					if(inst instanceof Write)
						lastWrite = (Write)inst;
				}
			}
	}
}

