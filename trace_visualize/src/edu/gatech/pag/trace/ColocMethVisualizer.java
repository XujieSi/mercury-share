package edu.gatech.pag.trace;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import edu.gatech.traceprocessor.parser.Configuration;
import edu.gatech.traceprocessor.parser.Method;
import edu.gatech.traceprocessor.parser.Program;
import edu.gatech.traceprocessor.utils.Utils;

public class ColocMethVisualizer {
	static String inputFile;
	static String colocMethodsPath;
	static String pinMethodPath;
	static String outputFile;
	static String outputDir;
	static boolean ifFixCon;
	
	public static void main(String[] args) throws IOException {
		inputFile = System.getProperty(Utils.TRACE_PATH, null);
		outputFile = System.getProperty(Utils.PLAIN_TRACE_OUT_PATH,null);
		colocMethodsPath = System.getProperty(Utils.COLOC_METHOD_LIST, null);
		pinMethodPath = System.getProperty(Utils.PIN_METHOD_LIST,null);
		outputDir = System.getProperty(Utils.OUT_DIR, null);
		ifFixCon = Boolean.getBoolean(Utils.IF_FIXCONCUR);
		Configuration.loadCoLocMethods(colocMethodsPath);
		Configuration.loadLocalMethods(pinMethodPath);
		Program p = new Program();
		p.loadBinary(inputFile);
		p.updatePinAndColocMethods();
		p.fixConcurrencyWithColoc(false);
//		if(ifFixCon)
//			p.fixConcurrencyWithColoc(false);
		p.mergeSCCs();
		Set<Set<Method>> colocSets = p.getColocSet();
		Utils.printLogWithTime("Colocated: methhods");
		for(Set<Method> colSet : colocSets){
			TreeSet<Method> sortedSet = new TreeSet<Method>(colSet);
			Utils.printLogWithTime("********************");
			for(Method m : sortedSet){
				Utils.printLogWithTime(m.toString());
				if(p.isMethodPinned(m)){
					Utils.printLogWithTime("OOOOOPS, the method above is PINNED!!!!");
				}
			}
		}
		
		p.removeUnColoc();
		HtmlVisualizer.visualize(p, outputDir,false);
		if(outputFile!=null)
			p.writePlain(outputFile);
		Map<String,Long> methTimeMap = new HashMap<String,Long>();
		for(Method m : p.getPinnedMethods()){
			Long time = methTimeMap.get(m.methName());
			if(time == null)
				time = 0L;
			methTimeMap.put(m.methName(), time+m.getInclusiveTime());
		}
	}
	
}


