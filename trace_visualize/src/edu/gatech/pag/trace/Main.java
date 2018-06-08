package edu.gatech.pag.trace;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;

import old.edu.gatech.traceprocessor.Configuration;
import old.edu.gatech.traceprocessor.Function;
import old.edu.gatech.traceprocessor.TraceProcessor;
import old.edu.gatech.traceprocessor.VarEntry;
import old.edu.gatech.traceprocessor.offloadingalgorithms.solver.SFunctionFactory;

import org.apache.commons.io.FileUtils;

import edu.gatech.traceprocessor.utils.Pair;

public class Main {
	public final static String indexFile = "index.html";
	private static String outputFolder;
	private static String inputFile;
	private static String dotOut;
	private static String plainOut;
	private static String inlineOut;
	private static TraceProcessor tp;
	private static List<String> heads;
	private static boolean ifNested;
	private static boolean ifStateful;
	private static boolean ifTracePlain;
	private static boolean ifInline;
    private static HashMap<Character, String> replacements = new HashMap<Character, String>();
    static{
    replacements.put('&', "&amp;");
    replacements.put('>', "&gt;");
    replacements.put('<', "&lt;");
    replacements.put('\'', "&apos;");
    replacements.put('\"', "&quot;");
    replacements.put(' ', "&nbsp;");
    }
	
	public static void main(String[] args) throws IOException {
		inputFile = System.getProperty("mcc.path.input", null);
		outputFolder = System.getProperty("mcc.path.outputDir",null);
		dotOut = System.getProperty("mcc.path.dotOut",null);
		plainOut = System.getProperty("mcc.path.plainOut",null);
		inlineOut = System.getProperty("mcc.path.inlineOut",null);
		ifTracePlain = Boolean.getBoolean("mcc.input.plain");
		ifNested = Boolean.getBoolean("mcc.scheme.nested");
		ifStateful = Boolean.getBoolean("mcc.scheme.stateful");
		String inlineStr = System.getProperty("mcc.inline","false");
		ifInline = Boolean.parseBoolean(inlineStr);
		
		String localMethodsPath = System.getProperty("mcc.path.localmethods", null);
		String coLocMethodsPath = System.getProperty("mcc.path.coLocMethods", null);
		String networkModelPath = System.getProperty("mcc.path.networkModel", null);
		
		if(ifInline && (networkModelPath == null || coLocMethodsPath == null || localMethodsPath == null))
			throw new RuntimeException("To turn on inlining, set the correct network model, native method list and colocation method list!");
		
		Configuration.inlineOpt = ifInline;

		tp = new TraceProcessor(inputFile, localMethodsPath, coLocMethodsPath, ifNested,ifStateful,new SFunctionFactory());
		if(networkModelPath != null)
			Configuration.setNetworkModel(networkModelPath);
		tp.setIfBin(!ifTracePlain);
		if(plainOut!=null){
			PrintWriter pw = new PrintWriter(plainOut);
			tp.setPlainOut(pw);
		}
		tp.parse();
		if(dotOut!=null)
			drawDot(tp.getForest(),tp.getVarTable());
	//	String libPackages = "";//System.getProperty("libs", "java,javax,sun,libcore,dalvik,android,libcore,org.apache,com.android");
	//	cutForest(libPackages,tp.getForest());
		Vector<Function> threads = tp.getForest().getChildren();
		initEnv();
		createIndexPageAndMethodPages(threads);
		createVarPages(tp.getVarTable());
		System.out.println("Total time: "+getTotalTime(tp.getForest()));
		System.out.println("Java time: "+getJavaTime(tp.getForest()));
		System.out.println("Native time: "+getNativeTime(tp.getForest()));
		if(plainOut!=null&&inlineOut!=null){
			Set<Long> lineSet = new HashSet<Long>();
			for(Function f : tp.getInlinedFunctions()){
				lineSet.add(f.getEntryLineNum());
				lineSet.add(f.getExitLineNum());
			}
			long lineNumber = 0;
			Scanner sc = new Scanner(new File(plainOut));
			PrintWriter pw = new PrintWriter(new File(inlineOut));
			while(sc.hasNext()){
				lineNumber ++;
				if(!lineSet.contains(lineNumber))
					pw.println(sc.nextLine());
				else
					sc.nextLine();
			}
			pw.flush();
			pw.close();
		}
		tp.getForest().printStatistics(System.out);
	}
	
	private static void initEnv() throws IOException{
		InputStream is = Main.class.getResourceAsStream("/templateHead");
		Scanner sc = new Scanner(is);
		heads = new ArrayList<String>();
		while(sc.hasNext())
			heads.add(sc.nextLine());
		File source = new File("resources/js");
		File desc = new File(outputFolder);
		try{
		FileUtils.copyDirectory(source, desc);
		}catch(Exception e){
			System.out.println("Oops! Copying resources failed.");
		}
	}
	
	
	private static void createIndexPageAndMethodPages(Vector<Function> threads) throws FileNotFoundException{
		PrintWriter pw = new PrintWriter(outputFolder + File.separator + indexFile);
		printHeader(inputFile, pw, false);
		for(int i = 0; i < threads.size(); i++){
			Function root = threads.get(i);
			int threadID = root.getThreadID();
			pw.println("<a href=\"" + "thread-"+ threadID + ".html\">"+"thread-"+threadID+"</a><br/>");
			PrintWriter htmlOut = new PrintWriter(outputFolder+File.separator + "thread-"+threadID+".html");
			printHeader("thread-"+threadID,htmlOut, true);
			htmlOut.println("<ul class=\"mktree\" id=\"tree1\">");
			htmlOut.println("<li>");
			printFunction(root,htmlOut,"",true);
			htmlOut.println("</li>");
			htmlOut.println("</ul>");
			printTail(htmlOut);
			htmlOut.flush();
			htmlOut.close();
		}
		printTail(pw);
		pw.flush();
		pw.close();
	}
	
	private static void createVarPages(Set<VarEntry> vars) throws FileNotFoundException{
		for(VarEntry var : vars){
			if(var.isThreadShared()){
				PrintWriter htmlOut = new PrintWriter(outputFolder+File.separator+"var-"+var.getVid()+".html");
				printHeader("var-"+var.getValue(),htmlOut,true);
				htmlOut.println("Address: "+var.getValue()+",Size: "+var.getSize()+", id: "+var.getVid());
				htmlOut.println("Writer:");
				Function writer = var.getWriter();
				htmlOut.println("<ul>");
				htmlOut.println("<li>");
				htmlOut.println("<a href=thread-"+writer.getThreadID()+".html>"+writer.getName()+", startTime: "+writer.getStartTime()+", threadID: "
						+writer.getThreadID()+"</a>");
				htmlOut.println("</li>");
				htmlOut.println("</ul>");
				htmlOut.println("Readers:");
				htmlOut.println("<ul>");
				for(Function reader : var.getReaders()){
					htmlOut.println("<li>");
					htmlOut.println("<a href=\"thread-"+reader.getThreadID()+".html\">"+reader.getName()+", startTime: "+reader.getStartTime()+", threadID: "
							+reader.getThreadID()+"</a>");
					htmlOut.println("</li>");				
				}
				htmlOut.println("</ul>");
				printTail(htmlOut);
				htmlOut.flush();
				htmlOut.close();
			}
		}
	}
	
	private static void printFunction(Function f, PrintWriter pw, String indent, boolean isRoot){
		String head = htmlEscape(f.getName()+", startTime: "+f.getStartTime()+", endTime: "+f.getEndTime()+", execTime: " + f.getExecutionTime());
		if(!f.isNative())
			pw.println(indent+head);
		else
			pw.println(indent+"<font color=\"red\">(NATIVE)"+ head +"</font>");
		pw.println(indent+"<ul>");
		pw.println(indent+"<li>");
		pw.println(indent+"\tData written:");
		pw.println(indent+"\t<ul>");
		for(VarEntry v : f.getSubtreeOutput()){
			pw.println(indent+"\t<li>");
			if(v.isThreadShared())
				pw.println(indent+"\t\t<a href=\"var-"+v.getVid()+".html\">"+"Address: "+v.getValue()+", Size: "+v.getSize()+"</a>");
			else
				pw.println(indent+"\t\tAddress: "+v.getValue()+", Size: "+v.getSize());
			pw.println(indent+"\t</li>");
		}
		pw.println(indent+"\t</ul>");
		pw.println(indent+"</li>");
		pw.println(indent+"<li>");
		pw.println(indent+"\tData read:");
		pw.println(indent+"\t<ul>");
		for(VarEntry v : f.getSubtreeInput()){
			pw.println(indent+"\t<li>");
			if(v.isThreadShared())
				pw.println(indent+"\t\t<a href=\"var-"+v.getVid()+".html\">"+"Address: "+v.getValue()+", Size: "+v.getSize()+"</a>");
			else
				pw.println(indent+"\t\tAddress: "+v.getValue()+", Size: "+v.getSize());
			pw.println(indent+"\t</li>");
		}
		pw.println(indent+"\t</ul>");
		pw.println(indent+"</li>");
		Vector<Function> children = f.getChildren();
		if(children.size() != 0){
//			pw.println(indent+"<ul"+(isRoot?" class=\"mktree\" id=\"tree1\"":"")+">");
			for(Function c: children){
				pw.println(indent+"<li>");
				printFunction(c, pw, indent+"\t",false);
				pw.println(indent+"</li>");
			}
//			pw.println(indent+"</ul>");
		}
		pw.println(indent+"</ul>");
	}
	
	private static void printHeader(String title, PrintWriter pw, boolean withTemp){
		pw.println("<html>");
		pw.println("<head>");
		pw.println("<title>"+title+"</title>");
		if(withTemp){
			for(String s : heads)
				pw.println(s);
		}else{
		pw.println("</head>");
		pw.println("<body>");
		}
	}
	
	private static void printTail(PrintWriter pw){
		pw.println("</body>");
		pw.println("</html>");
	}

    public static String htmlEscape(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        for (char c: input.toCharArray()) {
            if (replacements.containsKey(c)) {
                sb.append(replacements.get(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
    
	/*
	 * Methods to modify the forest; if on a call stack, all methods 
	 * belong to one of the packages listed in packageNames, delete
	 * that call stack.
	 */
	public static void cutForest(String packageNames, Function forest){
		String[] packages = packageNames.split(",");
		String[] parsedPN = new String[packages.length];
		for(int i = 0; i < packages.length; i++){
			parsedPN[i] = packages[i].replace(".", "/").trim();
		}
		for(Function thread : forest.getChildren())
			for(Function top : thread.getChildren())
				cutSubTree(top,parsedPN);
	}
	
	private static boolean cutSubTree(Function f, String[] parsedPN){
		if(isLibMethod(f,parsedPN)){
			if(f.getChildren()!=null){
				for(Function c : f.getChildren()){
					if(!cutSubTree(c,parsedPN))
						return false;
				}
				f.getChildren().clear();}
			return true;
		}
		return false;
	}

	private static boolean isLibMethod(Function f, String[] parsedPN){
		String fname = f.getName().substring(1, f.getName().length());
		for(String p : parsedPN){
			if(fname.startsWith(p)&&!p.equals(""))
				return true;
		}
		return false;
	}
	
	public static long getTotalTime(Function forrest){
		long totalTime = 0;
		for(Function thread : forrest.getChildren())
			totalTime+=thread.getExecutionTime();
		return totalTime;
	}
	
	public static long getNativeTime(Function forrest){
		return getNativeTimeRecur(forrest);
	}

	private static long getNativeTimeRecur(Function f){
		long result = 0;
		if(f.isNative())
			result+=f.getRemainderExecutionTime();
		for(Function c : f.getChildren())
			result+=getNativeTimeRecur(c);
		return result;
	}
	
	public static long getJavaTime(Function forrest){
		return getTotalTime(forrest) - getNativeTime(forrest);
	}
	
	private static void drawDot(Function forest,Set<VarEntry> vars) throws FileNotFoundException{
		PrintWriter pw = new PrintWriter(new File(dotOut));
		pw.println("digraph G {");
//		drawNodeIDRecursively(forest,pw);
//		drawDotRecursively(forest,pw);
		for(Function t : forest.getChildren())
			drawThreadNode(t,pw);	
		Set<Pair<Integer,Integer>> threadEdges = new HashSet<Pair<Integer,Integer>>();
		for(VarEntry var : vars)
			if(var.isThreadShared()){
				Function w = var.getWriter();
				for(Function r : var.getReaders())
					if(r.getThreadID()!=w.getThreadID())
				threadEdges.add(new Pair<Integer,Integer>(w.getThreadID(),r.getThreadID()));
			}
		for(Pair<Integer,Integer> p : threadEdges){
			pw.println(p.getFirst()+"->"+p.getSecond()+";");
		}
		pw.println("}");
		pw.flush();
		pw.close();
	}
	
	
	private static void drawThreadNode(Function f, PrintWriter pw){
		pw.println(f.getThreadID()+" [lable=\""+f.getThreadID()+"\"];");
	}
	
	private static void drawNodeIDRecursively(Function f, PrintWriter pw){
		pw.println(f.getID()+" [lable=\""+f.toString()+"\"];");
		for(Function c : f.getChildren())
			drawNodeIDRecursively(c,pw);
	}
	
	private static void drawDotRecursively(Function f, PrintWriter pw){
		pw.print(f.getID()+" -> {");
		for(Function c : f.getChildren())
			pw.print(c.getID()+";");
		pw.println("}");
		for(Function c : f.getChildren())
			drawDotRecursively(c,pw);
	}
}
