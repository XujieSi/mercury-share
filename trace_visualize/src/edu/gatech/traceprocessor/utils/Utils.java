package edu.gatech.traceprocessor.utils;

import java.io.DataInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Utils {
	public final static String TRACE_PATH = "mcc.path.input";
	public final static String INPUT_TYPE = "mcc.input.type";
	public final static String OUT_DIR = "mcc.path.outdir";
	public final static String INPUT_BIN = "bin";
	public final static String INPUT_PLAIN = "plain";
	public final static String PLAIN_TRACE_OUT_PATH = "mcc.path.output";
	public final static String PIN_METHOD_LIST = "mcc.path.localmethods";
	public final static String COLOC_METHOD_LIST = "mcc.path.colocmethods";
	public final static String COLOC_LINE_LIST = "mcc.path.colocmethinsts";
	public final static String NETWORK_MODEL = "mcc.path.network";
	public final static String ALGORITHM = "mcc.algo";
	public final static String PIN_METHOD_OUT = "mcc.path.pinOut";
	public final static String DEF_PIN_OUT = "pinList.txt";
	public final static String DEP_LEVEL = "mcc.depLevel";
	public final static String IF_FIXCONCUR = "mcc.fixCon";
	public final static String DIS_COL = "mcc.disCol";
	
	public static boolean verboseLogging = true;
	
    private static HashMap<Character, String> replacements = new HashMap<Character, String>();
    static{
    replacements.put('&', "&amp;");
    replacements.put('>', "&gt;");
    replacements.put('<', "&lt;");
    replacements.put('\'', "&apos;");
    replacements.put('\"', "&quot;");
    replacements.put(' ', "&nbsp;");
    }

	public static void enableVerboseLogging(){
		verboseLogging = true;
	}
	
	public static void printLog(String msg){
		if(verboseLogging)
			System.out.println("LOG:"+msg);
	}
	
	public static void printLogWithTime(String msg){
		if(verboseLogging){
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		System.out.println("["+dateFormat.format(date)+"]"+msg);
		}
	}
	
	public static void printResults(String msg){
		System.out.println(msg);
	}
	
	public static void printError(String msg){
		System.out.println("ERROR:"+msg);
	}
	
	public static void printAsIs(String msg){
		System.out.print(msg);
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
	
	/**
	 * There is no unsigned int in java, so we have to use a long to represent it
	 * @param i
	 * @return
	 */
	public static long intToUnsigned(int i){
		return i & 0x00000000ffffffffL;
	}

    public static boolean areEqual(final Object x, final Object y) {
        return x == null ? y == null : x.equals(y);
    }
	
	public static String readStringUntilSpace(DataInputStream r) throws IOException{
		StringBuilder sb = new StringBuilder();
		while(true){
			char tc = (char)r.readByte();
			if(tc == ' ') //space
				break;
			sb.append(tc);
		}	
		return sb.toString();
	}
	
	public static void printConfig(){
		System.out.println("The execution parameters: ");
		for(Map.Entry entry : System.getProperties().entrySet())
			if(entry.getKey().toString().contains("mcc"))
				System.out.println("\t"+entry.getKey()+"="+entry.getValue());
	}
	
}