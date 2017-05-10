package edu.pitt.slideviewer.qview.connection.handler;

import java.io.InputStream;
import java.awt.*;
import java.util.*;

import edu.pitt.slideviewer.qview.connection.Communicator;
import edu.pitt.slideviewer.qview.connection.Utils;

/**
 * This class can process simple images from stream: JPEG, GIF, PNG whatever
 * Java supports It uses Toolkit.createImage() instead of ImageIO.read() because
 * of spead
 * 
 * @author tseytlin
 */

public class ImageStreamHandler implements StreamHandler {
	private Communicator com;
	private Image img; // result image
	private byte[] buffer = new byte[4096]; // temp buffer

	/**
	 * Need any component to track images
	 * 
	 * @param comp
	 */
	public ImageStreamHandler() {
		com = Communicator.getInstance();
	}

	/**
	 * Get Java object that was recieved
	 * 
	 * @return
	 */
	public Object getResult() {
		return img;
	}

	/**
	 * Use Toolkit to process simple image
	 */
	public boolean processStream(InputStream in) throws Exception {
		// ImageIO seems to be slow!!!!
		// it looks like it is cheaper to read inputStream
		// into byte array and loaded via Toolkit
		// long time = System.currentTimeMillis();

		// temp 4k buffer
		ArrayList list = new ArrayList();

		// read in all data into buffer
		int size = 0;
		for (size = in.read(buffer); size > -1; size = in.read(buffer)) {
			if (com.isCanceled()) {
				in.close();
				// System.out.println("canceled stream");
				return false;
			}
			byte[] temp = new byte[size];
			System.arraycopy(buffer, 0, temp, 0, size);
			list.add(temp);
		}
		// close stream
		in.close();

		// determine future data size
		size = 0;
		for (int i = 0; i < list.size(); i++) {
			size += ((byte[]) list.get(i)).length;
		}

		// THE data buffer
		byte[] data = new byte[size];

		// fill data buffer
		for (int offs = 0, i = 0; i < list.size(); i++) {
			buffer = (byte[]) list.get(i);
			System.arraycopy(buffer, 0, data, offs, buffer.length);
			offs = offs + buffer.length;
		}
		// System.out.println("stream transfer time for "+data.length+" bytes
		// "+(System.currentTimeMillis()-time)+" ms");
		// time = System.currentTimeMillis();

		// create image via toolkit (FASTER)
		img = Toolkit.getDefaultToolkit().createImage(data);

		// wait for image to finish loading
		Utils.flushImage(img);
		// System.out.println("toolkit image load
		// "+(System.currentTimeMillis()-time)+" ms");
		return true;
	}
}
