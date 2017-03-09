package main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Writer {
	FileOutputStream output = null;
	String test = "";

	public Writer() {

	}

	public void writeToFile(String packageString) throws IOException {
		output = new FileOutputStream(new File("output_" + packageString));
	}

	public void writeToFile(byte[] packageByte) throws IOException {
		output.write(packageByte);
	}	
}
