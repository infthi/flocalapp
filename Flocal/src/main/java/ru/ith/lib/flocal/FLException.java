package ru.ith.lib.flocal;

public class FLException extends Exception {
	public final String details;

	public FLException(String message, String details, Throwable cause){
		super(message, cause);
		this.details = details;
	}
	public FLException(String message, String details) {
		this(message, details, null);
	}
}
