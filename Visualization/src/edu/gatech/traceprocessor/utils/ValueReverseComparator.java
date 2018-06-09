package edu.gatech.traceprocessor.utils;

import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;

public class ValueReverseComparator implements Comparator<Map.Entry<String,Long>>{

	@Override
	public int compare(Entry<String, Long> o1, Entry<String, Long> o2) {
		long result = o2.getValue()-o1.getValue();
		if(result == 0L)
			return 0;
		if(result > 0L)
			return 1;
		return -1;
	}
	
}