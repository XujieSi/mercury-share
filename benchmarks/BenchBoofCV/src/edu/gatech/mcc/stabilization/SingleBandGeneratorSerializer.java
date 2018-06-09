package edu.gatech.mcc.stabilization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;

import boofcv.core.image.inst.SingleBandGenerator;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class SingleBandGeneratorSerializer extends Serializer<SingleBandGenerator>{

	@Override
	public void write(Kryo kryo, Output output, SingleBandGenerator object) {
	    ByteArrayOutputStream b = new ByteArrayOutputStream();
        try {
        	ObjectOutputStream o = new ObjectOutputStream(b);
			o.writeObject(object.getType());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
        byte[] ba = b.toByteArray();
        output.writeInt(ba.length);
		output.writeBytes(ba);
	}

	@Override
	public SingleBandGenerator read(Kryo kryo, Input input,
			Class<SingleBandGenerator> type) {
		ByteArrayInputStream b = new ByteArrayInputStream(input.readBytes(input.readInt()));
		ObjectInputStream o;
		try {
			o = new ObjectInputStream(b);
			return new SingleBandGenerator((Class)o.readObject());
		} catch (Exception e) {
			throw new RuntimeException(e);
		} 
	}

}
