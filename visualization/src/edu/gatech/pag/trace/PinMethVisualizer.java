package edu.gatech.pag.trace;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.gatech.traceprocessor.parser.Configuration;
import edu.gatech.traceprocessor.parser.Method;
import edu.gatech.traceprocessor.parser.Program;
import edu.gatech.traceprocessor.utils.Utils;
import edu.gatech.traceprocessor.utils.ValueReverseComparator;

public class PinMethVisualizer {
	static String inputFile;
	static String localMethodsPath;
	static String outputFile;
	static String outputDir;
	static String pinFile;
	
	public static void main(String[] args) throws IOException {
		inputFile = System.getProperty(Utils.TRACE_PATH, null);
		outputFile = System.getProperty(Utils.PLAIN_TRACE_OUT_PATH,null);
		localMethodsPath = System.getProperty(Utils.PIN_METHOD_LIST, null);
		outputDir = System.getProperty(Utils.OUT_DIR, null);
		pinFile = System.getProperty(Utils.PIN_METHOD_OUT,"pinlist.txt");
		Configuration.loadLocalMethods(localMethodsPath);
		Program p = new Program();
		p.loadBinary(inputFile);
		p.updatePinAndColocMethods();
//		p.inlineUnpinned();
		p.removeUnpinned();
		HtmlVisualizer.visualize(p, outputDir,false);
		if(outputFile!=null)
			p.writePlain(outputFile);
		Map<String,Long> methTimeMap = new HashMap<String,Long>();
		Map<String,Integer> methCountMap = new HashMap<String,Integer>();
		for(Method m : p.getPinnedMethods()){
			Long time = methTimeMap.get(m.methName());
			Integer count = methCountMap.get(m.methName());
			if(time == null)
				time = 0L;
			if(count == null)
				count = 0;
			methTimeMap.put(m.methName(), time+m.getInclusiveTime());
			methCountMap.put(m.methName(), count+1);
		}
		List<Map.Entry<String,Long>> entryList = new ArrayList<Map.Entry<String,Long>>(methTimeMap.entrySet());
		Collections.sort(entryList, new ValueReverseComparator());
		PrintWriter pw = new PrintWriter(new File(pinFile));
		for(Map.Entry<String, Long> entry : entryList){
			pw.println(entry.getKey()+"\t\t\t\t"+entry.getValue()+"\t\t\t\t"+methCountMap.get(entry.getKey()));
		}
		pw.flush();
		pw.close();
	}
	
}


