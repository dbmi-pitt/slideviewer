package edu.pitt.slideviewer;

public class ViewerException extends Exception {
	public ViewerException(String message){
		super(message);	
	}
	public ViewerException(String message,Throwable cause){
		super(message,cause);	
	}
}
