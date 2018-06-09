package edu.gatech.traceprocessor.utils;

import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.gatech.traceprocessor.VarEntry;

/**
 * A bitset implementation of Set interface specially for Chord Dom objects. The
 * operations between DomBitSets with the same Dom should be significantly
 * faster than normal Set operations.
 * 
 * @author xin
 * 
 * @param <E>
 */
public class VarBitSet implements Set<VarEntry> {
	private List<VarEntry> domain;
	private BitSet bitSet;

	public VarBitSet(List<VarEntry> domain, int capacity) {
		this.domain = domain;
		bitSet = new BitSet(capacity);
	}

	public VarBitSet(VarBitSet that){
		this.domain = that.domain;
		this.bitSet = (BitSet)that.bitSet.clone();
	}
	
	@Override
	public int size() {
		return bitSet.cardinality();
	}

	@Override
	public boolean isEmpty() {
		return bitSet.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		if(o instanceof VarEntry){
			VarEntry v = (VarEntry)o;
			int index = v.getVid(); 
			if (index >= 0)
				return bitSet.get(index);
		}
		return false;
	}

	@Override
	public Iterator<VarEntry> iterator() {
		return new DomIterator();
	}

	@Override
	public Object[] toArray() {
		Object[] ret = new Object[this.size()];
		Iterator<VarEntry> iter = this.iterator();
		for (int i = 0; i < ret.length; i++) {
			ret[i] = iter.next();
		}
		return ret;
	}


	@Override
	public boolean add(VarEntry v) {
		int idx = v.getVid();
		if (bitSet.get(idx))
			return false;
		bitSet.set(idx);
		return true;
	}

	@Override
	public boolean remove(Object o) {
		if(o instanceof VarEntry){
			VarEntry v = (VarEntry)o;
			int idx = v.getVid();
			if (idx < 0)
				return false;
			if (!bitSet.get(idx))
				return false;
			bitSet.clear(idx);
			return true;
		}
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		if (c instanceof VarBitSet) {
			VarBitSet that = (VarBitSet) c;
			if (that.domain != domain)// we assume over the same type, all the
								// DomBitSet share the same Dom
				return false;
			BitSet bsCopy = (BitSet) bitSet.clone();
			bsCopy.or(that.bitSet);
			if (bsCopy.equals(bitSet))
				return true;
			return false;
		}
		for (Object o : c)
			if (!this.contains(o))
				return false;
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends VarEntry> c) {
		if (c instanceof VarBitSet) {
			VarBitSet that = (VarBitSet) c;
			if (that.domain != domain)// we assume over the same type, all the
								// DomBitSet share the same Dom
				return false;
			BitSet bsCopy = (BitSet) bitSet.clone();
			bitSet.or(that.bitSet);
			if (bsCopy.equals(bitSet))
				return false;
			return true;
		}
		boolean added = false;
		for (VarEntry o : c)
			added |= this.add(o);
		return added;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		if (c instanceof VarBitSet) {
			VarBitSet that = (VarBitSet) c;
			if (that.domain != domain)// we assume over the same type, all the
								// DomBitSet share the same Dom
				return false;
			BitSet bsCopy = (BitSet) bitSet.clone();
			bitSet.and(that.bitSet);
			if (bsCopy.equals(bitSet))
				return false;
			return true;
		}
		BitSet newBS = new BitSet(domain.size());
		for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i + 1)) {
			if (c.contains(domain.get(i)))
				newBS.set(i);
		}
		if (newBS.equals(bitSet))
			return false;
		bitSet = newBS;
		return true;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		if (c instanceof VarBitSet) {
			VarBitSet that = (VarBitSet) c;
			if (that.domain != domain)// we assume over the same type, all the
								// DomBitSet share the same Dom
				return false;
			BitSet bsCopy = (BitSet) bitSet.clone();
			bitSet.andNot(that.bitSet);
			if (bsCopy.equals(bitSet))
				return false;
			return true;
		}
		boolean removed = false;
		for (Object o : c) {
			int idx = domain.indexOf(o);
			if (idx >= 0)
				if (bitSet.get(idx)) {
					removed = true;
					bitSet.clear(idx);
				}
		}
		return removed;
	}

	@Override
	public void clear() {
		bitSet.clear();
	}

	public String toString() {
		Iterator<VarEntry> i = iterator();
		if (!i.hasNext())
			return "[]";

		StringBuilder sb = new StringBuilder();
		sb.append('[');
		for (;;) {
			VarEntry e = i.next();
			sb.append(e);
			if (!i.hasNext())
				return sb.append(']').toString();
			sb.append(", ");
		}
	}
	
    public boolean equals(Object o) {
        if (o == this)
            return true;

        if (!(o instanceof Set))
            return false;
        Collection c = (Collection) o;
        if (c.size() != size())
            return false;
        try {
            return containsAll(c);
        } catch (ClassCastException unused)   {
            return false;
        } catch (NullPointerException unused) {
            return false;
        }
    }

    public int hashCode() {
        int h = 0;
        Iterator<VarEntry> i = iterator();
        while (i.hasNext()) {
            VarEntry obj = i.next();
            if (obj != null)
                h += obj.hashCode();
        }
        return h;
    }


	class DomIterator implements Iterator<VarEntry> {
		int counter = 0;

		@Override
		public boolean hasNext() {
			return bitSet.nextSetBit(counter) != -1;
		}

		@Override
		public VarEntry next() {
			int nextIdx = bitSet.nextSetBit(counter);
			if (nextIdx == -1)
				throw new IndexOutOfBoundsException();
			counter = nextIdx + 1;
			return domain.get(nextIdx);
		}

		@Override
		public void remove() {
			int lastIdx = counter - 1;
			int lastIdx1 = bitSet.nextSetBit(lastIdx);
			if (lastIdx1 != lastIdx)
				throw new RuntimeException(
						"Iterator.remove() must be called after Iterator.next()");
			bitSet.clear(lastIdx);
		}

	}


	@Override
	public <T> T[] toArray(T[] a) {
		throw new RuntimeException("Unsupported method!");
	}

}
