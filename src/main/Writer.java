package main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Writer {
	FileOutputStream output = null;
	String test = "";

	public Writer() {

	}

	public void writeToFile(String payloadString) throws IOException {
		output = new FileOutputStream(new File("new_" + payloadString));
	}

	public void writeToFile(byte[] payload) throws IOException {
		output.write(payload);
	}	
}
