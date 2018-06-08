package edu.gatech.pag.trace;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;

import edu.gatech.traceprocessor.parser.Data;
import edu.gatech.traceprocessor.parser.Instruction;
import edu.gatech.traceprocessor.parser.Method;
import edu.gatech.traceprocessor.parser.MethodEntry;
import edu.gatech.traceprocessor.parser.MethodExit;
import edu.gatech.traceprocessor.parser.Program;
import edu.gatech.traceprocessor.parser.Read;
import edu.gatech.traceprocessor.parser.Write;
import edu.gatech.traceprocessor.utils.Utils;

public class HtmlVisualizer {
	private static List<String> heads;
	public final static String indexFile = "index.html";
	public final static String VAR_DIR = "vars";
	
	public static void main(String[] args) throws IOException {
		String input = System.getProperty(Utils.TRACE_PATH, null);
		Program p = new Program();
		p.load(input);
		String outDir = System.getProperty(Utils.OUT_DIR, null);
		String out = System.getProperty(Utils.PLAIN_TRACE_OUT_PATH,null);
		if(out != null){
			p.writePlain(out);
		}
		visualize(p,outDir,false);
	}

	public static void visualize(Program p, String output, boolean printVarAccess) throws IOException{
		initEnv(output);
		createIndexPageAndMethodPages(p.getTraceName(),p.getThreads(),output, printVarAccess);
		if(printVarAccess)
			createVarPages(output,p.getData().values());
	}
	
	private static void initEnv(String outputFolder) throws IOException{
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
		File varDir = new File(outputFolder+File.separator +VAR_DIR);
		if(!varDir.exists())
			varDir.mkdirs();
	}
	
	private static void createIndexPageAndMethodPages(String inputFile,List<Method> threads, String outputFolder, boolean ifPrintVarAccess) throws FileNotFoundException{
		PrintWriter pw = new PrintWriter(outputFolder + File.separator + indexFile);
		printHeader(inputFile, pw, false);
		for(int i = 0; i < threads.size(); i++){
			Method root = threads.get(i);
			int threadID = root.getThreadID();
			pw.println("<a href=\"" + "thread-"+ threadID + ".html\">"+"thread-"+threadID+"</a><br/>");
			PrintWriter htmlOut = new PrintWriter(outputFolder+File.separator + "thread-"+threadID+".html");
			printHeader("thread-"+threadID,htmlOut, true);
			htmlOut.println("<ul class=\"mktree\" id=\"tree1\">");
			htmlOut.println("<li>");
			printFunction(root,htmlOut,"",true, ifPrintVarAccess);
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
	
	private static void createVarPages(String outputFolder, Collection<Data> vars) throws FileNotFoundException{
		for(Data var : vars){
		//	if(var.isThreadShared()){
				PrintWriter htmlOut = new PrintWriter(outputFolder+File.separator+VAR_DIR+File.separator+"var-"+var.getAddr()+"-"+var.getIndex()+".html");
				printHeader("var-"+var.getAddr()+"-"+var.getIndex(),htmlOut,true);
				htmlOut.println("Address: "+var.getAddr()+",Size: "+var.getSize());
				htmlOut.println("Accessors:");
				htmlOut.println("<ul>");
				for(Instruction accessor : var.getAccessors()){
					String type = (accessor instanceof Write)?"write":"read";
					htmlOut.println("<li>");
					htmlOut.println("<a href=\"../thread-"+accessor.getThreadID()+".html\">"+type+"in "+accessor.getMethod()+"</a>");
					htmlOut.println("</li>");
				}
				htmlOut.println("</ul>");
				printTail(htmlOut);
				htmlOut.flush();
				htmlOut.close();
		//	}
		}
	}

	private static void printFunction(Method f, PrintWriter pw, String indent, boolean isRoot, boolean printVarAccess){
		String head = Utils.htmlEscape(f.methName()+", startTime: "+f.getStartTime()+", endTime: "+f.getEndTime()+", execTime: " + f.getInclusiveTime() + ", lineNum: "+f.getMethodLine());
		if(!f.isNative())
			pw.println(indent+head);
		else{
			pw.println(indent+"<font color=\"red\">(NATIVE)"+ head +"</font>");
			System.out.println(head);
		}
		if(f.getCallees().size()!=0 || printVarAccess){
			pw.println(indent+"<ul>");
			for(Instruction i: f.getInstructions()){
				if(i instanceof MethodEntry){
					MethodEntry entry = (MethodEntry)i;
					pw.println(indent+"<li>");
					printFunction(entry.curMeth, pw, indent+"\t",false, printVarAccess);
					pw.println(indent+"</li>");
				}
				else 
					if(printVarAccess){
						if(i instanceof Write){
							Write w = (Write)i;
							pw.println(indent+"<li>");
							pw.println(indent+"Write. Address: <a href=\""+VAR_DIR+File.separator+"var-"+w.data.getAddr()+"-"+w.data.getIndex()+".html\">" + w.addr+"</a>, object size: "+w.objSize+", field size: "
									+w.fldSize+", field offset: "+w.offset);
							pw.println(indent+"</li>");
						}else if(i instanceof Read){
							Read r = (Read)i;
							pw.println(indent+"<li>");
							pw.println(indent+"Read. Address: <a href=\""+VAR_DIR+File.separator+"var-"+r.data.getAddr()+"-"+r.data.getIndex()+".html\">" + r.addr+"</a>, object size: "+r.objSize+", field size: "
									+r.fldSize+", field offset: "+r.offset);
							pw.println(indent+"</li>");
						}
					}
			}
			pw.println(indent+"</ul>");
		}
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
	
}
