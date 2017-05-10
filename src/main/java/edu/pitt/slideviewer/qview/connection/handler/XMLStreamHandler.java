package edu.pitt.slideviewer.qview.connection.handler;


import java.io.InputStream;

import org.w3c.dom.Document;

import edu.pitt.slideviewer.qview.connection.Utils;

public class XMLStreamHandler implements StreamHandler {
	private Document doc;
	
	public Object getResult() {
		return doc;
	}

	public boolean processStream(InputStream in) throws Exception {
		doc = Utils.parseXML(in);
		return true;
	}

}
