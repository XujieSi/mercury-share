package edu.gatech.traceprocessor.parser;

public class RegOrStaticData extends Data {

	public RegOrStaticData(long addr, int size, int index) {
		super(addr, size, index);
	}

	@Override
	public String toString() {
		return "RegOrStaticData [addr=" + addr + ", size=" + size + "]";
	}

}
