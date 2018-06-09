package edu.gatech.pag.trace;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import edu.gatech.traceprocessor.parser.Configuration;
import edu.gatech.traceprocessor.parser.Method;
import edu.gatech.traceprocessor.parser.Program;

public class PinTimeVisualizer {
	static String inputFile;
	static String localMethodsPath;
	static String outputFile;
	static Map<Integer,List<Long>> threTimeMap = new HashMap<Integer, List<Long>>();
	static double maxTime = -1;
	final static int maxWidth = 1024;
	final static int height = 50;
	
	public static void main(String[] args) throws IOException {
		inputFile = System.getProperty("mcc.path.input", null);
		outputFile = System.getProperty("mcc.path.output",null);
		localMethodsPath = System.getProperty("mcc.path.localmethods", null);
		Configuration.loadLocalMethods(localMethodsPath);
		Program p = new Program();
		p.loadBinary(inputFile);
		for(Method t : p.getThreads()){
			findPinnedFrags(t);
			if(t.getInclusiveTime() > maxTime)
				maxTime = t.getInclusiveTime();
		}
		for(Map.Entry<Integer, List<Long>> threTimeEntry : threTimeMap.entrySet()){
			int tid = threTimeEntry.getKey();
			List<Long> timeList = threTimeEntry.getValue();
			Method t = p.getThread(tid);
			drawImage(t,timeList);
		}
	}
	
	private static void findPinnedFrags(Method m){
		Method p = m.getCaller();
		int tid = m.getThreadID();
		List<Long> timeList = threTimeMap.get(tid);
		if(timeList == null){
			timeList = new ArrayList<Long>();
			threTimeMap.put(tid, timeList);
		}
		if(p == null || !isMethodPinned(p)){
			if(isMethodPinned(m)){
				timeList.add(m.getStartTime());
			}
		}else if(!isMethodPinned(m)){
			timeList.add(m.getStartTime());
		}
		for(Method c : m.getCallees())
			findPinnedFrags(c);
		if(p == null || !isMethodPinned(p)){
			if(isMethodPinned(m)){
				timeList.add(m.getEndTime());
			}
		}else if(!isMethodPinned(m)){
			timeList.add(m.getEndTime());
		}
	}
	
	private static void drawImage(Method thread, List<Long> timeList) throws IOException{
		File input = new File(inputFile);
		long curThreTime = thread.getInclusiveTime();
		double scale = ((double)maxWidth)/maxTime;
		int width = (int)(scale*curThreTime);
		if(width == 0){
			System.out.println("The running time of thread "+thread.getThreadID()+" is too small!");
			return;
		}
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = image.createGraphics();
		g2d.setColor(new Color(255, 0, 0));
		g2d.fillRect(0, 0, width, height);
	    int count = timeList.size();
	    g2d.setColor(new Color(0,0,0));
	    for(int i = 0; i < count/2; i++){
	    	int start = (int)(timeList.get(i*2) * scale);
	    	int end = (int)(timeList.get(i*2+1) * scale);
	    	g2d.fillRect(start, 0, end-start, height);
	    }
	    File outputfile = new File(input.getName()+"-"+thread.getThreadID()+".png");
	    ImageIO.write(image, "png", outputfile);
	}
	
	private static boolean isMethodPinned(Method m){
		String mName = m.methName();
		int pinTag = Configuration.getMethodPinTag(mName);
		if(m.isNative()){
			if(pinTag != Method.STATELESS && pinTag != Method.STATEFULL){
				return true;
			}
		}
		return false;
	}

}
