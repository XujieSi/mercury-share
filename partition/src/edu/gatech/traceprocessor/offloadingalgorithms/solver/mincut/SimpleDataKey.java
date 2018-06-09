package edu.gatech.traceprocessor.offloadingalgorithms.solver.mincut;

import java.util.Set;

import edu.gatech.traceprocessor.parser.Method;

public class SimpleDataKey {
	Method writer;
	Set<Method> readers;
	
	public SimpleDataKey(){
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((readers == null) ? 0 : readers.hashCode());
		result = prime * result + ((writer == null) ? 0 : writer.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SimpleDataKey other = (SimpleDataKey) obj;
		if (readers == null) {
			if (other.readers != null)
				return false;
		} else if (!readers.equals(other.readers))
			return false;
		if (writer == null) {
			if (other.writer != null)
				return false;
		} else if (!writer.equals(other.writer))
			return false;
		return true;
	}
	
	
}
