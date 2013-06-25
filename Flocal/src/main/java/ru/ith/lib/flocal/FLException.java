package ru.ith.lib.flocal;

public class FLException extends Exception {
	public final String details;
	
	public FLException(String message, String details) {
		super(message);
		this.details = details;
	}
}
