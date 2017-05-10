package edu.pitt.slideviewer.qview.connection.handler;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class ListHandler implements StreamHandler {
	private ArrayList<String> list = new ArrayList<String>();
	
	public Object getResult() {
		return list;
	}

	public boolean processStream(InputStream in) throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		for(String line = reader.readLine();line != null;line = reader.readLine()){
			list.add(line.trim());
		}
		reader.close();
		return true;
	}

}
