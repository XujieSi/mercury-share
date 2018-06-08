package edu.gatech.traceprocessor.parser;

public class RegOrStaticData extends Data {

	public String RS_TypeName;
	public RegOrStaticData(long addr, int size, int index, String _typeName) {
		super(addr, size, index, _typeName);
		RS_TypeName = _typeName;
	}

	@Override
	public String toString() {
		return "RegOrStaticData [addr=" + addr + ", size=" + size + ", type=" + RS_TypeName + "]";
	}

}
