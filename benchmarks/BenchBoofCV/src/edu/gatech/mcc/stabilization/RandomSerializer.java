package edu.gatech.mcc.stabilization;

import java.util.Random;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class RandomSerializer extends Serializer<Random> {

	@Override
	public void write(Kryo kryo, Output output, Random object) {
		
	}

	@Override
	public Random read(Kryo kryo, Input input, Class<Random> type) {
		return new Random();
	}

}
